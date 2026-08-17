package com.example.data.model

data class CartItem(
    val product: Product,
    val quantity: Double = 1.0,
    val customPrice: Double = product.sellPrice
) {
    val total: Double get() = quantity * customPrice
    val profit: Double get() = (customPrice - product.purchasePrice) * quantity
}
