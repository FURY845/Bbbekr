package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class SafeTransactionType(val isIncome: Boolean, val titleAr: String) {
    INCOME_SALE(true, "مبيعات نقدية"),
    INCOME_CUSTOMER_PAYMENT(true, "تحصيل من عميل"),
    INCOME_DEPOSIT(true, "إيداع نقدي بالخزنة"),
    EXPENSE_GENERAL(false, "مصروف عام"),
    EXPENSE_RENT(false, "إيجار"),
    EXPENSE_BILLS(false, "فواتير كهرباء/مياه/هاتف"),
    EXPENSE_SALARIES(false, "رواتب ومستحقات"),
    EXPENSE_PURCHASE(false, "شراء بضاعة"),
    EXPENSE_REFUND(false, "مرتجع نقدية لعميل"),
    WITHDRAWAL(false, "مسحوبات شخصية")
}

@Entity(tableName = "safe_transactions")
data class SafeTransaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: SafeTransactionType,
    val amount: Double,
    val category: String = "عام",
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val relatedInvoiceId: Long? = null,
    val relatedCustomerId: Long? = null
)
