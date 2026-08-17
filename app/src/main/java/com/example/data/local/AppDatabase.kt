package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        Product::class,
        Customer::class,
        CustomerTransaction::class,
        Invoice::class,
        InvoiceItem::class,
        SafeTransaction::class,
        StoreSettings::class,
        InventoryLog::class
    ],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun productDao(): ProductDao
    abstract fun customerDao(): CustomerDao
    abstract fun customerTransactionDao(): CustomerTransactionDao
    abstract fun invoiceDao(): InvoiceDao
    abstract fun safeDao(): SafeDao
    abstract fun storeSettingsDao(): StoreSettingsDao
    abstract fun inventoryLogDao(): InventoryLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "alamana_warehouse_v4.db"
                )
                    .addCallback(DatabaseCallback(scope))
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialData(database)
                    }
                }
            }
        }

        suspend fun populateInitialData(database: AppDatabase) {
            val storeSettingsDao = database.storeSettingsDao()

            // 1. Initial Settings with currency = "ج" and Store Name = "مخزن الأمانة"
            storeSettingsDao.saveSettings(
                StoreSettings(
                    id = 1,
                    storeName = "مخزن الأمانة",
                    ownerName = "المدير المسؤول",
                    phone = "01000000000",
                    address = "المخزن الرئيسي",
                    taxNumber = "",
                    currency = "ج",
                    invoiceFooterNote = "شكراً لتعاملكم معنا - مخزن الأمانة",
                    isDarkMode = false
                )
            )
            // Completely empty: 0 products, 0 customers, 0 safe transactions, 0 invoices.
        }
    }
}
