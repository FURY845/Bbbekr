package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class PaymentType(val titleAr: String) {
    CASH("نقدي (كاش)"),
    CREDIT("آجل (على الحساب)"),
    CARD("شبكة / بطاقة"),
    TRANSFER("تحويل بنكي")
}

@Entity(tableName = "invoices")
data class Invoice(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val invoiceNumber: String,
    val customerId: Long? = null,
    val customerName: String = "عميل نقدي",
    val customerPhone: String = "",
    val customerAddress: String = "",
    val previousDebt: Double = 0.0,
    val totalAmount: Double = 0.0, // Subtotal of products
    val discountAmount: Double = 0.0,
    val netAmount: Double = 0.0, // Products Total
    val paidAmount: Double = 0.0,
    val remainingAmount: Double = 0.0,
    val totalCost: Double = 0.0,
    val totalProfit: Double = 0.0,
    val paymentType: PaymentType = PaymentType.CASH,
    val timestamp: Long = System.currentTimeMillis(),
    val notes: String = ""
)
