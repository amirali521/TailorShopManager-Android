package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "measurement_fields")
data class MeasurementField(
    @PrimaryKey val id: String = "", // UUID
    val userId: String = "",
    val templateId: String = "", // Links to SizingTemplate
    val label: String = "", // Field title (e.g., Chest, Collar)
    val type: String = "", // "TEXT", "NUMBER", "RADIO", "CHECKBOX"
    val options: String = "", // Comma-separated options for radio/checkbox
    val displayOrder: Int = 0,
    val updatedAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false,
    val isDeleted: Boolean = false,
    val isDemand: Boolean = false,
    val hasSubInput: Boolean = false,
    val subInputType: String = "",
    val subInputLabel: String = "",
    val subInputOptions: String = "",
    val subInputTargetOption: String = "",
    val hasNoteInput: Boolean = false
)
