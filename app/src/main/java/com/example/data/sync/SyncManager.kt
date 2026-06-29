package com.example.data.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.model.*
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class SyncManager(private val context: Context, private val database: AppDatabase) {

    private var firestore: FirebaseFirestore? = null

    private val _syncStatus = MutableStateFlow<String>("Idle")
    val syncStatus: StateFlow<String> = _syncStatus

    init {
        try {
            var initialized = false
            try {
                if (FirebaseApp.getApps(context).isNotEmpty()) {
                    initialized = true
                }
            } catch (e: Exception) {}

            if (!initialized) {
                try {
                    FirebaseApp.initializeApp(context)
                } catch (e: Exception) {}
            }
            firestore = FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.e("SyncManager", "Firebase not available for sync, running in local-only mode", e)
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    suspend fun triggerSync(userId: String) = withContext(Dispatchers.IO) {
        if (!isNetworkAvailable()) {
            _syncStatus.value = "Offline"
            Log.d("SyncManager", "No internet connection. Sync skipped.")
            return@withContext
        }

        val db = firestore
        if (db == null) {
            _syncStatus.value = "Demo Sync Success (Local)"
            Log.d("SyncManager", "Running in local demo mode, simulated sync completed.")
            return@withContext
        }

        _syncStatus.value = "Syncing..."
        Log.d("SyncManager", "Starting sync process for user: $userId")

        try {
            // Create user parent document first so it exists physically in Firestore
            try {
                val firebaseAuth = com.google.firebase.auth.FirebaseAuth.getInstance()
                val authUser = firebaseAuth.currentUser
                val prefs = context.getSharedPreferences("tailor_shop_prefs", Context.MODE_PRIVATE)
                val shopPhone = prefs.getString("shop_phone", "") ?: ""
                if (authUser != null && authUser.uid == userId) {
                    val userRef = db.collection("users").document(userId)
                    val userMap = mapOf(
                        "userId" to userId,
                        "displayName" to (authUser.displayName ?: "Tailor Partner"),
                        "email" to (authUser.email ?: ""),
                        "phone" to shopPhone,
                        "createdAt" to (authUser.metadata?.creationTimestamp ?: System.currentTimeMillis()),
                        "lastSyncAt" to System.currentTimeMillis()
                    )
                    userRef.set(userMap).await()
                    Log.d("SyncManager", "Parent users/$userId record verified & updated in Firestore.")
                } else {
                    val userRef = db.collection("users").document(userId)
                    val userMap = mapOf(
                        "userId" to userId,
                        "phone" to shopPhone,
                        "lastSyncAt" to System.currentTimeMillis()
                    )
                    userRef.set(userMap).await()
                    Log.d("SyncManager", "Parent users/$userId fallback record updated in Firestore.")
                }
            } catch (authEx: Exception) {
                Log.e("SyncManager", "Could not fetch or create matching Auth User record, creating fallback users/$userId document.", authEx)
                val userRef = db.collection("users").document(userId)
                val prefs = context.getSharedPreferences("tailor_shop_prefs", Context.MODE_PRIVATE)
                val shopPhone = prefs.getString("shop_phone", "") ?: ""
                val userMap = mapOf(
                    "userId" to userId,
                    "phone" to shopPhone,
                    "lastSyncAt" to System.currentTimeMillis()
                )
                userRef.set(userMap).await()
            }

            // 1. Sync Customers
            syncCustomers(userId, db)

            // 2. Sync Measurement Fields
            syncMeasurementFields(userId, db)

            // 3. Sync Measurement Values
            syncMeasurementValues(userId, db)

            // 4. Sync Inventory Items
            syncInventory(userId, db)

            // 5. Sync Ledger Records
            syncLedger(userId, db)

            // 6. Sync Sizing Templates
            syncSizingTemplates(userId, db)

            // 7. Sync Customer Measurements
            syncCustomerMeasurements(userId, db)

            // 8. Sync Payment Records
            syncPaymentRecords(userId, db)

            // 9. Sync Order Records
            syncOrderRecords(userId, db)

            _syncStatus.value = "Uploaded & Synced"
            Log.d("SyncManager", "Synchronization complete.")
        } catch (e: Exception) {
            _syncStatus.value = "Sync Error"
            Log.e("SyncManager", "Sync failed", e)
        }
    }

    private suspend fun syncCustomers(userId: String, db: FirebaseFirestore) {
        val customerDao = database.customerDao()

        // Upload unsynced local data
        val unsynced = customerDao.getUnsyncedCustomers().filter { it.userId == userId }
        for (item in unsynced) {
            val docRef = db.collection("users").document(userId)
                .collection("customers").document(item.id)
            if (item.isDeleted) {
                docRef.delete().await()
            } else {
                docRef.set(item).await()
            }
            customerDao.markSynced(item.id)
        }

        // Download cloud data
        val snapshot = db.collection("users").document(userId)
            .collection("customers").get().await()

        val cloudItems = snapshot.toObjects(Customer::class.java)
        val localItems = customerDao.getCustomersList(userId)
        val localMap = localItems.associateBy { it.id }

        val toInsert = mutableListOf<Customer>()
        for (cloud in cloudItems) {
            if (cloud.isDeleted) continue
            val local = localMap[cloud.id]
            if (local == null || cloud.updatedAt > local.updatedAt) {
                toInsert.add(cloud.copy(isSynced = true))
            }
        }
        if (toInsert.isNotEmpty()) {
            customerDao.insertCustomers(toInsert)
        }
    }

    private suspend fun syncMeasurementFields(userId: String, db: FirebaseFirestore) {
        val fieldDao = database.measurementFieldDao()

        // Upload unsynced
        val unsynced = fieldDao.getUnsyncedFields().filter { it.userId == userId }
        for (item in unsynced) {
            val docRef = db.collection("users").document(userId)
                .collection("measurement_fields").document(item.id)
            if (item.isDeleted) {
                docRef.delete().await()
            } else {
                docRef.set(item).await()
            }
            fieldDao.markSynced(item.id)
        }

        // Download
        val snapshot = db.collection("users").document(userId)
            .collection("measurement_fields").get().await()

        val cloudItems = snapshot.toObjects(MeasurementField::class.java)
        val localItems = fieldDao.getFieldsList(userId)
        val localMap = localItems.associateBy { it.id }

        val toInsert = mutableListOf<MeasurementField>()
        for (cloud in cloudItems) {
            if (cloud.isDeleted) continue
            val local = localMap[cloud.id]
            if (local == null || cloud.updatedAt > local.updatedAt) {
                toInsert.add(cloud.copy(isSynced = true))
            }
        }
        if (toInsert.isNotEmpty()) {
            fieldDao.insertFields(toInsert)
        }
    }

    private suspend fun syncMeasurementValues(userId: String, db: FirebaseFirestore) {
        val valueDao = database.measurementValueDao()

        // Upload unsynced
        val unsynced = valueDao.getUnsyncedValues().filter { it.userId == userId }
        for (item in unsynced) {
            val docRef = db.collection("users").document(userId)
                .collection("measurement_values").document(item.id)
            if (item.isDeleted) {
                docRef.delete().await()
            } else {
                docRef.set(item).await()
            }
            valueDao.markSynced(item.id)
        }

        // Download
        val snapshot = db.collection("users").document(userId)
            .collection("measurement_values").get().await()

        val cloudItems = snapshot.toObjects(MeasurementValue::class.java)
        val localItems = valueDao.getValuesList(userId)
        val localMap = localItems.associateBy { it.id }

        val toInsert = mutableListOf<MeasurementValue>()
        for (cloud in cloudItems) {
            if (cloud.isDeleted) continue
            val local = localMap[cloud.id]
            if (local == null || cloud.updatedAt > local.updatedAt) {
                toInsert.add(cloud.copy(isSynced = true))
            }
        }
        if (toInsert.isNotEmpty()) {
            valueDao.insertValues(toInsert)
        }
    }

    private suspend fun syncInventory(userId: String, db: FirebaseFirestore) {
        val inventoryDao = database.inventoryDao()

        // Upload
        val unsynced = inventoryDao.getUnsyncedInventory().filter { it.userId == userId }
        for (item in unsynced) {
            val docRef = db.collection("users").document(userId)
                .collection("inventory_items").document(item.id)
            if (item.isDeleted) {
                docRef.delete().await()
            } else {
                docRef.set(item).await()
            }
            inventoryDao.markSynced(item.id)
        }

        // Download
        val snapshot = db.collection("users").document(userId)
            .collection("inventory_items").get().await()

        val cloudItems = snapshot.toObjects(InventoryItem::class.java)
        val localItems = inventoryDao.getInventoryList(userId)
        val localMap = localItems.associateBy { it.id }

        val toInsert = mutableListOf<InventoryItem>()
        for (cloud in cloudItems) {
            if (cloud.isDeleted) continue
            val local = localMap[cloud.id]
            if (local == null || cloud.updatedAt > local.updatedAt) {
                toInsert.add(cloud.copy(isSynced = true))
            }
        }
        if (toInsert.isNotEmpty()) {
            inventoryDao.insertItems(toInsert)
        }
    }

    private suspend fun syncLedger(userId: String, db: FirebaseFirestore) {
        val ledgerDao = database.ledgerDao()

        // Upload
        val unsynced = ledgerDao.getUnsyncedLedger().filter { it.userId == userId }
        for (item in unsynced) {
            val docRef = db.collection("users").document(userId)
                .collection("ledger_records").document(item.id)
            if (item.isDeleted) {
                docRef.delete().await()
            } else {
                docRef.set(item).await()
            }
            ledgerDao.markSynced(item.id)
        }

        // Download
        val snapshot = db.collection("users").document(userId)
            .collection("ledger_records").get().await()

        val cloudItems = snapshot.toObjects(LedgerRecord::class.java)
        val localItems = ledgerDao.getLedgerList(userId)
        val localMap = localItems.associateBy { it.id }

        val toInsert = mutableListOf<LedgerRecord>()
        for (cloud in cloudItems) {
            if (cloud.isDeleted) continue
            val local = localMap[cloud.id]
            if (local == null || cloud.updatedAt > local.updatedAt) {
                toInsert.add(cloud.copy(isSynced = true))
            }
        }
        if (toInsert.isNotEmpty()) {
            ledgerDao.insertLedgers(toInsert)
        }
    }

    private suspend fun syncSizingTemplates(userId: String, db: FirebaseFirestore) {
        val templateDao = database.sizingTemplateDao()

        // Upload
        val unsynced = templateDao.getUnsyncedTemplates().filter { it.userId == userId }
        for (item in unsynced) {
            val docRef = db.collection("users").document(userId)
                .collection("sizing_templates").document(item.id)
            if (item.isDeleted) {
                docRef.delete().await()
            } else {
                docRef.set(item).await()
            }
            templateDao.markSynced(item.id)
        }

        // Download
        val snapshot = db.collection("users").document(userId)
            .collection("sizing_templates").get().await()

        val cloudItems = snapshot.toObjects(SizingTemplate::class.java)
        val localItems = templateDao.getTemplatesList(userId)
        val localMap = localItems.associateBy { it.id }

        val toInsert = mutableListOf<SizingTemplate>()
        for (cloud in cloudItems) {
            if (cloud.isDeleted) continue
            val local = localMap[cloud.id]
            if (local == null || cloud.updatedAt > local.updatedAt) {
                toInsert.add(cloud.copy(isSynced = true))
            }
        }
        if (toInsert.isNotEmpty()) {
            templateDao.insertTemplates(toInsert)
        }
    }

    private suspend fun syncCustomerMeasurements(userId: String, db: FirebaseFirestore) {
        val measurementDao = database.customerMeasurementDao()

        // Upload
        val unsynced = measurementDao.getUnsyncedMeasurements().filter { it.userId == userId }
        for (item in unsynced) {
            val docRef = db.collection("users").document(userId)
                .collection("customer_measurements").document(item.id)
            if (item.isDeleted) {
                docRef.delete().await()
            } else {
                docRef.set(item).await()
            }
            measurementDao.markSynced(item.id)
        }

        // Download
        val snapshot = db.collection("users").document(userId)
            .collection("customer_measurements").get().await()

        val cloudItems = snapshot.toObjects(CustomerMeasurement::class.java)
        val localItems = measurementDao.getMeasurementsList(userId)
        val localMap = localItems.associateBy { it.id }

        val toInsert = mutableListOf<CustomerMeasurement>()
        for (cloud in cloudItems) {
            if (cloud.isDeleted) continue
            val local = localMap[cloud.id]
            if (local == null || cloud.updatedAt > local.updatedAt) {
                toInsert.add(cloud.copy(isSynced = true))
            }
        }
        if (toInsert.isNotEmpty()) {
            measurementDao.insertMeasurements(toInsert)
        }
    }

    private suspend fun syncPaymentRecords(userId: String, db: FirebaseFirestore) {
        val paymentDao = database.paymentRecordDao()

        // Upload
        val unsynced = paymentDao.getUnsyncedPaymentRecords().filter { it.userId == userId }
        for (item in unsynced) {
            val docRef = db.collection("users").document(userId)
                .collection("payment_records").document(item.id)
            if (item.isDeleted) {
                docRef.delete().await()
            } else {
                docRef.set(item).await()
            }
            paymentDao.markSynced(item.id)
        }

        // Download
        val snapshot = db.collection("users").document(userId)
            .collection("payment_records").get().await()

        val cloudItems = snapshot.toObjects(PaymentRecord::class.java)
        val localItems = paymentDao.getPaymentRecordsList(userId)
        val localMap = localItems.associateBy { it.id }

        val toInsert = mutableListOf<PaymentRecord>()
        for (cloud in cloudItems) {
            if (cloud.isDeleted) continue
            val local = localMap[cloud.id]
            if (local == null || cloud.updatedAt > local.updatedAt) {
                toInsert.add(cloud.copy(isSynced = true))
            }
        }
        if (toInsert.isNotEmpty()) {
            paymentDao.insertPaymentRecords(toInsert)
        }
    }

    private suspend fun syncOrderRecords(userId: String, db: FirebaseFirestore) {
        val orderDao = database.orderRecordDao()

        // Upload
        val unsynced = orderDao.getUnsyncedOrderRecords().filter { it.userId == userId }
        for (item in unsynced) {
            val docRef = db.collection("users").document(userId)
                .collection("order_records").document(item.id)
            if (item.isDeleted) {
                docRef.delete().await()
            } else {
                docRef.set(item).await()
            }
            orderDao.markSynced(item.id)
        }

        // Download
        val snapshot = db.collection("users").document(userId)
            .collection("order_records").get().await()

        val cloudItems = snapshot.toObjects(OrderRecord::class.java)
        val localItems = orderDao.getOrderRecordsList(userId)
        val localMap = localItems.associateBy { it.id }

        val toInsert = mutableListOf<OrderRecord>()
        for (cloud in cloudItems) {
            if (cloud.isDeleted) continue
            val local = localMap[cloud.id]
            if (local == null || cloud.updatedAt > local.updatedAt) {
                toInsert.add(cloud.copy(isSynced = true))
            }
        }
        if (toInsert.isNotEmpty()) {
            orderDao.insertOrderRecords(toInsert)
        }
    }
}
