package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "employee_payment_records")
data class EmployeePaymentRecord(
    @PrimaryKey val id: String = "",
    val employeeId: String = "",
    val paymentType: String = "", // "PAYMENT" or "ADVANCE"
    val amount: Double = 0.0,
    val date: Long = System.currentTimeMillis(),
    val notes: String = "",
    val isClosed: Boolean = false
)
