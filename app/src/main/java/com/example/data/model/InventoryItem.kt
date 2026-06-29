package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "inventory_items")
data class InventoryItem(
    @PrimaryKey val id: String = "", // UUID
    val userId: String = "",
    val name: String = "",
    val unitType: String = "", // "Meters" or "Suits"
    val purchasePrice: Double = 0.0,
    val sellingPrice: Double = 0.0,
    val stockQuantity: Int = 0,
    val updatedAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false,
    val isDeleted: Boolean = false
) {
    val profitPerUnit: Double
        get() = sellingPrice - purchasePrice

    val totalPotentialProfit: Double
        get() = profitPerUnit * stockQuantity
}
