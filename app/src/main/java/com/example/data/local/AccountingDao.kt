package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.model.Customer
import com.example.data.model.CustomerTransaction
import com.example.data.model.InventoryLog
import com.example.data.model.Invoice
import com.example.data.model.InvoiceItem
import com.example.data.model.Product
import com.example.data.model.SafeTransaction
import com.example.data.model.StoreSettings
import kotlinx.coroutines.flow.Flow

data class InvoiceWithItems(
    @androidx.room.Embedded val invoice: Invoice,
    @androidx.room.Relation(
        parentColumn = "id",
        entityColumn = "invoiceId"
    )
    val items: List<InvoiceItem>
)

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY name ASC")
    fun getAllProducts(): Flow<List<Product>>

    @Query("SELECT * FROM products ORDER BY name ASC")
    suspend fun getAllProductsDirect(): List<Product>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getProductById(id: Long): Product?

    @Query("SELECT * FROM products WHERE barcode = :barcode LIMIT 1")
    suspend fun getProductByBarcode(barcode: String): Product?

    @Query("SELECT * FROM products WHERE stockQuantity <= minStockAlert")
    fun getLowStockProducts(): Flow<List<Product>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: Product): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<Product>): List<Long>

    @Update
    suspend fun updateProduct(product: Product)

    @Delete
    suspend fun deleteProduct(product: Product)

    @Query("UPDATE products SET stockQuantity = stockQuantity + :diff WHERE id = :productId")
    suspend fun adjustStock(productId: Long, diff: Double)

    @Query("UPDATE products SET sellPrice = :newPrice WHERE id = :productId")
    suspend fun updateSellPrice(productId: Long, newPrice: Double)

    @Query("DELETE FROM products")
    suspend fun clearAllProducts()
}

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers ORDER BY name ASC")
    fun getAllCustomers(): Flow<List<Customer>>

    @Query("SELECT * FROM customers ORDER BY name ASC")
    suspend fun getAllCustomersDirect(): List<Customer>

    @Query("SELECT * FROM customers WHERE id = :id")
    suspend fun getCustomerById(id: Long): Customer?

    @Query("SELECT SUM(balance) FROM customers WHERE balance > 0")
    fun getTotalCustomerDebts(): Flow<Double?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: Customer): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomers(customers: List<Customer>): List<Long>

    @Update
    suspend fun updateCustomer(customer: Customer)

    @Delete
    suspend fun deleteCustomer(customer: Customer)

    @Query("UPDATE customers SET balance = balance + :amountDiff WHERE id = :customerId")
    suspend fun adjustBalance(customerId: Long, amountDiff: Double)

    @Query("UPDATE customers SET balance = :newBalance, debtDate = :debtDate WHERE id = :customerId")
    suspend fun updateBalanceAndDate(customerId: Long, newBalance: Double, debtDate: Long)

    @Query("DELETE FROM customers")
    suspend fun clearAllCustomers()
}

@Dao
interface CustomerTransactionDao {
    @Query("SELECT * FROM customer_transactions WHERE customerId = :customerId ORDER BY timestamp DESC")
    fun getTransactionsForCustomer(customerId: Long): Flow<List<CustomerTransaction>>

    @Query("SELECT * FROM customer_transactions ORDER BY timestamp DESC")
    suspend fun getAllTransactionsDirect(): List<CustomerTransaction>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: CustomerTransaction): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<CustomerTransaction>): List<Long>

    @Query("DELETE FROM customer_transactions WHERE customerId = :customerId")
    suspend fun deleteTransactionsForCustomer(customerId: Long)

    @Query("DELETE FROM customer_transactions")
    suspend fun clearAllTransactions()
}

@Dao
interface InvoiceDao {
    @Transaction
    @Query("SELECT * FROM invoices ORDER BY timestamp DESC")
    fun getAllInvoicesWithItems(): Flow<List<InvoiceWithItems>>

    @Transaction
    @Query("SELECT * FROM invoices WHERE customerId = :customerId ORDER BY timestamp DESC")
    fun getInvoicesWithItemsForCustomer(customerId: Long): Flow<List<InvoiceWithItems>>

    @Transaction
    @Query("SELECT * FROM invoices ORDER BY timestamp DESC")
    suspend fun getAllInvoicesWithItemsDirect(): List<InvoiceWithItems>

    @Transaction
    @Query("SELECT * FROM invoices WHERE id = :id")
    suspend fun getInvoiceWithItemsById(id: Long): InvoiceWithItems?

    @Query("SELECT * FROM invoices WHERE timestamp >= :startTimestamp ORDER BY timestamp DESC")
    fun getInvoicesSince(startTimestamp: Long): Flow<List<Invoice>>

    @Query("SELECT * FROM invoices WHERE timestamp >= :startTimestamp ORDER BY timestamp DESC")
    suspend fun getInvoicesSinceDirect(startTimestamp: Long): List<Invoice>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoice(invoice: Invoice): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoices(invoices: List<Invoice>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoiceItems(items: List<InvoiceItem>)

    @Query("DELETE FROM invoices WHERE id = :id")
    suspend fun deleteInvoice(id: Long)

    @Query("DELETE FROM invoice_items WHERE invoiceId = :invoiceId")
    suspend fun deleteInvoiceItems(invoiceId: Long)

    @Query("DELETE FROM invoices")
    suspend fun clearAllInvoices()

    @Query("DELETE FROM invoice_items")
    suspend fun clearAllInvoiceItems()
}

@Dao
interface SafeDao {
    @Query("SELECT * FROM safe_transactions ORDER BY timestamp DESC")
    fun getAllSafeTransactions(): Flow<List<SafeTransaction>>

    @Query("SELECT * FROM safe_transactions ORDER BY timestamp DESC")
    suspend fun getAllSafeTransactionsDirect(): List<SafeTransaction>

    @Query("SELECT * FROM safe_transactions WHERE timestamp >= :startTimestamp ORDER BY timestamp DESC")
    fun getSafeTransactionsSince(startTimestamp: Long): Flow<List<SafeTransaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSafeTransaction(transaction: SafeTransaction): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSafeTransactions(transactions: List<SafeTransaction>): List<Long>

    @Query("DELETE FROM safe_transactions")
    suspend fun clearAllSafeTransactions()
}

@Dao
interface StoreSettingsDao {
    @Query("SELECT * FROM store_settings WHERE id = 1 LIMIT 1")
    fun getSettings(): Flow<StoreSettings?>

    @Query("SELECT * FROM store_settings WHERE id = 1 LIMIT 1")
    suspend fun getSettingsDirect(): StoreSettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSettings(settings: StoreSettings)
}

@Dao
interface InventoryLogDao {
    @Query("SELECT * FROM inventory_logs WHERE timestamp >= :startTimestamp ORDER BY timestamp DESC")
    fun getLogsSince(startTimestamp: Long): Flow<List<InventoryLog>>

    @Query("SELECT * FROM inventory_logs WHERE movementType = 'OUT_SALE' AND timestamp >= :startTimestamp ORDER BY timestamp DESC")
    fun getTodaySalesLogs(startTimestamp: Long): Flow<List<InventoryLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: InventoryLog): Long

    @Query("DELETE FROM inventory_logs")
    suspend fun clearAllInventoryLogs()
}
