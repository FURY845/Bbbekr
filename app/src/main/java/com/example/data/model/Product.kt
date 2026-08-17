package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val barcode: String = "",
    val purchasePrice: Double = 0.0,
    val sellPrice: Double = 0.0,
    val stockQuantity: Double = 0.0,
    val minStockAlert: Double = 5.0,
    val category: String = "عام",
    val unit: String = "قطعة",
    val imageUrl: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
