package com.example.data.local

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers WHERE userId = :userId AND isDeleted = 0")
    fun getCustomersFlow(userId: String): Flow<List<Customer>>

    @Query("SELECT * FROM customers WHERE userId = :userId AND isDeleted = 0")
    suspend fun getCustomersList(userId: String): List<Customer>

    @Query("SELECT * FROM customers WHERE isSynced = 0")
    suspend fun getUnsyncedCustomers(): List<Customer>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: Customer)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomers(customers: List<Customer>)

    @Query("UPDATE customers SET isDeleted = 1, isSynced = 0, updatedAt = :timestamp WHERE id = :id")
    suspend fun softDeleteCustomer(id: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE customers SET isSynced = 1 WHERE id = :id")
    suspend fun markSynced(id: String)
}

@Dao
interface MeasurementFieldDao {
    @Query("SELECT * FROM measurement_fields WHERE userId = :userId AND isDeleted = 0 ORDER BY displayOrder ASC")
    fun getFieldsFlow(userId: String): Flow<List<MeasurementField>>

    @Query("SELECT * FROM measurement_fields WHERE userId = :userId AND isDeleted = 0 ORDER BY displayOrder ASC")
    suspend fun getFieldsList(userId: String): List<MeasurementField>

    @Query("SELECT * FROM measurement_fields WHERE isSynced = 0")
    suspend fun getUnsyncedFields(): List<MeasurementField>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertField(field: MeasurementField)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFields(fields: List<MeasurementField>)

    @Query("UPDATE measurement_fields SET isDeleted = 1, isSynced = 0, updatedAt = :timestamp WHERE id = :id")
    suspend fun softDeleteField(id: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE measurement_fields SET isSynced = 1 WHERE id = :id")
    suspend fun markSynced(id: String)
}

@Dao
interface MeasurementValueDao {
    @Query("SELECT * FROM measurement_values WHERE userId = :userId AND isDeleted = 0")
    fun getValuesFlow(userId: String): Flow<List<MeasurementValue>>

    @Query("SELECT * FROM measurement_values WHERE customerId = :customerId AND userId = :userId AND isDeleted = 0")
    fun getValuesByCustomerFlow(customerId: String, userId: String): Flow<List<MeasurementValue>>

    @Query("SELECT * FROM measurement_values WHERE customerId = :customerId AND userId = :userId AND isDeleted = 0")
    suspend fun getValuesByCustomer(customerId: String, userId: String): List<MeasurementValue>

    @Query("SELECT * FROM measurement_values WHERE userId = :userId AND isDeleted = 0")
    suspend fun getValuesList(userId: String): List<MeasurementValue>

    @Query("SELECT * FROM measurement_values WHERE isSynced = 0")
    suspend fun getUnsyncedValues(): List<MeasurementValue>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertValue(value: MeasurementValue)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertValues(values: List<MeasurementValue>)

    @Query("UPDATE measurement_values SET isDeleted = 1, isSynced = 0, updatedAt = :timestamp WHERE id = :id")
    suspend fun softDeleteValue(id: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE measurement_values SET isSynced = 1 WHERE id = :id")
    suspend fun markSynced(id: String)
}

@Dao
interface InventoryDao {
    @Query("SELECT * FROM inventory_items WHERE userId = :userId AND isDeleted = 0")
    fun getInventoryFlow(userId: String): Flow<List<InventoryItem>>

    @Query("SELECT * FROM inventory_items WHERE userId = :userId AND isDeleted = 0")
    suspend fun getInventoryList(userId: String): List<InventoryItem>

    @Query("SELECT * FROM inventory_items WHERE isSynced = 0")
    suspend fun getUnsyncedInventory(): List<InventoryItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: InventoryItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<InventoryItem>)

    @Query("UPDATE inventory_items SET isDeleted = 1, isSynced = 0, updatedAt = :timestamp WHERE id = :id")
    suspend fun softDeleteItem(id: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE inventory_items SET isSynced = 1 WHERE id = :id")
    suspend fun markSynced(id: String)
}

@Dao
interface LedgerDao {
    @Query("SELECT * FROM ledger_records WHERE userId = :userId AND isDeleted = 0")
    fun getLedgerFlow(userId: String): Flow<List<LedgerRecord>>

    @Query("SELECT * FROM ledger_records WHERE customerId = :customerId AND userId = :userId AND isDeleted = 0 LIMIT 1")
    suspend fun getLedgerForCustomer(customerId: String, userId: String): LedgerRecord?

    @Query("SELECT * FROM ledger_records WHERE customerId = :customerId AND userId = :userId AND isDeleted = 0")
    fun getLedgerForCustomerFlow(customerId: String, userId: String): Flow<LedgerRecord?>

    @Query("SELECT * FROM ledger_records WHERE userId = :userId AND isDeleted = 0")
    suspend fun getLedgerList(userId: String): List<LedgerRecord>

    @Query("SELECT * FROM ledger_records WHERE isSynced = 0")
    suspend fun getUnsyncedLedger(): List<LedgerRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLedger(ledger: LedgerRecord)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLedgers(ledgers: List<LedgerRecord>)

    @Query("UPDATE ledger_records SET isDeleted = 1, isSynced = 0, updatedAt = :timestamp WHERE id = :id")
    suspend fun softDeleteLedger(id: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE ledger_records SET isSynced = 1 WHERE id = :id")
    suspend fun markSynced(id: String)
}

@Dao
interface SizingTemplateDao {
    @Query("SELECT * FROM sizing_templates WHERE userId = :userId AND isDeleted = 0")
    fun getTemplatesFlow(userId: String): Flow<List<SizingTemplate>>

    @Query("SELECT * FROM sizing_templates WHERE userId = :userId AND isDeleted = 0")
    suspend fun getTemplatesList(userId: String): List<SizingTemplate>

    @Query("SELECT * FROM sizing_templates WHERE isSynced = 0")
    suspend fun getUnsyncedTemplates(): List<SizingTemplate>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: SizingTemplate)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplates(templates: List<SizingTemplate>)

    @Query("UPDATE sizing_templates SET isDeleted = 1, isSynced = 0, updatedAt = :timestamp WHERE id = :id")
    suspend fun softDeleteTemplate(id: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE sizing_templates SET isSynced = 1 WHERE id = :id")
    suspend fun markSynced(id: String)
}

@Dao
interface CustomerMeasurementDao {
    @Query("SELECT * FROM customer_measurements WHERE userId = :userId AND isDeleted = 0")
    fun getMeasurementsFlow(userId: String): Flow<List<CustomerMeasurement>>

    @Query("SELECT * FROM customer_measurements WHERE customerId = :customerId AND userId = :userId AND isDeleted = 0")
    fun getMeasurementsForCustomerFlow(customerId: String, userId: String): Flow<List<CustomerMeasurement>>

    @Query("SELECT * FROM customer_measurements WHERE userId = :userId AND isDeleted = 0")
    suspend fun getMeasurementsList(userId: String): List<CustomerMeasurement>

    @Query("SELECT * FROM customer_measurements WHERE customerId = :customerId AND userId = :userId AND isDeleted = 0")
    suspend fun getMeasurementsForCustomer(customerId: String, userId: String): List<CustomerMeasurement>

    @Query("SELECT * FROM customer_measurements WHERE isSynced = 0")
    suspend fun getUnsyncedMeasurements(): List<CustomerMeasurement>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeasurement(measurement: CustomerMeasurement)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeasurements(measurements: List<CustomerMeasurement>)

    @Query("UPDATE customer_measurements SET isDeleted = 1, isSynced = 0, updatedAt = :timestamp WHERE id = :id")
    suspend fun softDeleteMeasurement(id: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE customer_measurements SET isSynced = 1 WHERE id = :id")
    suspend fun markSynced(id: String)
}

@Dao
interface PaymentRecordDao {
    @Query("SELECT * FROM payment_records WHERE userId = :userId AND isDeleted = 0 ORDER BY paymentDate DESC")
    fun getPaymentRecordsFlow(userId: String): Flow<List<PaymentRecord>>

    @Query("SELECT * FROM payment_records WHERE customerId = :customerId AND userId = :userId AND isDeleted = 0 ORDER BY paymentDate DESC")
    fun getPaymentRecordsForCustomerFlow(customerId: String, userId: String): Flow<List<PaymentRecord>>

    @Query("SELECT * FROM payment_records WHERE customerId = :customerId AND userId = :userId AND isDeleted = 0 ORDER BY paymentDate DESC")
    suspend fun getPaymentRecordsForCustomer(customerId: String, userId: String): List<PaymentRecord>

    @Query("SELECT * FROM payment_records WHERE userId = :userId AND isDeleted = 0")
    suspend fun getPaymentRecordsList(userId: String): List<PaymentRecord>

    @Query("SELECT * FROM payment_records WHERE isSynced = 0")
    suspend fun getUnsyncedPaymentRecords(): List<PaymentRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPaymentRecord(record: PaymentRecord)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPaymentRecords(records: List<PaymentRecord>)

    @Query("UPDATE payment_records SET isDeleted = 1, isSynced = 0, updatedAt = :timestamp WHERE id = :id")
    suspend fun softDeletePaymentRecord(id: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE payment_records SET isSynced = 1 WHERE id = :id")
    suspend fun markSynced(id: String)
}

@Dao
interface OrderRecordDao {
    @Query("SELECT * FROM order_records WHERE userId = :userId AND isDeleted = 0 ORDER BY orderDate DESC")
    fun getOrderRecordsFlow(userId: String): Flow<List<OrderRecord>>

    @Query("SELECT * FROM order_records WHERE customerId = :customerId AND userId = :userId AND isDeleted = 0 ORDER BY orderDate DESC")
    fun getOrderRecordsForCustomerFlow(customerId: String, userId: String): Flow<List<OrderRecord>>

    @Query("SELECT * FROM order_records WHERE customerId = :customerId AND userId = :userId AND isDeleted = 0 ORDER BY orderDate DESC")
    suspend fun getOrderRecordsForCustomer(customerId: String, userId: String): List<OrderRecord>

    @Query("SELECT * FROM order_records WHERE userId = :userId AND isDeleted = 0")
    suspend fun getOrderRecordsList(userId: String): List<OrderRecord>

    @Query("SELECT * FROM order_records WHERE isSynced = 0")
    suspend fun getUnsyncedOrderRecords(): List<OrderRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrderRecord(record: OrderRecord)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrderRecords(records: List<OrderRecord>)

    @Query("UPDATE order_records SET isDeleted = 1, isSynced = 0, updatedAt = :timestamp WHERE id = :id")
    suspend fun softDeleteOrderRecord(id: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE order_records SET isSynced = 1 WHERE id = :id")
    suspend fun markSynced(id: String)
}

@Dao
interface EmployeeDao {
    @Query("SELECT * FROM employees ORDER BY name ASC")
    fun getAllEmployeesFlow(): Flow<List<Employee>>

    @Query("SELECT * FROM employees ORDER BY name ASC")
    suspend fun getAllEmployeesList(): List<Employee>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmployee(employee: Employee)

    @Query("DELETE FROM employees WHERE id = :id")
    suspend fun deleteEmployee(id: String)

    @Query("UPDATE employees SET isCycleOpen = :isCycleOpen WHERE id = :id")
    suspend fun updateEmployeeCycleStatus(id: String, isCycleOpen: Boolean)
}

@Dao
interface EmployeeWorkRecordDao {
    @Query("SELECT * FROM employee_work_records WHERE employeeId = :employeeId ORDER BY date DESC")
    fun getWorkRecordsForEmployeeFlow(employeeId: String): Flow<List<EmployeeWorkRecord>>

    @Query("SELECT * FROM employee_work_records ORDER BY date DESC")
    fun getAllWorkRecordsFlow(): Flow<List<EmployeeWorkRecord>>

    @Query("SELECT * FROM employee_work_records WHERE employeeId = :employeeId ORDER BY date DESC")
    suspend fun getWorkRecordsForEmployee(employeeId: String): List<EmployeeWorkRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkRecord(record: EmployeeWorkRecord)

    @Query("SELECT * FROM employee_work_records WHERE id = :id LIMIT 1")
    suspend fun getWorkRecordById(id: String): EmployeeWorkRecord?

    @Query("DELETE FROM employee_work_records WHERE id = :id")
    suspend fun deleteWorkRecord(id: String)

    @Query("UPDATE employee_work_records SET isClosed = 1 WHERE employeeId = :employeeId AND isClosed = 0")
    suspend fun closeActiveWorkRecords(employeeId: String)
}

@Dao
interface EmployeePaymentRecordDao {
    @Query("SELECT * FROM employee_payment_records WHERE employeeId = :employeeId ORDER BY date DESC")
    fun getPaymentRecordsForEmployeeFlow(employeeId: String): Flow<List<EmployeePaymentRecord>>

    @Query("SELECT * FROM employee_payment_records ORDER BY date DESC")
    fun getAllPaymentRecordsFlow(): Flow<List<EmployeePaymentRecord>>

    @Query("SELECT * FROM employee_payment_records WHERE employeeId = :employeeId ORDER BY date DESC")
    suspend fun getPaymentRecordsForEmployee(employeeId: String): List<EmployeePaymentRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPaymentRecord(record: EmployeePaymentRecord)

    @Query("DELETE FROM employee_payment_records WHERE id = :id")
    suspend fun deletePaymentRecord(id: String)

    @Query("UPDATE employee_payment_records SET isClosed = 1 WHERE employeeId = :employeeId AND isClosed = 0")
    suspend fun closeActivePaymentRecords(employeeId: String)
}


