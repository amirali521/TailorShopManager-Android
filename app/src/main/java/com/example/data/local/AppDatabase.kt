package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.*

@Database(
    entities = [
        Customer::class,
        MeasurementField::class,
        MeasurementValue::class,
        InventoryItem::class,
        LedgerRecord::class,
        SizingTemplate::class,
        CustomerMeasurement::class,
        PaymentRecord::class,
        OrderRecord::class,
        Employee::class,
        EmployeeWorkRecord::class,
        EmployeePaymentRecord::class
    ],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun customerDao(): CustomerDao
    abstract fun measurementFieldDao(): MeasurementFieldDao
    abstract fun measurementValueDao(): MeasurementValueDao
    abstract fun inventoryDao(): InventoryDao
    abstract fun ledgerDao(): LedgerDao
    abstract fun sizingTemplateDao(): SizingTemplateDao
    abstract fun customerMeasurementDao(): CustomerMeasurementDao
    abstract fun paymentRecordDao(): PaymentRecordDao
    abstract fun orderRecordDao(): OrderRecordDao
    abstract fun employeeDao(): EmployeeDao
    abstract fun employeeWorkRecordDao(): EmployeeWorkRecordDao
    abstract fun employeePaymentRecordDao(): EmployeePaymentRecordDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "tailor_shop_db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
