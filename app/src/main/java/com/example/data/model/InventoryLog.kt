package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class MovementType(val titleAr: String) {
    IN_PURCHASE("شراء / إضافة مخزون"),
    OUT_SALE("مبيعات"),
    RETURN_ITEM("مرتجع بضاعة"),
    ADJUSTMENT("تسوية جردية")
}

@Entity(tableName = "inventory_logs")
data class InventoryLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productId: Long,
    val productName: String,
    val movementType: MovementType,
    val quantity: Double,
    val previousStock: Double = 0.0,
    val newStock: Double = 0.0,
    val unitPrice: Double = 0.0,
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
