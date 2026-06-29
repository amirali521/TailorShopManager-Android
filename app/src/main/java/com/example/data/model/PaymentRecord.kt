package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "payment_records")
data class PaymentRecord(
    @PrimaryKey val id: String = "",
    val userId: String = "",
    val customerId: String = "",
    val ledgerId: String = "",
    val amountPaid: Double = 0.0,
    val paymentDate: Long = System.currentTimeMillis(),
    val note: String = "",
    val updatedAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false,
    val isDeleted: Boolean = false
)
