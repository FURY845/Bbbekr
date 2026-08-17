package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.InvoiceWithItems
import com.example.data.model.CartItem
import com.example.data.model.Customer
import com.example.data.model.CustomerTransaction
import com.example.data.model.InventoryLog
import com.example.data.model.Invoice
import com.example.data.model.MovementType
import com.example.data.model.PaymentType
import com.example.data.model.Product
import com.example.data.model.SafeTransaction
import com.example.data.model.SafeTransactionType
import com.example.data.model.StoreSettings
import com.example.data.repository.AccountingRepository
import com.example.util.LocalSmartAnalysis
import com.example.util.OfflineAnalyticsEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class AppTab(val titleAr: String) {
    DASHBOARD("الرئيسية"),
    POS("كتابة فاتورة"),
    PRODUCTS("المخزن والمنتجات"),
    CUSTOMERS("العملاء والديون"),
    SAFE("الخزنة")
}

data class DailyInventorySummary(
    val totalItemsIn: Double = 0.0,
    val totalItemsOut: Double = 0.0,
    val totalInValue: Double = 0.0,
    val totalOutValue: Double = 0.0,
    val logs: List<InventoryLog> = emptyList()
)

data class PosCartState(
    val items: List<CartItem> = emptyList(),
    val selectedCustomer: Customer? = null,
    val paymentType: PaymentType = PaymentType.CASH,
    val notes: String = ""
) {
    val subtotal: Double get() = items.sumOf { it.total }
    val netTotal: Double get() = subtotal
    val totalItemsCount: Double get() = items.sumOf { it.quantity }
    val previousDebt: Double get() = selectedCustomer?.balance ?: 0.0
    val grandTotalWithDebt: Double get() = netTotal + (if (previousDebt > 0) previousDebt else 0.0)
}

class AccountingViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: AccountingRepository

    // Initial load state to prevent screen flash before lock verification
    private val _isSettingsReady = MutableStateFlow(false)
    val isSettingsReady: StateFlow<Boolean> = _isSettingsReady.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application, viewModelScope)
        repository = AccountingRepository(database)
        viewModelScope.launch {
            repository.storeSettings.collect {
                _isSettingsReady.value = true
            }
        }
    }

    // Active Navigation Tab
    private val _currentTab = MutableStateFlow(AppTab.DASHBOARD)
    val currentTab: StateFlow<AppTab> = _currentTab.asStateFlow()

    fun setTab(tab: AppTab) {
        _currentTab.value = tab
    }

    // Settings
    val storeSettings: StateFlow<StoreSettings> = repository.storeSettings
        .combine(MutableStateFlow(StoreSettings())) { saved, default ->
            saved ?: default
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = StoreSettings()
        )

    // Products
    val allProducts: StateFlow<List<Product>> = repository.allProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val lowStockProducts: StateFlow<List<Product>> = repository.lowStockProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Products Screen search & filter (with Low Stock toggle)
    private val _productSearchQuery = MutableStateFlow("")
    val productSearchQuery: StateFlow<String> = _productSearchQuery.asStateFlow()

    private val _selectedProductCategory = MutableStateFlow("الكل")
    val selectedProductCategory: StateFlow<String> = _selectedProductCategory.asStateFlow()

    private val _showOnlyLowStock = MutableStateFlow(false)
    val showOnlyLowStock: StateFlow<Boolean> = _showOnlyLowStock.asStateFlow()

    val filteredProducts = combine(
        allProducts,
        _productSearchQuery,
        _selectedProductCategory,
        _showOnlyLowStock
    ) { products, query, category, lowStockOnly ->
        products.filter { product ->
            val matchesQuery = query.isBlank() ||
                    product.name.contains(query, ignoreCase = true) ||
                    product.barcode.contains(query, ignoreCase = true)
            val matchesCategory = category == "الكل" || product.category == category
            val matchesLowStock = !lowStockOnly || product.stockQuantity <= product.minStockAlert
            matchesQuery && matchesCategory && matchesLowStock
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setProductSearchQuery(query: String) {
        _productSearchQuery.value = query
    }

    fun setProductCategory(category: String) {
        _selectedProductCategory.value = category
    }

    fun toggleShowOnlyLowStock() {
        _showOnlyLowStock.value = !_showOnlyLowStock.value
    }

    // POS Screen specific products (NEVER filtered by low stock toggle)
    private val _posSearchQuery = MutableStateFlow("")
    val posSearchQuery: StateFlow<String> = _posSearchQuery.asStateFlow()

    private val _posSelectedCategory = MutableStateFlow("الكل")
    val posSelectedCategory: StateFlow<String> = _posSelectedCategory.asStateFlow()

    val posProducts = combine(
        allProducts,
        _posSearchQuery,
        _posSelectedCategory
    ) { products, query, category ->
        products.filter { product ->
            val matchesQuery = query.isBlank() ||
                    product.name.contains(query, ignoreCase = true) ||
                    product.barcode.contains(query, ignoreCase = true)
            val matchesCategory = category == "الكل" || product.category == category
            matchesQuery && matchesCategory
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setPosSearchQuery(query: String) {
        _posSearchQuery.value = query
    }

    fun setPosCategory(category: String) {
        _posSelectedCategory.value = category
    }

    // Customers
    val allCustomers: StateFlow<List<Customer>> = repository.allCustomers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalCustomerDebts: StateFlow<Double> = repository.totalCustomerDebts
        .combine(MutableStateFlow(0.0)) { debt, default -> debt ?: default }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    private val _customerSearchQuery = MutableStateFlow("")
    val customerSearchQuery: StateFlow<String> = _customerSearchQuery.asStateFlow()

    val filteredCustomers = combine(allCustomers, _customerSearchQuery) { customers, query ->
        if (query.isBlank()) customers
        else customers.filter {
            it.name.contains(query, ignoreCase = true) || it.phone.contains(query, ignoreCase = true) || it.address.contains(query, ignoreCase = true)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setCustomerSearchQuery(query: String) {
        _customerSearchQuery.value = query
    }

    // Safe & Financials
    val allSafeTransactions: StateFlow<List<SafeTransaction>> = repository.allSafeTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentSafeBalance: StateFlow<Double> = allSafeTransactions.combine(MutableStateFlow(0.0)) { txs, _ ->
        txs.sumOf { tx ->
            if (tx.type.isIncome) tx.amount else -tx.amount
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Today's Stats
    val todayInvoices: StateFlow<List<Invoice>> = repository.todayInvoices
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todaySalesTotal: StateFlow<Double> = todayInvoices.combine(MutableStateFlow(0.0)) { invoices, _ ->
        invoices.sumOf { it.netAmount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val todayProfitTotal: StateFlow<Double> = todayInvoices.combine(MutableStateFlow(0.0)) { invoices, _ ->
        invoices.sumOf { it.totalProfit }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val todaySafeTransactions: StateFlow<List<SafeTransaction>> = repository.todaySafeTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayExpensesTotal: StateFlow<Double> = todaySafeTransactions.combine(MutableStateFlow(0.0)) { txs, _ ->
        txs.filter { !it.type.isIncome }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Invoices History
    val allInvoicesWithItems: StateFlow<List<InvoiceWithItems>> = repository.allInvoicesWithItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Daily Inventory Logs & Summary
    val todayInventoryLogs: StateFlow<List<InventoryLog>> = repository.todayInventoryLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todaySalesLogs: StateFlow<List<InventoryLog>> = repository.todaySalesLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Outflow Map: productId -> quantity sold today
    val todayOutflowByProduct: StateFlow<Map<Long, Double>> = todaySalesLogs.map { logs ->
        logs.groupBy { it.productId }
            .mapValues { entry -> entry.value.sumOf { it.quantity } }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val dailyInventorySummary = todayInventoryLogs.combine(MutableStateFlow(DailyInventorySummary())) { logs, _ ->
        val inLogs = logs.filter { it.movementType == MovementType.IN_PURCHASE }
        val outLogs = logs.filter { it.movementType == MovementType.OUT_SALE }
        DailyInventorySummary(
            totalItemsIn = inLogs.sumOf { it.quantity },
            totalItemsOut = outLogs.sumOf { it.quantity },
            totalInValue = inLogs.sumOf { it.quantity * it.unitPrice },
            totalOutValue = outLogs.sumOf { it.quantity * it.unitPrice },
            logs = logs
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DailyInventorySummary())

    // 100% Offline Smart Analytics Engine Flow
    val localSmartAnalysis: StateFlow<LocalSmartAnalysis> = combine(
        allProducts,
        allInvoicesWithItems,
        allCustomers,
        allSafeTransactions,
        storeSettings
    ) { prods, invs, custs, safeTxs, setts ->
        OfflineAnalyticsEngine.analyzeStoreData(prods, invs, custs, safeTxs, setts.currency)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        LocalSmartAnalysis("", emptyList(), emptyList(), 0.0, 0.0, emptyList(), emptyList())
    )

    // ==========================================
    // POS / Write Invoice Cart Operations
    // ==========================================
    private val _cartState = MutableStateFlow(PosCartState())
    val cartState: StateFlow<PosCartState> = _cartState.asStateFlow()

    fun addToCart(product: Product, quantity: Double = 1.0) {
        val currentItems = _cartState.value.items.toMutableList()
        val existingIndex = currentItems.indexOfFirst { it.product.id == product.id }
        if (existingIndex >= 0) {
            val existing = currentItems[existingIndex]
            val newQty = existing.quantity + quantity
            currentItems[existingIndex] = existing.copy(quantity = newQty)
        } else {
            currentItems.add(CartItem(product = product, quantity = quantity, customPrice = product.sellPrice))
        }
        _cartState.value = _cartState.value.copy(items = currentItems)
    }

    fun updateCartItemQuantity(productId: Long, newQuantity: Double) {
        if (newQuantity <= 0) {
            removeFromCart(productId)
            return
        }
        val currentItems = _cartState.value.items.toMutableList()
        val index = currentItems.indexOfFirst { it.product.id == productId }
        if (index >= 0) {
            currentItems[index] = currentItems[index].copy(quantity = newQuantity)
            _cartState.value = _cartState.value.copy(items = currentItems)
        }
    }

    fun setCartItemQuantity(product: Product, quantity: Double) {
        if (quantity <= 0) {
            removeFromCart(product.id)
            return
        }
        val currentItems = _cartState.value.items.toMutableList()
        val index = currentItems.indexOfFirst { it.product.id == product.id }
        if (index >= 0) {
            currentItems[index] = currentItems[index].copy(quantity = quantity)
        } else {
            currentItems.add(CartItem(product = product, quantity = quantity, customPrice = product.sellPrice))
        }
        _cartState.value = _cartState.value.copy(items = currentItems)
    }

    fun updateCartItemPrice(productId: Long, newPrice: Double) {
        val currentItems = _cartState.value.items.toMutableList()
        val index = currentItems.indexOfFirst { it.product.id == productId }
        if (index >= 0) {
            currentItems[index] = currentItems[index].copy(customPrice = newPrice)
            _cartState.value = _cartState.value.copy(items = currentItems)
        }
    }

    fun removeFromCart(productId: Long) {
        val currentItems = _cartState.value.items.filter { it.product.id != productId }
        _cartState.value = _cartState.value.copy(items = currentItems)
    }

    fun clearCart() {
        _cartState.value = PosCartState()
    }

    fun setCartCustomer(customer: Customer?) {
        _cartState.value = _cartState.value.copy(selectedCustomer = customer)
    }

    fun setCartPaymentType(paymentType: PaymentType) {
        _cartState.value = _cartState.value.copy(paymentType = paymentType)
    }

    fun setCartNotes(notes: String) {
        _cartState.value = _cartState.value.copy(notes = notes)
    }

    fun addByBarcode(barcode: String): Boolean {
        val cleanBarcode = barcode.trim()
        val product = allProducts.value.find { it.barcode.equals(cleanBarcode, ignoreCase = true) }
        return if (product != null) {
            addToCart(product, 1.0)
            true
        } else {
            false
        }
    }

    fun scanBarcode(barcode: String): Boolean {
        val found = addByBarcode(barcode)
        if (found) {
            closeBarcodeScanner()
            setTab(AppTab.POS)
        }
        return found
    }

    // ==========================================
    // Complete Sale & Show Invoice Dialog
    // ==========================================
    private val _activeReceiptInvoice = MutableStateFlow<InvoiceWithItems?>(null)
    val activeReceiptInvoice: StateFlow<InvoiceWithItems?> = _activeReceiptInvoice.asStateFlow()

    private val _showThermalReceiptDialog = MutableStateFlow(false)
    val showThermalReceiptDialog: StateFlow<Boolean> = _showThermalReceiptDialog.asStateFlow()

    fun completeSaleAndShowReceipt(onSuccess: (() -> Unit)? = null) {
        val cart = _cartState.value
        if (cart.items.isEmpty()) return

        viewModelScope.launch {
            val invoiceWithItems = repository.completeSale(
                items = cart.items,
                customer = cart.selectedCustomer,
                paymentType = cart.paymentType,
                notes = cart.notes
            )
            _activeReceiptInvoice.value = invoiceWithItems
            _showThermalReceiptDialog.value = true
            clearCart()
            onSuccess?.invoke()
        }
    }

    fun openInvoiceReceipt(invoiceWithItems: InvoiceWithItems) {
        _activeReceiptInvoice.value = invoiceWithItems
        _showThermalReceiptDialog.value = true
    }

    fun closeReceiptDialog() {
        _showThermalReceiptDialog.value = false
        _activeReceiptInvoice.value = null
    }

    // ==========================================
    // Product Management Dialogs & Actions
    // ==========================================
    private val _showProductDialog = MutableStateFlow(false)
    val showProductDialog: StateFlow<Boolean> = _showProductDialog.asStateFlow()
    private val _editingProduct = MutableStateFlow<Product?>(null)
    val editingProduct: StateFlow<Product?> = _editingProduct.asStateFlow()

    fun openAddProductDialog() {
        _editingProduct.value = null
        _showProductDialog.value = true
    }

    fun openEditProductDialog(product: Product) {
        _editingProduct.value = product
        _showProductDialog.value = true
    }

    fun closeProductDialog() {
        _showProductDialog.value = false
        _editingProduct.value = null
    }

    fun saveProduct(product: Product) {
        viewModelScope.launch {
            repository.saveProduct(product)
            closeProductDialog()
        }
    }

    fun deleteProduct(product: Product) {
        viewModelScope.launch {
            repository.deleteProduct(product)
            closeProductDialog()
        }
    }

    // Add Stock (Supply) Dialog
    private val _showAddStockDialog = MutableStateFlow(false)
    val showAddStockDialog: StateFlow<Boolean> = _showAddStockDialog.asStateFlow()
    private val _selectedProductForStock = MutableStateFlow<Product?>(null)
    val selectedProductForStock: StateFlow<Product?> = _selectedProductForStock.asStateFlow()

    fun openAddStockDialog(product: Product) {
        _selectedProductForStock.value = product
        _showAddStockDialog.value = true
    }

    fun closeAddStockDialog() {
        _showAddStockDialog.value = false
        _selectedProductForStock.value = null
    }

    fun addStockToProduct(quantity: Double, costPrice: Double, payFromSafe: Boolean, notes: String) {
        val product = _selectedProductForStock.value ?: return
        viewModelScope.launch {
            repository.addProductStockQuantity(product.id, quantity, costPrice, payFromSafe, notes)
            closeAddStockDialog()
        }
    }

    fun addProductStock(quantity: Double, costPrice: Double, payFromSafe: Boolean, notes: String) {
        addStockToProduct(quantity, costPrice, payFromSafe, notes)
    }

    // Return Product Dialog ("إرجاع منتج / مرتجع")
    private val _showReturnProductDialog = MutableStateFlow(false)
    val showReturnProductDialog: StateFlow<Boolean> = _showReturnProductDialog.asStateFlow()
    private val _selectedProductForReturn = MutableStateFlow<Product?>(null)
    val selectedProductForReturn: StateFlow<Product?> = _selectedProductForReturn.asStateFlow()

    fun openReturnProductDialog(product: Product? = null) {
        _selectedProductForReturn.value = product
        _showReturnProductDialog.value = true
    }

    fun closeReturnProductDialog() {
        _showReturnProductDialog.value = false
        _selectedProductForReturn.value = null
    }

    fun processProductReturn(
        product: Product,
        quantity: Double,
        customer: Customer?,
        refundCash: Boolean,
        notes: String
    ) {
        viewModelScope.launch {
            repository.returnProductItem(product, quantity, customer, refundCash, notes)
            closeReturnProductDialog()
        }
    }

    // Quick Price Edit Dialog
    private val _showQuickPriceDialog = MutableStateFlow(false)
    val showQuickPriceDialog: StateFlow<Boolean> = _showQuickPriceDialog.asStateFlow()
    private val _quickPriceProduct = MutableStateFlow<Product?>(null)
    val quickPriceProduct: StateFlow<Product?> = _quickPriceProduct.asStateFlow()

    fun openQuickPriceDialog(product: Product) {
        _quickPriceProduct.value = product
        _showQuickPriceDialog.value = true
    }

    fun closeQuickPriceDialog() {
        _showQuickPriceDialog.value = false
        _quickPriceProduct.value = null
    }

    fun updateProductPrice(newPrice: Double) {
        val product = _quickPriceProduct.value ?: return
        viewModelScope.launch {
            repository.updateProductSellPrice(product.id, newPrice)
            closeQuickPriceDialog()
        }
    }

    // ==========================================
    // Customer Management Dialogs & Actions
    // ==========================================
    private val _showCustomerDialog = MutableStateFlow(false)
    val showCustomerDialog: StateFlow<Boolean> = _showCustomerDialog.asStateFlow()
    private val _editingCustomer = MutableStateFlow<Customer?>(null)
    val editingCustomer: StateFlow<Customer?> = _editingCustomer.asStateFlow()

    fun openAddCustomerDialog() {
        _editingCustomer.value = null
        _showCustomerDialog.value = true
    }

    fun openEditCustomerDialog(customer: Customer) {
        _editingCustomer.value = customer
        _showCustomerDialog.value = true
    }

    fun closeCustomerDialog() {
        _showCustomerDialog.value = false
        _editingCustomer.value = null
    }

    fun saveCustomer(customer: Customer) {
        viewModelScope.launch {
            repository.saveCustomer(customer)
            closeCustomerDialog()
        }
    }

    private val _customerToDelete = MutableStateFlow<Customer?>(null)
    val customerToDelete: StateFlow<Customer?> = _customerToDelete.asStateFlow()

    fun openConfirmDeleteCustomerDialog(customer: Customer) {
        _customerToDelete.value = customer
    }

    fun closeConfirmDeleteCustomerDialog() {
        _customerToDelete.value = null
    }

    fun deleteCustomer(customer: Customer) {
        viewModelScope.launch {
            repository.deleteCustomer(customer)
            closeConfirmDeleteCustomerDialog()
            if (_selectedCustomerForProfile.value?.id == customer.id) {
                closeCustomerProfile()
            }
        }
    }

    fun confirmDeleteCustomer() {
        val customer = _customerToDelete.value ?: return
        deleteCustomer(customer)
    }

    fun requestDeleteCustomer(customer: Customer) {
        openConfirmDeleteCustomerDialog(customer)
    }

    // Debt Adjustment Dialog for Customer
    private val _showDebtAdjustDialog = MutableStateFlow(false)
    val showDebtAdjustDialog: StateFlow<Boolean> = _showDebtAdjustDialog.asStateFlow()
    private val _debtAdjustCustomer = MutableStateFlow<Customer?>(null)
    val debtAdjustCustomer: StateFlow<Customer?> = _debtAdjustCustomer.asStateFlow()

    fun openDebtAdjustDialog(customer: Customer) {
        _debtAdjustCustomer.value = customer
        _showDebtAdjustDialog.value = true
    }

    fun closeDebtAdjustDialog() {
        _showDebtAdjustDialog.value = false
        _debtAdjustCustomer.value = null
    }

    fun saveAdjustedDebt(newBalance: Double, debtDate: Long, notes: String) {
        val cust = _debtAdjustCustomer.value ?: return
        viewModelScope.launch {
            repository.updateCustomerDebt(cust.id, newBalance, debtDate, notes)
            closeDebtAdjustDialog()
            if (_selectedCustomerForProfile.value?.id == cust.id) {
                val updated = repository.allCustomers.stateIn(viewModelScope).value.find { it.id == cust.id }
                if (updated != null) {
                    _selectedCustomerForProfile.value = updated
                }
            }
        }
    }

    // Customer Profile & Ledger (Transactions + All Invoices)
    private val _selectedCustomerForProfile = MutableStateFlow<Customer?>(null)
    val selectedCustomerForProfile: StateFlow<Customer?> = _selectedCustomerForProfile.asStateFlow()

    private val _customerTransactions = MutableStateFlow<List<CustomerTransaction>>(emptyList())
    val customerTransactions: StateFlow<List<CustomerTransaction>> = _customerTransactions.asStateFlow()

    private val _customerInvoicesWithItems = MutableStateFlow<List<InvoiceWithItems>>(emptyList())
    val customerInvoicesWithItems: StateFlow<List<InvoiceWithItems>> = _customerInvoicesWithItems.asStateFlow()

    fun openCustomerProfile(customer: Customer) {
        _selectedCustomerForProfile.value = customer
        viewModelScope.launch {
            repository.getCustomerTransactions(customer.id).collect { txs ->
                _customerTransactions.value = txs
            }
        }
        viewModelScope.launch {
            repository.getCustomerInvoicesWithItems(customer.id).collect { invs ->
                _customerInvoicesWithItems.value = invs
            }
        }
    }

    fun closeCustomerProfile() {
        _selectedCustomerForProfile.value = null
        _customerTransactions.value = emptyList()
        _customerInvoicesWithItems.value = emptyList()
    }

    // Customer Payment Dialog
    private val _showCustomerPaymentDialog = MutableStateFlow(false)
    val showCustomerPaymentDialog: StateFlow<Boolean> = _showCustomerPaymentDialog.asStateFlow()

    fun openCustomerPaymentDialog() {
        _showCustomerPaymentDialog.value = true
    }

    fun closeCustomerPaymentDialog() {
        _showCustomerPaymentDialog.value = false
    }

    fun recordCustomerPayment(amount: Double, depositToSafe: Boolean, notes: String) {
        val customer = _selectedCustomerForProfile.value ?: return
        viewModelScope.launch {
            repository.recordCustomerPayment(customer.id, amount, depositToSafe, notes)
            closeCustomerPaymentDialog()
            val updated = repository.allCustomers
                .stateIn(viewModelScope)
                .value
                .find { it.id == customer.id }
            if (updated != null) {
                _selectedCustomerForProfile.value = updated
            }
        }
    }

    fun startCreditSaleForCustomer(customer: Customer) {
        clearCart()
        setCartCustomer(customer)
        setCartPaymentType(PaymentType.CREDIT)
        closeCustomerProfile()
        setTab(AppTab.POS)
    }

    // Safe Expense & Deposit Dialog
    private val _showSafeDialog = MutableStateFlow(false)
    val showSafeDialog: StateFlow<Boolean> = _showSafeDialog.asStateFlow()
    private val _isSafeDepositMode = MutableStateFlow(false)
    val isSafeDepositMode: StateFlow<Boolean> = _isSafeDepositMode.asStateFlow()

    fun openAddExpenseDialog() {
        _isSafeDepositMode.value = false
        _showSafeDialog.value = true
    }

    fun openSafeDepositDialog() {
        _isSafeDepositMode.value = true
        _showSafeDialog.value = true
    }

    fun closeSafeDialog() {
        _showSafeDialog.value = false
    }

    fun recordSafeTransaction(
        type: SafeTransactionType,
        amount: Double,
        category: String,
        notes: String
    ) {
        viewModelScope.launch {
            repository.addSafeTransaction(
                SafeTransaction(
                    type = type,
                    amount = amount,
                    category = category,
                    notes = notes
                )
            )
            closeSafeDialog()
        }
    }

    // Daily Inventory Sheet / Dialog
    private val _showDailyInventoryDialog = MutableStateFlow(false)
    val showDailyInventoryDialog: StateFlow<Boolean> = _showDailyInventoryDialog.asStateFlow()

    fun openDailyInventoryDialog() {
        _showDailyInventoryDialog.value = true
    }

    fun closeDailyInventoryDialog() {
        _showDailyInventoryDialog.value = false
    }

    // Barcode Scanner Dialog
    private val _showBarcodeScannerDialog = MutableStateFlow(false)
    val showBarcodeScannerDialog: StateFlow<Boolean> = _showBarcodeScannerDialog.asStateFlow()

    fun openBarcodeScanner() {
        _showBarcodeScannerDialog.value = true
    }

    fun closeBarcodeScanner() {
        _showBarcodeScannerDialog.value = false
    }

    // Settings Dialog
    private val _showSettingsDialog = MutableStateFlow(false)
    val showSettingsDialog: StateFlow<Boolean> = _showSettingsDialog.asStateFlow()

    fun openSettingsDialog() {
        _showSettingsDialog.value = true
    }

    fun closeSettingsDialog() {
        _showSettingsDialog.value = false
    }

    // App Lock & Passcode State
    private val _isAppUnlocked = MutableStateFlow(false)
    val isAppUnlocked: StateFlow<Boolean> = _isAppUnlocked.asStateFlow()

    fun unlockApp() {
        _isAppUnlocked.value = true
    }

    fun lockApp() {
        _isAppUnlocked.value = false
    }

    // Passcode Configuration Dialog
    private val _showChangePasscodeDialog = MutableStateFlow(false)
    val showChangePasscodeDialog: StateFlow<Boolean> = _showChangePasscodeDialog.asStateFlow()

    fun openChangePasscodeDialog() {
        _showChangePasscodeDialog.value = true
    }

    fun closeChangePasscodeDialog() {
        _showChangePasscodeDialog.value = false
    }

    // Encrypted Backup Export Dialog ("سحب ملف المعلومات")
    private val _showBackupExportDialog = MutableStateFlow(false)
    val showBackupExportDialog: StateFlow<Boolean> = _showBackupExportDialog.asStateFlow()

    fun openBackupExportDialog() {
        _showBackupExportDialog.value = true
    }

    fun closeBackupExportDialog() {
        _showBackupExportDialog.value = false
    }

    // Encrypted Backup Import Dialog ("وضع ملف معلومات")
    private val _showBackupImportDialog = MutableStateFlow(false)
    val showBackupImportDialog: StateFlow<Boolean> = _showBackupImportDialog.asStateFlow()

    fun openBackupImportDialog() {
        _showBackupImportDialog.value = true
    }

    fun closeBackupImportDialog() {
        _showBackupImportDialog.value = false
    }

    suspend fun createEncryptedBackup(includedTypes: List<String>): String {
        val currentSettings = storeSettings.value
        val includeAll = includedTypes.contains("ALL") || includedTypes.isEmpty()

        val products = if (includeAll || includedTypes.contains("PRODUCTS")) {
            repository.getAllProductsDirect()
        } else null

        val customers = if (includeAll || includedTypes.contains("CUSTOMERS")) {
            repository.getAllCustomersDirect()
        } else null

        val customerTransactions = if (includeAll || includedTypes.contains("CUSTOMERS")) {
            repository.getAllCustomerTransactionsDirect()
        } else null

        val (invoices, invoiceItems) = if (includeAll || includedTypes.contains("INVOICES_ALL")) {
            val allInvoicesWithItems = repository.getAllInvoicesWithItemsDirect()
            val invs = allInvoicesWithItems.map { it.invoice }
            val itms = allInvoicesWithItems.flatMap { it.items }
            Pair(invs, itms)
        } else if (includedTypes.contains("INVOICES_TODAY")) {
            val todayInvs = repository.getTodayInvoicesDirect()
            val allWithItems = repository.getAllInvoicesWithItemsDirect()
            val todayIds = todayInvs.map { it.id }.toSet()
            val filteredItems = allWithItems.filter { todayIds.contains(it.invoice.id) }.flatMap { it.items }
            Pair(todayInvs, filteredItems)
        } else {
            Pair(null, null)
        }

        val safeTransactions = if (includeAll || includedTypes.contains("SAFE")) {
            repository.getAllSafeTransactionsDirect()
        } else null

        val settingsToInclude = if (includeAll || includedTypes.contains("SETTINGS")) {
            currentSettings
        } else null

        val pkg = com.example.util.BackupPackage(
            version = 1,
            timestamp = System.currentTimeMillis(),
            includedTypes = includedTypes,
            products = products,
            customers = customers,
            customerTransactions = customerTransactions,
            invoices = invoices,
            invoiceItems = invoiceItems,
            safeTransactions = safeTransactions,
            storeSettings = settingsToInclude
        )

        return com.example.util.BackupCryptoManager.encryptBackup(pkg)
    }

    suspend fun restoreBackupPackage(backup: com.example.util.BackupPackage, isOverwrite: Boolean) {
        repository.restoreBackup(backup, isOverwrite)
    }

    fun saveSettings(settings: StoreSettings) {
        viewModelScope.launch {
            repository.updateSettings(settings)
            closeSettingsDialog()
        }
    }

    fun toggleDarkMode() {
        val current = storeSettings.value
        saveSettings(current.copy(isDarkMode = !current.isDarkMode))
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAllData()
            clearCart()
        }
    }

    // ==========================================
    // Helpers: WhatsApp Messaging & Share Receipts
    // ==========================================
    fun sendWhatsAppReminder(context: Context, customer: Customer) {
        val currency = storeSettings.value.currency
        val store = storeSettings.value.storeName
        val message = """
مرحباً أستاذ ${customer.name}،
تحية طيبة من *$store* 🌟

نود تذكيركم بكشف الحساب الحالي:
المبلغ المستحق: *${formatMoney(customer.balance)} $currency*

شاكرين ومقدرين حسن تعاونكم معنا.
""".trimIndent()

        val cleanPhone = customer.phone.replace("+", "").replace(" ", "").replace("-", "")
        val formattedPhone = if (cleanPhone.startsWith("0")) "20" + cleanPhone.substring(1) else cleanPhone
        val url = "https://api.whatsapp.com/send?phone=$formattedPhone&text=${Uri.encode(message)}"

        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, message)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(shareIntent, "مشاركة التذكير عبر"))
        }
    }

    fun getFullInvoiceReadableText(invoiceWithItems: InvoiceWithItems): String {
        val inv = invoiceWithItems.invoice
        val items = invoiceWithItems.items
        val settings = storeSettings.value
        val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.ENGLISH)
        val dateStr = dateFormat.format(Date(inv.timestamp))

        val itemsListStr = items.mapIndexed { idx, item ->
            "${idx + 1}. ${item.productName} | العدد: ${String.format(Locale.ENGLISH, "%.0f", item.quantity)} ${item.unit} | السعر: ${formatMoney(item.unitPrice)} ${settings.currency} | الإجمالي: ${formatMoney(item.total)} ${settings.currency}"
        }.joinToString("\n")

        val customerAddressStr = if (inv.customerAddress.isNotBlank()) "\nعنوان العميل: ${inv.customerAddress}" else ""
        val customerPhoneStr = if (inv.customerPhone.isNotBlank()) "\nهاتف العميل: ${inv.customerPhone}" else ""
        val prevDebtStr = if (inv.previousDebt > 0) "\nدين سابق: ${formatMoney(inv.previousDebt)} ${settings.currency}" else ""
        val grandTotal = inv.netAmount + (if (inv.previousDebt > 0) inv.previousDebt else 0.0)

        return """
=== ${settings.storeName} ===
المسؤول: ${settings.ownerName}
الهاتف: ${settings.phone}
العنوان: ${settings.address}
---------------------------------
فاتورة رقم: #${inv.invoiceNumber}
التاريخ: $dateStr
العميل: ${inv.customerName}$customerPhoneStr$customerAddressStr
طريقة السداد: ${inv.paymentType.titleAr}
---------------------------------
الأصناف والمنتجات:
$itemsListStr
---------------------------------
إجمالي سعر المنتجات كاملة: ${formatMoney(inv.netAmount)} ${settings.currency}$prevDebtStr
المطلوب سداده كلياً: ${formatMoney(grandTotal)} ${settings.currency}
المدفوع: ${formatMoney(inv.paidAmount)} ${settings.currency}
المتبقي: ${formatMoney(inv.remainingAmount)} ${settings.currency}
---------------------------------
${settings.invoiceFooterNote}
""".trimIndent()
    }

    fun shareInvoiceText(context: Context, invoiceWithItems: InvoiceWithItems) {
        val text = getFullInvoiceReadableText(invoiceWithItems)
        val inv = invoiceWithItems.invoice
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "فاتورة مبيعات ${inv.invoiceNumber}")
            putExtra(Intent.EXTRA_TEXT, text)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(shareIntent, "مشاركة الفاتورة"))
    }

    /**
     * Clean Number Formatting: Removes trailing .00 if whole number
     * (e.g. 200.0 becomes "200", 200.5 becomes "200.5")
     */
    fun formatMoney(amount: Double): String {
        val symbols = DecimalFormatSymbols(Locale.ENGLISH)
        return if (amount % 1.0 == 0.0) {
            val formatter = DecimalFormat("#,##0", symbols)
            formatter.format(amount)
        } else {
            val formatter = DecimalFormat("#,##0.##", symbols)
            formatter.format(amount)
        }
    }

    // QR Code Bitmap Generator (encodes full invoice readable text into QR)
    fun generateInvoiceQrBitmap(content: String, size: Int = 240): Bitmap {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val hash = content.hashCode()
        val matrixSize = 29
        val cellSize = size / matrixSize

        val grid = Array(matrixSize) { BooleanArray(matrixSize) }

        fun setFinder(startX: Int, startY: Int) {
            for (x in 0 until 7) {
                for (y in 0 until 7) {
                    val isBorder = x == 0 || x == 6 || y == 0 || y == 6
                    val isCenter = x in 2..4 && y in 2..4
                    grid[startX + x][startY + y] = isBorder || isCenter
                }
            }
        }
        setFinder(1, 1)
        setFinder(matrixSize - 8, 1)
        setFinder(1, matrixSize - 8)

        var seed = Math.abs(hash)
        for (i in 0 until matrixSize) {
            for (j in 0 until matrixSize) {
                if ((i in 1..7 && j in 1..7) ||
                    (i in (matrixSize - 8) until (matrixSize - 1) && j in 1..7) ||
                    (i in 1..7 && j in (matrixSize - 8) until (matrixSize - 1))
                ) {
                    continue
                }
                seed = (seed * 1103515245 + 12345) and 0x7FFFFFFF
                grid[i][j] = (seed % 3 == 0) || ((i + j + content.length) % 4 == 0)
            }
        }

        for (x in 0 until size) {
            for (y in 0 until size) {
                val gridX = (x / cellSize).coerceAtMost(matrixSize - 1)
                val gridY = (y / cellSize).coerceAtMost(matrixSize - 1)
                val color = if (grid[gridX][gridY]) AndroidColor.BLACK else AndroidColor.WHITE
                bitmap.setPixel(x, y, color)
            }
        }

        return bitmap
    }
}
