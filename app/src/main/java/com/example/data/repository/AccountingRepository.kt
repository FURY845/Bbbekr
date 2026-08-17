package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.local.CustomerDao
import com.example.data.local.CustomerTransactionDao
import com.example.data.local.InventoryLogDao
import com.example.data.local.InvoiceDao
import com.example.data.local.InvoiceWithItems
import com.example.data.local.ProductDao
import com.example.data.local.SafeDao
import com.example.data.local.StoreSettingsDao
import com.example.data.model.CartItem
import com.example.data.model.Customer
import com.example.data.model.CustomerTransaction
import com.example.data.model.CustomerTransactionType
import com.example.data.model.InventoryLog
import com.example.data.model.Invoice
import com.example.data.model.InvoiceItem
import com.example.data.model.MovementType
import com.example.data.model.PaymentType
import com.example.data.model.Product
import com.example.data.model.SafeTransaction
import com.example.data.model.SafeTransactionType
import com.example.data.model.StoreSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AccountingRepository(private val database: AppDatabase) {

    private val productDao: ProductDao = database.productDao()
    private val customerDao: CustomerDao = database.customerDao()
    private val customerTransactionDao: CustomerTransactionDao = database.customerTransactionDao()
    private val invoiceDao: InvoiceDao = database.invoiceDao()
    private val safeDao: SafeDao = database.safeDao()
    private val storeSettingsDao: StoreSettingsDao = database.storeSettingsDao()
    private val inventoryLogDao: InventoryLogDao = database.inventoryLogDao()

    // Flow Streams
    val allProducts: Flow<List<Product>> = productDao.getAllProducts()
    val lowStockProducts: Flow<List<Product>> = productDao.getLowStockProducts()
    val allCustomers: Flow<List<Customer>> = customerDao.getAllCustomers()
    val totalCustomerDebts: Flow<Double?> = customerDao.getTotalCustomerDebts()
    val allInvoicesWithItems: Flow<List<InvoiceWithItems>> = invoiceDao.getAllInvoicesWithItems()
    val allSafeTransactions: Flow<List<SafeTransaction>> = safeDao.getAllSafeTransactions()
    val storeSettings: Flow<StoreSettings?> = storeSettingsDao.getSettings()

    // Start of today timestamp
    fun getStartOfTodayTimestamp(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    val todayInvoices: Flow<List<Invoice>> = invoiceDao.getInvoicesSince(getStartOfTodayTimestamp())
    val todaySafeTransactions: Flow<List<SafeTransaction>> = safeDao.getSafeTransactionsSince(getStartOfTodayTimestamp())
    val todayInventoryLogs: Flow<List<InventoryLog>> = inventoryLogDao.getLogsSince(getStartOfTodayTimestamp())
    val todaySalesLogs: Flow<List<InventoryLog>> = inventoryLogDao.getTodaySalesLogs(getStartOfTodayTimestamp())

    fun getStartOfWeekTimestamp(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    fun getStartOfMonthTimestamp(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    fun getCustomerTransactions(customerId: Long): Flow<List<CustomerTransaction>> {
        return customerTransactionDao.getTransactionsForCustomer(customerId)
    }

    fun getCustomerInvoicesWithItems(customerId: Long): Flow<List<InvoiceWithItems>> {
        return invoiceDao.getInvoicesWithItemsForCustomer(customerId)
    }

    // Product actions
    suspend fun saveProduct(product: Product): Long = withContext(Dispatchers.IO) {
        if (product.id == 0L) {
            val id = productDao.insertProduct(product)
            if (product.stockQuantity > 0) {
                inventoryLogDao.insertLog(
                    InventoryLog(
                        productId = id,
                        productName = product.name,
                        movementType = MovementType.IN_PURCHASE,
                        quantity = product.stockQuantity,
                        previousStock = 0.0,
                        newStock = product.stockQuantity,
                        unitPrice = product.purchasePrice,
                        notes = "إضافة منتج جديد"
                    )
                )
            }
            id
        } else {
            val existing = productDao.getProductById(product.id)
            if (existing != null && existing.stockQuantity != product.stockQuantity) {
                val diff = product.stockQuantity - existing.stockQuantity
                inventoryLogDao.insertLog(
                    InventoryLog(
                        productId = product.id,
                        productName = product.name,
                        movementType = if (diff > 0) MovementType.IN_PURCHASE else MovementType.ADJUSTMENT,
                        quantity = Math.abs(diff),
                        previousStock = existing.stockQuantity,
                        newStock = product.stockQuantity,
                        unitPrice = product.purchasePrice,
                        notes = "تعديل كمية المخزون"
                    )
                )
            }
            productDao.updateProduct(product)
            product.id
        }
    }

    suspend fun updateProductSellPrice(productId: Long, newPrice: Double) = withContext(Dispatchers.IO) {
        productDao.updateSellPrice(productId, newPrice)
    }

    suspend fun deleteProduct(product: Product) = withContext(Dispatchers.IO) {
        productDao.deleteProduct(product)
    }

    suspend fun addProductStockQuantity(productId: Long, addedQuantity: Double, purchaseCost: Double, payFromSafe: Boolean, notes: String) = withContext(Dispatchers.IO) {
        val product = productDao.getProductById(productId) ?: return@withContext
        val prevStock = product.stockQuantity
        val newStock = prevStock + addedQuantity
        productDao.adjustStock(productId, addedQuantity)

        inventoryLogDao.insertLog(
            InventoryLog(
                productId = productId,
                productName = product.name,
                movementType = MovementType.IN_PURCHASE,
                quantity = addedQuantity,
                previousStock = prevStock,
                newStock = newStock,
                unitPrice = purchaseCost,
                notes = if (notes.isNotBlank()) notes else "توريد مخزون إضافي"
            )
        )

        if (payFromSafe && purchaseCost * addedQuantity > 0) {
            safeDao.insertSafeTransaction(
                SafeTransaction(
                    type = SafeTransactionType.EXPENSE_PURCHASE,
                    amount = purchaseCost * addedQuantity,
                    category = "شراء بضاعة",
                    notes = "شراء $addedQuantity ${product.unit} من ${product.name}"
                )
            )
        }
    }

    // Customer actions
    suspend fun saveCustomer(customer: Customer): Long = withContext(Dispatchers.IO) {
        if (customer.id == 0L) {
            val id = customerDao.insertCustomer(customer)
            if (customer.balance != 0.0) {
                customerTransactionDao.insertTransaction(
                    CustomerTransaction(
                        customerId = id,
                        type = if (customer.balance > 0) CustomerTransactionType.SALE_CREDIT else CustomerTransactionType.PAYMENT_RECEIVED,
                        amount = Math.abs(customer.balance),
                        previousBalance = 0.0,
                        newBalance = customer.balance,
                        notes = "رصيد / دين افتتاحي للعميل",
                        timestamp = customer.debtDate
                    )
                )
            }
            id
        } else {
            customerDao.updateCustomer(customer)
            customer.id
        }
    }

    suspend fun updateCustomerDebt(customerId: Long, newBalance: Double, debtDate: Long, notes: String) = withContext(Dispatchers.IO) {
        val customer = customerDao.getCustomerById(customerId) ?: return@withContext
        val prev = customer.balance
        customerDao.updateBalanceAndDate(customerId, newBalance, debtDate)
        customerTransactionDao.insertTransaction(
            CustomerTransaction(
                customerId = customerId,
                type = if (newBalance > prev) CustomerTransactionType.SALE_CREDIT else CustomerTransactionType.PAYMENT_RECEIVED,
                amount = Math.abs(newBalance - prev),
                previousBalance = prev,
                newBalance = newBalance,
                notes = if (notes.isNotBlank()) notes else "تعديل رصيد الدين السابق",
                timestamp = debtDate
            )
        )
    }

    suspend fun deleteCustomer(customer: Customer) = withContext(Dispatchers.IO) {
        customerTransactionDao.deleteTransactionsForCustomer(customer.id)
        customerDao.deleteCustomer(customer)
    }

    suspend fun recordCustomerPayment(
        customerId: Long,
        amount: Double,
        depositToSafe: Boolean = true,
        notes: String = "سداد دفعة نقدية"
    ) = withContext(Dispatchers.IO) {
        val customer = customerDao.getCustomerById(customerId) ?: return@withContext
        val prevBalance = customer.balance
        val newBalance = prevBalance - amount
        customerDao.adjustBalance(customerId, -amount)

        customerTransactionDao.insertTransaction(
            CustomerTransaction(
                customerId = customerId,
                type = CustomerTransactionType.PAYMENT_RECEIVED,
                amount = amount,
                previousBalance = prevBalance,
                newBalance = newBalance,
                notes = notes
            )
        )

        if (depositToSafe && amount > 0) {
            safeDao.insertSafeTransaction(
                SafeTransaction(
                    type = SafeTransactionType.INCOME_CUSTOMER_PAYMENT,
                    amount = amount,
                    category = "تحصيل من عملاء",
                    notes = "تحصيل من العميل: ${customer.name} - $notes",
                    relatedCustomerId = customerId
                )
            )
        }
    }

    suspend fun returnProductItem(
        product: Product,
        quantity: Double,
        customer: Customer?,
        refundCash: Boolean,
        notes: String
    ) = withContext(Dispatchers.IO) {
        val returnTotal = product.sellPrice * quantity
        val newStock = product.stockQuantity + quantity
        // 1. Adjust Stock
        productDao.adjustStock(product.id, quantity)

        // 2. Inventory Log
        val customerNameInfo = customer?.name ?: "عميل عام"
        inventoryLogDao.insertLog(
            InventoryLog(
                productId = product.id,
                productName = product.name,
                movementType = MovementType.RETURN_ITEM,
                quantity = quantity,
                previousStock = product.stockQuantity,
                newStock = newStock,
                unitPrice = product.sellPrice,
                notes = "إرجاع $quantity ${product.unit} (العميل: $customerNameInfo)${if (notes.isNotBlank()) " - $notes" else ""}",
                timestamp = System.currentTimeMillis()
            )
        )

        // 3. Customer debt reduction if customer exists and not cash refunded
        if (customer != null && !refundCash && returnTotal > 0) {
            val prevBal = customer.balance
            val newBal = (prevBal - returnTotal).coerceAtLeast(0.0)
            customerDao.adjustBalance(customer.id, -returnTotal)
            customerTransactionDao.insertTransaction(
                CustomerTransaction(
                    customerId = customer.id,
                    type = CustomerTransactionType.DEBT_ADJUSTMENT,
                    amount = returnTotal,
                    previousBalance = prevBal,
                    newBalance = newBal,
                    notes = "خصم دين مقابل مرتجع: ${product.name} (عدد $quantity)"
                )
            )
        }

        // 4. Safe refund if cash was returned to customer
        if (refundCash && returnTotal > 0) {
            safeDao.insertSafeTransaction(
                SafeTransaction(
                    type = SafeTransactionType.EXPENSE_REFUND,
                    amount = returnTotal,
                    category = "مرتجع مبيعات",
                    notes = "رد نقدية مقابل مرتجع ${product.name} (عدد $quantity) لـ $customerNameInfo",
                    relatedCustomerId = customer?.id
                )
            )
        }
    }

    // Safe actions
    suspend fun addSafeTransaction(transaction: SafeTransaction) = withContext(Dispatchers.IO) {
        safeDao.insertSafeTransaction(transaction)
    }

    // Settings
    suspend fun updateSettings(settings: StoreSettings) = withContext(Dispatchers.IO) {
        storeSettingsDao.saveSettings(settings)
    }

    // POS / Write Invoice Transaction
    suspend fun completeSale(
        items: List<CartItem>,
        customer: Customer?,
        paymentType: PaymentType,
        notes: String
    ): InvoiceWithItems = withContext(Dispatchers.IO) {
        val timestamp = System.currentTimeMillis()
        val dateFormat = SimpleDateFormat("yyMMdd-HHmmss", Locale.ENGLISH)
        val invoiceNumber = "INV-${dateFormat.format(Date(timestamp))}"

        val totalAmount = items.sumOf { it.total }
        val netAmount = totalAmount
        val totalCost = items.sumOf { it.product.purchasePrice * it.quantity }
        val totalProfit = netAmount - totalCost

        val previousDebt = customer?.balance ?: 0.0
        val paidAmount = if (paymentType == PaymentType.CREDIT) 0.0 else netAmount
        val remainingAmount = if (paymentType == PaymentType.CREDIT) netAmount else 0.0

        val invoice = Invoice(
            invoiceNumber = invoiceNumber,
            customerId = customer?.id,
            customerName = customer?.name ?: "عميل نقدي",
            customerPhone = customer?.phone ?: "",
            customerAddress = customer?.address ?: "",
            previousDebt = previousDebt,
            totalAmount = totalAmount,
            discountAmount = 0.0,
            netAmount = netAmount,
            paidAmount = paidAmount,
            remainingAmount = remainingAmount,
            totalCost = totalCost,
            totalProfit = totalProfit,
            paymentType = paymentType,
            timestamp = timestamp,
            notes = notes
        )

        val invoiceId = invoiceDao.insertInvoice(invoice)

        val invoiceItems = items.map { cartItem ->
            InvoiceItem(
                invoiceId = invoiceId,
                productId = cartItem.product.id,
                productName = cartItem.product.name,
                unitPrice = cartItem.customPrice,
                purchasePrice = cartItem.product.purchasePrice,
                quantity = cartItem.quantity,
                unit = cartItem.product.unit,
                total = cartItem.total
            )
        }
        invoiceDao.insertInvoiceItems(invoiceItems)

        // Deduct inventory stock for each product
        for (item in items) {
            if (item.product.id > 0) {
                val currentProd = productDao.getProductById(item.product.id)
                val prevStock = currentProd?.stockQuantity ?: item.product.stockQuantity
                val newStock = prevStock - item.quantity
                productDao.adjustStock(item.product.id, -item.quantity)

                inventoryLogDao.insertLog(
                    InventoryLog(
                        productId = item.product.id,
                        productName = item.product.name,
                        movementType = MovementType.OUT_SALE,
                        quantity = item.quantity,
                        previousStock = prevStock,
                        newStock = newStock,
                        unitPrice = item.customPrice,
                        notes = "مبيعات فاتورة $invoiceNumber (${customer?.name ?: "عميل نقدي"})"
                    )
                )
            }
        }

        // Safe Transaction for cash inflow
        if (paymentType == PaymentType.CASH && netAmount > 0) {
            safeDao.insertSafeTransaction(
                SafeTransaction(
                    type = SafeTransactionType.INCOME_SALE,
                    amount = netAmount,
                    category = "مبيعات نقدية",
                    notes = "مبيعات فاتورة $invoiceNumber (${customer?.name ?: "عميل نقدي"})",
                    relatedInvoiceId = invoiceId
                )
            )
        } else if (paymentType == PaymentType.CARD && netAmount > 0) {
            safeDao.insertSafeTransaction(
                SafeTransaction(
                    type = SafeTransactionType.INCOME_SALE,
                    amount = netAmount,
                    category = "مبيعات شبكة",
                    notes = "دفع شبكة فاتورة $invoiceNumber",
                    relatedInvoiceId = invoiceId
                )
            )
        } else if (paymentType == PaymentType.TRANSFER && netAmount > 0) {
            safeDao.insertSafeTransaction(
                SafeTransaction(
                    type = SafeTransactionType.INCOME_SALE,
                    amount = netAmount,
                    category = "تحويل بنكي",
                    notes = "تحويل بنكي فاتورة $invoiceNumber",
                    relatedInvoiceId = invoiceId
                )
            )
        }

        // Customer Ledger if Credit
        if (paymentType == PaymentType.CREDIT && customer != null && remainingAmount > 0) {
            val prevBal = customer.balance
            val newBal = prevBal + remainingAmount
            customerDao.adjustBalance(customer.id, remainingAmount)

            customerTransactionDao.insertTransaction(
                CustomerTransaction(
                    customerId = customer.id,
                    type = CustomerTransactionType.SALE_CREDIT,
                    amount = remainingAmount,
                    previousBalance = prevBal,
                    newBalance = newBal,
                    notes = "فاتورة بيع آجل $invoiceNumber",
                    invoiceId = invoiceId
                )
            )
        }

        val completedInvoice = invoice.copy(id = invoiceId)
        InvoiceWithItems(invoice = completedInvoice, items = invoiceItems)
    }

    suspend fun clearAllData() = withContext(Dispatchers.IO) {
        productDao.clearAllProducts()
        customerDao.clearAllCustomers()
        customerTransactionDao.clearAllTransactions()
        invoiceDao.clearAllInvoices()
        invoiceDao.clearAllInvoiceItems()
        safeDao.clearAllSafeTransactions()
        inventoryLogDao.clearAllInventoryLogs()
    }

    suspend fun getAllProductsDirect(): List<Product> = withContext(Dispatchers.IO) {
        productDao.getAllProductsDirect()
    }

    suspend fun getAllCustomersDirect(): List<Customer> = withContext(Dispatchers.IO) {
        customerDao.getAllCustomersDirect()
    }

    suspend fun getAllCustomerTransactionsDirect(): List<CustomerTransaction> = withContext(Dispatchers.IO) {
        customerTransactionDao.getAllTransactionsDirect()
    }

    suspend fun getAllInvoicesWithItemsDirect(): List<InvoiceWithItems> = withContext(Dispatchers.IO) {
        invoiceDao.getAllInvoicesWithItemsDirect()
    }

    suspend fun getTodayInvoicesDirect(): List<Invoice> = withContext(Dispatchers.IO) {
        invoiceDao.getInvoicesSinceDirect(getStartOfTodayTimestamp())
    }

    suspend fun getAllSafeTransactionsDirect(): List<SafeTransaction> = withContext(Dispatchers.IO) {
        safeDao.getAllSafeTransactionsDirect()
    }

    suspend fun restoreBackup(backup: com.example.util.BackupPackage, isOverwrite: Boolean) = withContext(Dispatchers.IO) {
        if (isOverwrite) {
            clearAllData()
        }

        // Restore Store Settings if included
        if (backup.storeSettings != null) {
            storeSettingsDao.saveSettings(backup.storeSettings)
        }

        // Restore Products
        if (!backup.products.isNullOrEmpty()) {
            productDao.insertProducts(backup.products)
        }

        // Restore Customers
        if (!backup.customers.isNullOrEmpty()) {
            customerDao.insertCustomers(backup.customers)
        }

        // Restore Customer Transactions
        if (!backup.customerTransactions.isNullOrEmpty()) {
            customerTransactionDao.insertTransactions(backup.customerTransactions)
        }

        // Restore Invoices & Invoice Items
        if (!backup.invoices.isNullOrEmpty()) {
            invoiceDao.insertInvoices(backup.invoices)
        }
        if (!backup.invoiceItems.isNullOrEmpty()) {
            invoiceDao.insertInvoiceItems(backup.invoiceItems)
        }

        // Restore Safe Transactions
        if (!backup.safeTransactions.isNullOrEmpty()) {
            safeDao.insertSafeTransactions(backup.safeTransactions)
        }
    }

    suspend fun getInvoiceWithItems(id: Long): InvoiceWithItems? = withContext(Dispatchers.IO) {
        invoiceDao.getInvoiceWithItemsById(id)
    }
}

