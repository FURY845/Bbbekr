package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "store_settings")
data class StoreSettings(
    @PrimaryKey val id: Int = 1,
    val storeName: String = "مخزن الأمانة",
    val ownerName: String = "المدير المسؤول",
    val phone: String = "01000000000",
    val address: String = "المخزن الرئيسي",
    val taxNumber: String = "",
    val currency: String = "ج",
    val invoiceFooterNote: String = "شكراً لتعاملكم معنا - مخزن الأمانة",
    val isDarkMode: Boolean = false,
    val isAppLockEnabled: Boolean = false,
    val lockType: String = "PIN_4", // "PIN_4", "PIN_6", "PASSWORD", "PATTERN"
    val passcode: String = "",
    val isBiometricEnabled: Boolean = false,
    val invoiceTemplate: String = "MODERN" // "MODERN", "ROYAL", "MINIMAL"
)

