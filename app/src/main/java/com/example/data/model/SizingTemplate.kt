package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sizing_templates")
data class SizingTemplate(
    @PrimaryKey val id: String = "", // UUID
    val userId: String = "",
    val name: String = "", // e.g. "Suit Template", "Shirt Template"
    val updatedAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false,
    val isDeleted: Boolean = false
)
