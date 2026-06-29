package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "customer_measurements")
data class CustomerMeasurement(
    @PrimaryKey val id: String = "", // UUID
    val userId: String = "",
    val customerId: String = "",
    val templateId: String = "", // SizingTemplate ID
    val title: String = "", // e.g. "Formal Suit Fitting"
    val date: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false,
    val isDeleted: Boolean = false
)
