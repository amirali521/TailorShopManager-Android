package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "measurement_values")
data class MeasurementValue(
    @PrimaryKey val id: String = "", // UUID
    val userId: String = "",
    val customerId: String = "",
    val measurementId: String = "default_measurement", // Links to CustomerMeasurement
    val fieldId: String = "",
    val value: String = "", // The actual sizing value entered
    val subValue: String = "", // Added to store sub-input selections
    val note: String = "", // Added to store custom note for this input value
    val updatedAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false,
    val isDeleted: Boolean = false
)
