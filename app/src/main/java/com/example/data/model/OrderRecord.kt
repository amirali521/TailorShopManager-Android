package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "order_records")
data class OrderRecord(
    @PrimaryKey val id: String = "",
    val userId: String = "",
    val customerId: String = "",
    val ledgerId: String = "",
    val itemName: String = "", // e.g. "Work: Bridal Dress" or "Inventory: Blue Fabric"
    val price: Double = 0.0,
    val quantity: Int = 1,
    val orderDate: Long = System.currentTimeMillis(),
    val isCompleted: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false,
    val isDeleted: Boolean = false
)
