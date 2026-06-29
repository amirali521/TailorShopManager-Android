package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "employees")
data class Employee(
    @PrimaryKey val id: String = "",
    val name: String = "",
    val phoneOrEmail: String = "", // matches helper sign-in unique identifier
    val role: String = "", // Tailor, Master, Cutter, etc.
    val defaultPieceRate: Double = 0.0,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val rateItemsJson: String = "",
    val isCycleOpen: Boolean = true
)
