package com.example.data.local

import androidx.room.TypeConverter
import com.example.data.model.CustomerTransactionType
import com.example.data.model.MovementType
import com.example.data.model.PaymentType
import com.example.data.model.SafeTransactionType

class Converters {
    @TypeConverter
    fun fromPaymentType(value: PaymentType): String = value.name

    @TypeConverter
    fun toPaymentType(value: String): PaymentType = try {
        PaymentType.valueOf(value)
    } catch (e: Exception) {
        PaymentType.CASH
    }

    @TypeConverter
    fun fromCustomerTransactionType(value: CustomerTransactionType): String = value.name

    @TypeConverter
    fun toCustomerTransactionType(value: String): CustomerTransactionType = try {
        CustomerTransactionType.valueOf(value)
    } catch (e: Exception) {
        CustomerTransactionType.SALE_CREDIT
    }

    @TypeConverter
    fun fromSafeTransactionType(value: SafeTransactionType): String = value.name

    @TypeConverter
    fun toSafeTransactionType(value: String): SafeTransactionType = try {
        SafeTransactionType.valueOf(value)
    } catch (e: Exception) {
        SafeTransactionType.INCOME_SALE
    }

    @TypeConverter
    fun fromMovementType(value: MovementType): String = value.name

    @TypeConverter
    fun toMovementType(value: String): MovementType = try {
        MovementType.valueOf(value)
    } catch (e: Exception) {
        MovementType.OUT_SALE
    }
}
