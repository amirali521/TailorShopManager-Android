package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "employee_work_records")
data class EmployeeWorkRecord(
    @PrimaryKey val id: String = "",
    val employeeId: String = "",
    val suitType: String = "", // e.g. "Kid's Shalwar", "Groom Suit", etc.
    val piecesCount: Int = 1,
    val ratePaid: Double = 0.0,
    val totalAmount: Double = 0.0,
    val date: Long = System.currentTimeMillis(),
    val notes: String = "",
    val isClosed: Boolean = false
)
