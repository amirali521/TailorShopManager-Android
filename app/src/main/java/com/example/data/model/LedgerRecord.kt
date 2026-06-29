package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ledger_records")
data class LedgerRecord(
    @PrimaryKey val id: String = "", // UUID
    val userId: String = "",
    val customerId: String = "", // foreign-key-like mapping
    val totalValue: Double = 0.0, // Total Bill
    val amountPaid: Double = 0.0, // Paid
    val pendingDebt: Double = 0.0, // calculated/supplied
    val updatedAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false,
    val isDeleted: Boolean = false
)
