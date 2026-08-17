package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class CustomerTransactionType(val titleAr: String) {
    SALE_CREDIT("فاتورة بيع آجل (+)"),
    PAYMENT_RECEIVED("سداد دفعة نقدية (-)"),
    DEBT_ADJUSTMENT("تسوية رصيد")
}

@Entity(tableName = "customer_transactions")
data class CustomerTransaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val customerId: Long,
    val type: CustomerTransactionType,
    val amount: Double,
    val previousBalance: Double = 0.0,
    val newBalance: Double = 0.0,
    val notes: String = "",
    val invoiceId: Long? = null,
    val timestamp: Long = System.currentTimeMillis()
)
