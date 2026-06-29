package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.auth.AuthManager
import com.example.auth.UserSession
import com.example.data.local.AppDatabase
import com.example.data.model.*
import com.example.data.sync.SyncManager
import com.example.util.CsvHelper
import com.example.util.JsonBackupHelper
import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await
import java.util.UUID

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
data class SubUser(
    val emailOrPhone: String = "",
    val name: String = "",
    val passcode: String = "",
    val ownerId: String = "",
    val createdAt: Long = 0L
)

data class DeviceContact(
    val name: String = "",
    val phone: String = "",
    val isAppUser: Boolean = false,
    val appUserId: String? = null
)

data class SharedCustomerPayload(
    val customer: com.example.data.model.Customer = com.example.data.model.Customer(),
    val measurements: List<com.example.data.model.CustomerMeasurement> = emptyList(),
    val values: List<com.example.data.model.MeasurementValue> = emptyList()
)

data class ChatMessage(
    val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val receiverId: String = "",
    val content: String = "",
    val timestamp: Long = 0L,
    val type: String = "text", // "text" or "measurements"
    val customerNames: List<String> = emptyList(),
    val sharedCustomers: List<SharedCustomerPayload> = emptyList(),
    val shareStatus: String = "pending" // "pending", "accepted", "rejected"
)

data class AppNotification(
    val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val title: String = "",
    val message: String = "",
    val timestamp: Long = 0L,
    val isRead: Boolean = false
)

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class TailorViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val authManager = AuthManager(application)
    private val syncManager = SyncManager(application, db)
    private val prefs = application.getSharedPreferences("tailor_shop_prefs", Context.MODE_PRIVATE)

    // Auth flows
    val currentUser: StateFlow<UserSession?> = authManager.currentUser
    
    // Active userId flow - maps to ownerId if sub user to pull correct datasets
    val activeUserId: Flow<String> = currentUser.map { user ->
        user?.let { if (it.isSubUser) it.ownerId else it.userId } ?: "guest_user"
    }

    // App Lock Preferences
    val isAppLockEnabled = MutableStateFlow(prefs.getBoolean("is_app_lock_enabled", false))
    val appLockPin = MutableStateFlow(prefs.getString("app_lock_pin", "") ?: "")
    val isBiometricEnabled = MutableStateFlow(prefs.getBoolean("is_biometric_enabled", false))
    val selectedCurrency = MutableStateFlow(prefs.getString("selected_currency", "$") ?: "$")
    val blockedUserIds = MutableStateFlow<Set<String>>(emptySet())
    val usersWhoBlockedMe = MutableStateFlow<Set<String>>(emptySet())
    val selectedCustomerForDetails = MutableStateFlow<Customer?>(null)
    val activeEditingTemplateId = MutableStateFlow<String?>(null)
    val defaultTemplateIdFlow = MutableStateFlow(prefs.getString("dynamic_default_template_id", null))

    fun getDefaultTemplateId(): String? {
        return defaultTemplateIdFlow.value
    }

    fun setDefaultTemplateId(id: String?) {
        prefs.edit().putString("dynamic_default_template_id", id).apply()
        defaultTemplateIdFlow.value = id
    }

    fun updateSelectedCurrency(currency: String) {
        prefs.edit().putString("selected_currency", currency).apply()
        selectedCurrency.value = currency
    }

    fun setAppLock(enabled: Boolean, pin: String, biometric: Boolean) {
        prefs.edit().apply {
            putBoolean("is_app_lock_enabled", enabled)
            putString("app_lock_pin", pin)
            putBoolean("is_biometric_enabled", biometric)
            apply()
        }
        isAppLockEnabled.value = enabled
        appLockPin.value = pin
        isBiometricEnabled.value = biometric
    }

    // Sub-users Management Flow
    val registeredSubUsers = MutableStateFlow<List<SubUser>>(emptyList())

    fun fetchSubUsers() {
        val user = currentUser.value
        if (user != null && !user.isSubUser) {
            try {
                val dbRef = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                dbRef.collection("users").document(user.userId).collection("sub_users")
                    .get()
                    .addOnSuccessListener { snapshot ->
                        val list = snapshot.documents.mapNotNull { doc ->
                            doc.toObject(SubUser::class.java)
                        }
                        registeredSubUsers.value = list
                    }
                    .addOnFailureListener {
                        Log.e("TailorViewModel", "Error fetching sub-users", it)
                    }
            } catch (e: Exception) {
                Log.e("TailorViewModel", "FirebaseFirestore reference failed in fetchSubUsers", e)
            }
        }
    }

    fun registerSubUser(name: String, emailOrPhone: String, passcode: String, onComplete: (Boolean, String?) -> Unit) {
        val user = currentUser.value
        if (user == null || user.isSubUser) {
            onComplete(false, "Only registered primary shop owners can create sub-users")
            return
        }

        val cleanedEmailOrPhone = emailOrPhone.trim().lowercase()
        val subUser = SubUser(
            emailOrPhone = cleanedEmailOrPhone,
            name = name,
            passcode = passcode,
            ownerId = user.userId,
            createdAt = System.currentTimeMillis()
        )

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val dbRef = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                // Save to owner's sub_users collection in Firestore
                dbRef.collection("users").document(user.userId).collection("sub_users")
                    .document(cleanedEmailOrPhone)
                    .set(subUser).await()

                // Save to global sub-users lookup registrar
                val globalRef = dbRef.collection("global_sub_users").document(cleanedEmailOrPhone)
                val globalData = mapOf(
                    "subUserId" to cleanedEmailOrPhone,
                    "name" to name,
                    "passcode" to passcode,
                    "ownerId" to user.userId,
                    "ownerEmail" to user.email,
                    "createdAt" to System.currentTimeMillis()
                )
                globalRef.set(globalData).await()

                withContext(Dispatchers.Main) {
                    fetchSubUsers()
                    onComplete(true, null)
                }
            } catch (e: java.lang.Exception) {
                withContext(Dispatchers.Main) {
                    onComplete(false, e.localizedMessage ?: "Firebase Database is unavailable. Please verify connection.")
                }
            }
        }
    }

    fun deleteSubUser(emailOrPhone: String, onComplete: (Boolean, String?) -> Unit) {
        val user = currentUser.value
        if (user == null || user.isSubUser) {
            onComplete(false, "Only registered primary shop owners can delete sub-users")
            return
        }

        val cleanedEmailOrPhone = emailOrPhone.trim().lowercase()
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val dbRef = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                // Delete from owner's sub_users collection in Firestore
                dbRef.collection("users").document(user.userId).collection("sub_users")
                    .document(cleanedEmailOrPhone)
                    .delete().await()

                // Delete from global sub-users lookup registrar
                com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("global_sub_users").document(cleanedEmailOrPhone)
                    .delete().await()

                withContext(Dispatchers.Main) {
                    fetchSubUsers()
                    onComplete(true, null)
                }
            } catch (e: java.lang.Exception) {
                withContext(Dispatchers.Main) {
                    onComplete(false, e.localizedMessage ?: "Failed to remove staff member helper account.")
                }
            }
        }
    }

    fun signInAsSubUser(emailOrPhone: String, passcode: String, onComplete: (Boolean, String?) -> Unit) {
        val cleanedEmailOrPhone = emailOrPhone.trim().lowercase()
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Ensure helper has an active anonymous session prior to scanning global registries to avoid authorization blocks
                val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
                if (auth.currentUser == null) {
                    try {
                        auth.signInAnonymously().await()
                    } catch (authEx: Exception) {
                        Log.w("TailorViewModel", "Anonymous authentication skipped or restricted by rules, proceeding with direct document fetch", authEx)
                    }
                }

                val dbRef = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                val doc = dbRef.collection("global_sub_users").document(cleanedEmailOrPhone).get().await()
                if (doc.exists()) {
                    val dbPasscode = doc.getString("passcode") ?: ""
                    if (dbPasscode == passcode) {
                        val name = doc.getString("name") ?: "Sub-user"
                        val ownerId = doc.getString("ownerId") ?: ""
                        
                        val session = UserSession(
                            userId = cleanedEmailOrPhone,
                            displayName = name,
                            email = cleanedEmailOrPhone,
                            isSubUser = true,
                            ownerId = ownerId
                        )
                        withContext(Dispatchers.Main) {
                            authManager.saveSubUserSession(session)
                            // Automatically sync all data of the linked owner immediately upon successful authentication
                            viewModelScope.launch {
                                syncManager.triggerSync(ownerId)
                            }
                            onComplete(true, null)
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            onComplete(false, "Invalid passcode/PIN code entered")
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        onComplete(false, "Sub-user with this mobile or email is not registered")
                    }
                }
            } catch (e: java.lang.Exception) {
                withContext(Dispatchers.Main) {
                    onComplete(false, "Connection error: ${e.localizedMessage ?: "Firebase system missing or offline."}")
                }
            }
        }
    }

    private fun getWriteUserId(): String {
        val user = currentUser.value
        return user?.let { if (it.isSubUser) it.ownerId else it.userId } ?: "guest_user"
    }

    // Shop Settings setup in profile
    val shopName = MutableStateFlow(prefs.getString("shop_name", "Tailor Shop Master") ?: "Tailor Shop Master")
    val shopPhone = MutableStateFlow(prefs.getString("shop_phone", "+1 (555) 019-9922") ?: "+1 (555) 019-9922")
    val shopAddress = MutableStateFlow(prefs.getString("shop_address", "City Tailoring Blvd, Suite 100") ?: "City Tailoring Blvd, Suite 100")

    fun updateShopProfile(name: String, phone: String, address: String) {
        if (currentUser.value?.isSubUser == true) return // sub-users cannot edit profile branding
        prefs.edit().apply {
            putString("shop_name", name)
            putString("shop_phone", phone)
            putString("shop_address", address)
            apply()
        }
        shopName.value = name
        shopPhone.value = phone
        shopAddress.value = address

        // Save to global_owners_directory collection in Firestore so friends can match them
        val userId = currentUser.value?.userId
        if (userId != null && userId != "guest_user") {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    val data = hashMapOf(
                        "userId" to userId,
                        "phone" to phone,
                        "shopName" to name,
                        "address" to address,
                        "updatedAt" to com.google.firebase.Timestamp.now()
                    )
                    firestore.collection("global_owners_directory").document(userId).set(data).await()
                } catch (e: Exception) {
                    Log.e("TailorViewModel", "Failed to register in global_owners_directory", e)
                }
            }
        }
    }

    // Core flows mapped dynamically to active userId
    val customers: StateFlow<List<Customer>> = activeUserId.flatMapLatest { userId ->
        db.customerDao().getCustomersFlow(userId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val templates: StateFlow<List<SizingTemplate>> = activeUserId.flatMapLatest { userId ->
        db.sizingTemplateDao().getTemplatesFlow(userId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val fields: StateFlow<List<MeasurementField>> = activeUserId.flatMapLatest { userId ->
        db.measurementFieldDao().getFieldsFlow(userId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customerMeasurements: StateFlow<List<CustomerMeasurement>> = activeUserId.flatMapLatest { userId ->
        db.customerMeasurementDao().getMeasurementsFlow(userId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val measurementValues: StateFlow<List<MeasurementValue>> = activeUserId.flatMapLatest { userId ->
        db.measurementValueDao().getValuesFlow(userId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val inventory: StateFlow<List<InventoryItem>> = activeUserId.flatMapLatest { userId ->
        db.inventoryDao().getInventoryFlow(userId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val ledgerRecords: StateFlow<List<LedgerRecord>> = activeUserId.flatMapLatest { userId ->
        db.ledgerDao().getLedgerFlow(userId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val paymentRecords: StateFlow<List<PaymentRecord>> = activeUserId.flatMapLatest { userId ->
        db.paymentRecordDao().getPaymentRecordsFlow(userId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val orderRecords: StateFlow<List<OrderRecord>> = activeUserId.flatMapLatest { userId ->
        db.orderRecordDao().getOrderRecordsFlow(userId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val employees: StateFlow<List<Employee>> = db.employeeDao().getAllEmployeesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val employeeWorkRecords: StateFlow<List<EmployeeWorkRecord>> = db.employeeWorkRecordDao().getAllWorkRecordsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val employeePaymentRecords: StateFlow<List<EmployeePaymentRecord>> = db.employeePaymentRecordDao().getAllPaymentRecordsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Sync state
    val syncStatus: StateFlow<String> = syncManager.syncStatus

    init {
        // Automatically sync on initial launch/authenticated if online
        viewModelScope.launch {
            currentUser.collect { user ->
                val userId = user?.let { if (it.isSubUser) it.ownerId else it.userId } ?: "guest_user"
                // Create default template and sizing fields if fresh database
                ensureDefaultSizingFields(userId)
                if (user != null) {
                    startListeningToNotifications()
                    startListeningToBlocks()
                    if (!user.isSubUser) {
                        migrateGuestData(user.userId)
                    }
                    syncManager.triggerSync(userId)
                } else {
                    stopListeningToNotifications()
                    stopListeningToBlocks()
                }
            }
        }
    }

    private suspend fun migrateGuestData(authenticatedUserId: String) = withContext(Dispatchers.IO) {
        Log.d("TailorViewModel", "Migrating guest_user data to $authenticatedUserId")
        try {
            // 1. Customers
            val guestCustomers = db.customerDao().getCustomersList("guest_user")
            if (guestCustomers.isNotEmpty()) {
                val migrated = guestCustomers.map { it.copy(userId = authenticatedUserId, isSynced = false, updatedAt = System.currentTimeMillis()) }
                db.customerDao().insertCustomers(migrated)
            }
            
            // 2. Sizing templates
            val guestTemplates = db.sizingTemplateDao().getTemplatesList("guest_user")
            if (guestTemplates.isNotEmpty()) {
                val migrated = guestTemplates.map { it.copy(userId = authenticatedUserId, isSynced = false, updatedAt = System.currentTimeMillis()) }
                db.sizingTemplateDao().insertTemplates(migrated)
            }
            
            // 3. Measurement fields
            val guestFields = db.measurementFieldDao().getFieldsList("guest_user")
            if (guestFields.isNotEmpty()) {
                val migrated = guestFields.map { it.copy(userId = authenticatedUserId, isSynced = false, updatedAt = System.currentTimeMillis()) }
                db.measurementFieldDao().insertFields(migrated)
            }
            
            // 4. Customer measurements
            val guestMeasurements = db.customerMeasurementDao().getMeasurementsList("guest_user")
            if (guestMeasurements.isNotEmpty()) {
                val migrated = guestMeasurements.map { it.copy(userId = authenticatedUserId, isSynced = false, updatedAt = System.currentTimeMillis()) }
                db.customerMeasurementDao().insertMeasurements(migrated)
            }
            
            // 5. Measurement values
            val guestValues = db.measurementValueDao().getValuesList("guest_user")
            if (guestValues.isNotEmpty()) {
                val migrated = guestValues.map { it.copy(userId = authenticatedUserId, isSynced = false, updatedAt = System.currentTimeMillis()) }
                db.measurementValueDao().insertValues(migrated)
            }
            
            // 6. Inventory items
            val guestInventory = db.inventoryDao().getInventoryList("guest_user")
            if (guestInventory.isNotEmpty()) {
                val migrated = guestInventory.map { it.copy(userId = authenticatedUserId, isSynced = false, updatedAt = System.currentTimeMillis()) }
                db.inventoryDao().insertItems(migrated)
            }
            
            // 7. Ledger records
            val guestLedgers = db.ledgerDao().getLedgerList("guest_user")
            if (guestLedgers.isNotEmpty()) {
                val migrated = guestLedgers.map { it.copy(userId = authenticatedUserId, isSynced = false, updatedAt = System.currentTimeMillis()) }
                db.ledgerDao().insertLedgers(migrated)
            }

            // 8. Payment records
            val guestPayments = db.paymentRecordDao().getPaymentRecordsList("guest_user")
            if (guestPayments.isNotEmpty()) {
                val migrated = guestPayments.map { it.copy(userId = authenticatedUserId, isSynced = false, updatedAt = System.currentTimeMillis()) }
                db.paymentRecordDao().insertPaymentRecords(migrated)
            }

            // 9. Order records
            val guestOrders = db.orderRecordDao().getOrderRecordsList("guest_user")
            if (guestOrders.isNotEmpty()) {
                val migrated = guestOrders.map { it.copy(userId = authenticatedUserId, isSynced = false, updatedAt = System.currentTimeMillis()) }
                db.orderRecordDao().insertOrderRecords(migrated)
            }
            Log.d("TailorViewModel", "Guest data migration complete")
        } catch (e: Exception) {
            Log.e("TailorViewModel", "Error migrating guest data", e)
        }
    }

    private suspend fun ensureDefaultSizingFields(userId: String) = withContext(Dispatchers.IO) {
        // Remove the previously created developer default template "default_template" and its fields to keep things clean
        db.sizingTemplateDao().softDeleteTemplate("default_template")
        val relatedFields = db.measurementFieldDao().getFieldsList(userId).filter { it.templateId == "default_template" }
        relatedFields.forEach {
            db.measurementFieldDao().softDeleteField(it.id)
        }

        // Install "Measurements/شلوار-قمیص" as a default free premium template if templates list is empty
        val existingTemplates = db.sizingTemplateDao().getTemplatesList(userId).filter { !it.isDeleted }
        if (existingTemplates.isEmpty()) {
            val templateId = UUID.randomUUID().toString()
            val defaultTemplate = SizingTemplate(
                id = templateId,
                userId = userId,
                name = "Measurements/شلوار-قمیص",
                updatedAt = System.currentTimeMillis()
            )
            db.sizingTemplateDao().insertTemplate(defaultTemplate)

            // Seed standard measurement fields
            val mLabels = listOf(
                "Length/لمبائی",
                "Sleeve/آستین",
                "Shoulder/تیرا",
                "Neck/گلا",
                "Chest/چھاتی",
                "Waist/کمر",
                "Hip/گھیرا",
                "Length/شلوار",
                "Hem/پانچہ"
            )
            mLabels.forEachIndexed { idx, label ->
                val field = MeasurementField(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    templateId = templateId,
                    label = label,
                    type = "NUMBER",
                    displayOrder = idx,
                    isDemand = false,
                    updatedAt = System.currentTimeMillis()
                )
                db.measurementFieldDao().insertField(field)
            }

            // Seed design style demands
            val dFieldsData = listOf(
                Triple("Daaman/دامن", "RADIO", "گول,چورس"),
                Triple("Neck/گلا", "RADIO", "کالر,بین,گول بین,ہاف بین,ہاف کالر"),
                Triple("Cuff/کف", "RADIO", "چورس,گول,کٹ"),
                Triple("Front Pocket/سامنے جیب", "RADIO", "نہیں,سنگل"),
                Triple("Side Pocket/سائیڈ جیب", "RADIO", "ایک,دو,نہیں"),
                Triple("Pant Pocket/شلوار پاکٹ", "RADIO", "نہیں,ایک,دو")
            )
            dFieldsData.forEachIndexed { idx, (label, type, options) ->
                val field = MeasurementField(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    templateId = templateId,
                    label = label,
                    type = type,
                    options = options,
                    displayOrder = mLabels.size + idx,
                    isDemand = true,
                    updatedAt = System.currentTimeMillis()
                )
                db.measurementFieldDao().insertField(field)
            }

            // Set this as the active default template
            prefs.edit().putString("dynamic_default_template_id", templateId).apply()
            defaultTemplateIdFlow.value = templateId
        }
    }

    // Google Auth interface
    fun signInGoogle(idToken: String, onComplete: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            authManager.signInWithGoogle(idToken, onComplete)
        }
    }

    fun signOut() {
        authManager.signOut()
    }

    fun deleteAccount(onComplete: (Boolean, String?) -> Unit) {
        val user = currentUser.value
        if (user == null) {
            onComplete(false, "No active login session found.")
            return
        }
        val userId = user.userId
        viewModelScope.launch {
            try {
                if (!user.isSubUser) {
                    val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    firestore.collection("global_owners_directory").document(userId).delete()
                    firestore.collection("users").document(userId).delete()
                }
            } catch (e: Exception) {
                Log.e("TailorViewModel", "Best effort Firestore delete failed during account deletion", e)
            }

            authManager.deleteAccount { success, error ->
                if (success) {
                    prefs.edit().clear().apply()
                    viewModelScope.launch(Dispatchers.IO) {
                        try {
                            db.clearAllTables()
                        } catch (e: Exception) {
                            Log.e("TailorViewModel", "Failed to clear Room database", e)
                        }
                    }
                    onComplete(true, null)
                } else {
                    onComplete(false, error)
                }
            }
        }
    }

    fun triggerSyncNow() {
        val user = currentUser.value ?: return
        val syncUserId = if (user.isSubUser) user.ownerId else user.userId
        viewModelScope.launch {
            syncManager.triggerSync(syncUserId)
        }
    }

    // --- Database Operations ---

    fun insertCustomer(name: String, phone: String, address: String) {
        val userId = getWriteUserId()
        viewModelScope.launch(Dispatchers.IO) {
            val cId = UUID.randomUUID().toString()
            val newCustomer = Customer(
                id = cId,
                userId = userId,
                name = name,
                phone = phone,
                address = address
            )
            db.customerDao().insertCustomer(newCustomer)

            // Create placeholder empty ledger record for the customer
            val newLedger = LedgerRecord(
                id = UUID.randomUUID().toString(),
                userId = userId,
                customerId = cId,
                totalValue = 0.0,
                amountPaid = 0.0,
                pendingDebt = 0.0
            )
            db.ledgerDao().insertLedger(newLedger)

            // Seed a default measurement card for the customer under the standard template (if any template exists)
            val userTemplates = db.sizingTemplateDao().getTemplatesList(userId)
            val defaultTid = prefs.getString("dynamic_default_template_id", null) ?: userTemplates.firstOrNull()?.id
            if (defaultTid != null) {
                val defaultCMId = UUID.randomUUID().toString()
                db.customerMeasurementDao().insertMeasurement(
                    CustomerMeasurement(
                        id = defaultCMId,
                        userId = userId,
                        customerId = cId,
                        templateId = defaultTid,
                        title = "Primary Measurements"
                    )
                )
            }

            if (userId != "guest_user") {
                syncManager.triggerSync(userId)
            }
        }
    }

    fun updateCustomer(customer: Customer) {
        if (currentUser.value?.isSubUser == true) return // sub-users cannot edit existing customer profiles
        val userId = getWriteUserId()
        viewModelScope.launch(Dispatchers.IO) {
            db.customerDao().insertCustomer(customer.copy(updatedAt = System.currentTimeMillis(), isSynced = false))
            if (userId != "guest_user") {
                syncManager.triggerSync(userId)
            }
        }
    }

    fun softDeleteCustomer(customer: Customer) {
        if (currentUser.value?.isSubUser == true) return // sub-users cannot delete customers
        val userId = getWriteUserId()
        viewModelScope.launch(Dispatchers.IO) {
            db.customerDao().softDeleteCustomer(customer.id)
            if (userId != "guest_user") {
                syncManager.triggerSync(userId)
            }
        }
    }

    // --- Sizing Templates Operations ---

    fun insertTemplate(name: String) {
        val userId = getWriteUserId()
        viewModelScope.launch(Dispatchers.IO) {
            val templateId = UUID.randomUUID().toString()
            val newTemplate = SizingTemplate(
                id = templateId,
                userId = userId,
                name = name
            )
            db.sizingTemplateDao().insertTemplate(newTemplate)
            if (userId != "guest_user") {
                syncManager.triggerSync(userId)
            }
        }
    }

    fun updateTemplateName(templateId: String, name: String) {
        if (currentUser.value?.isSubUser == true) return // sub-users cannot edit template names
        val userId = getWriteUserId()
        viewModelScope.launch(Dispatchers.IO) {
            val existing = db.sizingTemplateDao().getTemplatesList(userId).find { it.id == templateId }
            if (existing != null) {
                db.sizingTemplateDao().insertTemplate(existing.copy(name = name, updatedAt = System.currentTimeMillis(), isSynced = false))
                if (userId != "guest_user") {
                    syncManager.triggerSync(userId)
                }
            }
        }
    }

    fun deleteTemplate(templateId: String) {
        if (currentUser.value?.isSubUser == true) return // sub-users cannot delete templates
        val userId = getWriteUserId()
        viewModelScope.launch(Dispatchers.IO) {
            db.sizingTemplateDao().softDeleteTemplate(templateId)
            // also soft-delete sizing fields that belonged to it
            val relatedFields = db.measurementFieldDao().getFieldsList(userId).filter { it.templateId == templateId }
            relatedFields.forEach {
                db.measurementFieldDao().softDeleteField(it.id)
            }
            if (userId != "guest_user") {
                syncManager.triggerSync(userId)
            }
        }
    }

    // --- Measurement Template Field Operations ---

    fun insertField(
        templateId: String,
        label: String,
        type: String,
        options: String,
        isDemand: Boolean = false,
        hasSubInput: Boolean = false,
        subInputType: String = "",
        subInputLabel: String = "",
        subInputOptions: String = "",
        subInputTargetOption: String = "",
        hasNoteInput: Boolean = false
    ) {
        val userId = getWriteUserId()
        viewModelScope.launch(Dispatchers.IO) {
            val count = db.measurementFieldDao().getFieldsList(userId).filter { it.templateId == templateId }.size
            val newField = MeasurementField(
                id = UUID.randomUUID().toString(),
                userId = userId,
                templateId = templateId,
                label = label,
                type = type,
                options = options,
                displayOrder = count,
                isDemand = isDemand,
                hasSubInput = hasSubInput,
                subInputType = subInputType,
                subInputLabel = subInputLabel,
                subInputOptions = subInputOptions,
                subInputTargetOption = subInputTargetOption,
                hasNoteInput = hasNoteInput
            )
            db.measurementFieldDao().insertField(newField)
            if (userId != "guest_user") {
                syncManager.triggerSync(userId)
            }
        }
    }

    fun deleteField(fieldId: String) {
        if (currentUser.value?.isSubUser == true) return // sub-users cannot delete fields
        val userId = getWriteUserId()
        viewModelScope.launch(Dispatchers.IO) {
            db.measurementFieldDao().softDeleteField(fieldId)
            if (userId != "guest_user") {
                syncManager.triggerSync(userId)
            }
        }
    }

    fun updateField(
        fieldId: String,
        label: String,
        type: String,
        options: String,
        isDemand: Boolean,
        hasSubInput: Boolean,
        subInputType: String,
        subInputLabel: String,
        subInputOptions: String,
        subInputTargetOption: String,
        hasNoteInput: Boolean
    ) {
        if (currentUser.value?.isSubUser == true) return // sub-users cannot edit fields
        val userId = getWriteUserId()
        viewModelScope.launch(Dispatchers.IO) {
            val existing = db.measurementFieldDao().getFieldsList(userId).find { it.id == fieldId }
            if (existing != null) {
                val updated = existing.copy(
                    label = label,
                    type = type,
                    options = options,
                    isDemand = isDemand,
                    hasSubInput = hasSubInput,
                    subInputType = subInputType,
                    subInputLabel = subInputLabel,
                    subInputOptions = subInputOptions,
                    subInputTargetOption = subInputTargetOption,
                    hasNoteInput = hasNoteInput,
                    updatedAt = System.currentTimeMillis(),
                    isSynced = false
                )
                db.measurementFieldDao().insertField(updated)
                if (userId != "guest_user") {
                    syncManager.triggerSync(userId)
                }
            }
        }
    }

    // --- Customer Measurement Record Operations ---

    fun insertCustomerMeasurement(customerId: String, templateId: String, title: String) {
        val userId = getWriteUserId()
        viewModelScope.launch(Dispatchers.IO) {
            val cmId = UUID.randomUUID().toString()
            val newCM = CustomerMeasurement(
                id = cmId,
                userId = userId,
                customerId = customerId,
                templateId = templateId,
                title = title
            )
            db.customerMeasurementDao().insertMeasurement(newCM)
            if (userId != "guest_user") {
                syncManager.triggerSync(userId)
            }
        }
    }

    fun insertCustomerMeasurementWithValues(
        customerId: String,
        templateId: String,
        title: String,
        valuesMap: Map<String, Pair<String, String>>,
        notesMap: Map<String, String> = emptyMap()
    ) {
        val userId = getWriteUserId()
        viewModelScope.launch(Dispatchers.IO) {
            val cmId = UUID.randomUUID().toString()
            val newCM = CustomerMeasurement(
                id = cmId,
                userId = userId,
                customerId = customerId,
                templateId = templateId,
                title = title
            )
            db.customerMeasurementDao().insertMeasurement(newCM)
            valuesMap.forEach { (fieldId, pair) ->
                val (valString, subValString) = pair
                val noteStr = notesMap[fieldId] ?: ""
                if (valString.isNotBlank() || subValString.isNotBlank() || noteStr.isNotBlank()) {
                    val row = MeasurementValue(
                        id = UUID.randomUUID().toString(),
                        userId = userId,
                        customerId = customerId,
                        measurementId = cmId,
                        fieldId = fieldId,
                        value = valString,
                        subValue = subValString,
                        note = noteStr
                    )
                    db.measurementValueDao().insertValue(row)
                }
            }
            if (userId != "guest_user") {
                syncManager.triggerSync(userId)
            }
        }
    }

    fun deleteCustomerMeasurement(measurementId: String) {
        if (currentUser.value?.isSubUser == true) return // sub-users cannot delete measurement cards
        val userId = getWriteUserId()
        viewModelScope.launch(Dispatchers.IO) {
            db.customerMeasurementDao().softDeleteMeasurement(measurementId)
            if (userId != "guest_user") {
                syncManager.triggerSync(userId)
            }
        }
    }

    // --- Customer Measurement Inputs ---

    fun saveMeasurementValue(customerId: String, measurementId: String, fieldId: String, valString: String, subValString: String = "", note: String = "") {
        val userId = getWriteUserId()
        viewModelScope.launch(Dispatchers.IO) {
            // Check if record exists already to update or insert new
            val existing = db.measurementValueDao().getValuesList(userId)
                .find { it.measurementId == measurementId && it.fieldId == fieldId }

            val row = if (existing != null) {
                existing.copy(value = valString, subValue = subValString, note = note, updatedAt = System.currentTimeMillis(), isSynced = false)
            } else {
                MeasurementValue(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    customerId = customerId,
                    measurementId = measurementId,
                    fieldId = fieldId,
                    value = valString,
                    subValue = subValString,
                    note = note
                )
            }
            db.measurementValueDao().insertValue(row)
            if (userId != "guest_user") {
                syncManager.triggerSync(userId)
            }
        }
    }

    // --- Inventory Operations ---

    fun insertInventoryItem(name: String, unitType: String, purchasePrice: Double, sellingPrice: Double, stock: Int) {
        val userId = getWriteUserId()
        viewModelScope.launch(Dispatchers.IO) {
            val newItem = InventoryItem(
                id = UUID.randomUUID().toString(),
                userId = userId,
                name = name,
                unitType = unitType,
                purchasePrice = purchasePrice,
                sellingPrice = sellingPrice,
                stockQuantity = stock
            )
            db.inventoryDao().insertItem(newItem)
            if (userId != "guest_user") {
                syncManager.triggerSync(userId)
            }
        }
    }

    fun updateInventoryItem(item: InventoryItem) {
        if (currentUser.value?.isSubUser == true) return // sub-users cannot modify inventory records
        val userId = getWriteUserId()
        viewModelScope.launch(Dispatchers.IO) {
            db.inventoryDao().insertItem(item.copy(updatedAt = System.currentTimeMillis(), isSynced = false))
            if (userId != "guest_user") {
                syncManager.triggerSync(userId)
            }
        }
    }

    fun softDeleteInventoryItem(item: InventoryItem) {
        if (currentUser.value?.isSubUser == true) return // sub-users cannot delete inventory records
        val userId = getWriteUserId()
        viewModelScope.launch(Dispatchers.IO) {
            db.inventoryDao().softDeleteItem(item.id)
            if (userId != "guest_user") {
                syncManager.triggerSync(userId)
            }
        }
    }

    // --- Ledger / Financial Operations ---

    fun updateLedger(customerId: String, totalValue: Double, amountPaid: Double) {
        if (currentUser.value?.isSubUser == true) return // sub-users cannot adjust raw ledger directly
        val userId = getWriteUserId()
        viewModelScope.launch(Dispatchers.IO) {
            val existing = db.ledgerDao().getLedgerForCustomer(customerId, userId)
            val updated = if (existing != null) {
                existing.copy(
                    totalValue = totalValue,
                    amountPaid = amountPaid,
                    pendingDebt = totalValue - amountPaid,
                    updatedAt = System.currentTimeMillis(),
                    isSynced = false
                )
            } else {
                LedgerRecord(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    customerId = customerId,
                    totalValue = totalValue,
                    amountPaid = amountPaid,
                    pendingDebt = totalValue - amountPaid
                )
            }
            db.ledgerDao().insertLedger(updated)
            if (userId != "guest_user") {
                syncManager.triggerSync(userId)
            }
        }
    }

    fun createOrder(
        customerId: String,
        itemName: String,
        itemPrice: Double,
        advancePayment: Double,
        note: String,
        associatedInventoryItemId: String? = null,
        qty: Int = 1
    ) {
        val userId = getWriteUserId()
        viewModelScope.launch(Dispatchers.IO) {
            val existingLedger = db.ledgerDao().getLedgerForCustomer(customerId, userId)
            val ledgerId = existingLedger?.id ?: UUID.randomUUID().toString()

            // 1. Calculate and update Ledger Record
            val updatedLedger = if (existingLedger != null) {
                val newTotal = existingLedger.totalValue + itemPrice
                val newPaid = existingLedger.amountPaid + advancePayment
                existingLedger.copy(
                    totalValue = newTotal,
                    amountPaid = newPaid,
                    pendingDebt = newTotal - newPaid,
                    updatedAt = System.currentTimeMillis(),
                    isSynced = false
                )
            } else {
                LedgerRecord(
                    id = ledgerId,
                    userId = userId,
                    customerId = customerId,
                    totalValue = itemPrice,
                    amountPaid = advancePayment,
                    pendingDebt = itemPrice - advancePayment,
                    updatedAt = System.currentTimeMillis(),
                    isSynced = false
                )
            }
            db.ledgerDao().insertLedger(updatedLedger)

            // 2. Insert OrderRecord
            val orderId = UUID.randomUUID().toString()
            val orderRecord = OrderRecord(
                id = orderId,
                userId = userId,
                customerId = customerId,
                ledgerId = ledgerId,
                itemName = itemName,
                price = itemPrice,
                quantity = qty,
                orderDate = System.currentTimeMillis(),
                isCompleted = advancePayment >= itemPrice,
                updatedAt = System.currentTimeMillis(),
                isSynced = false,
                isDeleted = false
            )
            db.orderRecordDao().insertOrderRecord(orderRecord)

            // 3. Insert PaymentRecord if there's any payment made
            if (advancePayment > 0.0) {
                val paymentRecord = PaymentRecord(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    customerId = customerId,
                    ledgerId = ledgerId,
                    amountPaid = advancePayment,
                    paymentDate = System.currentTimeMillis(),
                    note = if (note.isNotEmpty()) note else "Advance payment for: $itemName",
                    updatedAt = System.currentTimeMillis(),
                    isSynced = false
                )
                db.paymentRecordDao().insertPaymentRecord(paymentRecord)
            }

            // 4. Update Inventory stock if an inventory item was used
            if (associatedInventoryItemId != null) {
                db.inventoryDao().getInventoryList(userId).find { it.id == associatedInventoryItemId }?.let { item ->
                    val newStock = (item.stockQuantity - qty).coerceAtLeast(0)
                    db.inventoryDao().insertItem(
                        item.copy(
                            stockQuantity = newStock,
                            updatedAt = System.currentTimeMillis(),
                            isSynced = false
                        )
                    )
                }
            }

            // 5. Trigger cloud sync
            if (userId != "guest_user") {
                syncManager.triggerSync(userId)
            }
        }
    }

    fun addPartialPayment(customerId: String, paymentAmount: Double, note: String) {
        val userId = getWriteUserId()
        viewModelScope.launch(Dispatchers.IO) {
            val existingLedger = db.ledgerDao().getLedgerForCustomer(customerId, userId)
            val ledgerId = existingLedger?.id ?: UUID.randomUUID().toString()

            val updatedLedger = if (existingLedger != null) {
                val newPaid = existingLedger.amountPaid + paymentAmount
                existingLedger.copy(
                    amountPaid = newPaid,
                    pendingDebt = existingLedger.totalValue - newPaid,
                    updatedAt = System.currentTimeMillis(),
                    isSynced = false
                )
            } else {
                LedgerRecord(
                    id = ledgerId,
                    userId = userId,
                    customerId = customerId,
                    totalValue = 0.0,
                    amountPaid = paymentAmount,
                    pendingDebt = -paymentAmount,
                    updatedAt = System.currentTimeMillis(),
                    isSynced = false
                )
            }
            db.ledgerDao().insertLedger(updatedLedger)

            val paymentRecord = PaymentRecord(
                id = UUID.randomUUID().toString(),
                userId = userId,
                customerId = customerId,
                ledgerId = ledgerId,
                amountPaid = paymentAmount,
                paymentDate = System.currentTimeMillis(),
                note = note,
                updatedAt = System.currentTimeMillis(),
                isSynced = false
            )
            db.paymentRecordDao().insertPaymentRecord(paymentRecord)

            if (userId != "guest_user") {
                syncManager.triggerSync(userId)
            }
        }
    }

    // Link an inventory sale directly to Customer's Ledger
    fun applyInventorySaleToLedger(customerId: String, item: InventoryItem, quantity: Int) {
        val userId = getWriteUserId()
        viewModelScope.launch(Dispatchers.IO) {
            if (item.stockQuantity >= quantity) {
                // Deduct stock quantity
                val updatedItem = item.copy(
                    stockQuantity = item.stockQuantity - quantity,
                    updatedAt = System.currentTimeMillis(),
                    isSynced = false
                )
                db.inventoryDao().insertItem(updatedItem)

                // Add to customer's ledger total bill
                val saleBillAmount = item.sellingPrice * quantity
                val existingLedger = db.ledgerDao().getLedgerForCustomer(customerId, userId)
                
                val updatedLedger = if (existingLedger != null) {
                    val newTotal = existingLedger.totalValue + saleBillAmount
                    existingLedger.copy(
                        totalValue = newTotal,
                        pendingDebt = newTotal - existingLedger.amountPaid,
                        updatedAt = System.currentTimeMillis(),
                        isSynced = false
                    )
                } else {
                    LedgerRecord(
                        id = UUID.randomUUID().toString(),
                        userId = userId,
                        customerId = customerId,
                        totalValue = saleBillAmount,
                        amountPaid = 0.0,
                        pendingDebt = saleBillAmount
                    )
                }
                db.ledgerDao().insertLedger(updatedLedger)
                if (userId != "guest_user") {
                    syncManager.triggerSync(userId)
                }
            }
        }
    }

    // --- UNIFIED JSON BACKUP & PORTABILITY ---

    fun exportAllJson(): String {
        return JsonBackupHelper.exportEverything(
            shopName = shopName.value,
            shopPhone = shopPhone.value,
            shopAddress = shopAddress.value,
            customers = customers.value,
            templates = templates.value,
            fields = fields.value,
            customerMeasurements = customerMeasurements.value,
            measurementValues = measurementValues.value,
            inventoryItems = inventory.value,
            ledgerRecords = ledgerRecords.value,
            paymentRecords = paymentRecords.value,
            orderRecords = orderRecords.value
        )
    }

    fun importAllJson(jsonStr: String): Boolean {
        val userId = currentUser.value?.userId ?: "guest_user"
        val payload = JsonBackupHelper.parseBackup(jsonStr, userId) ?: return false
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Update SharedPreferences
                updateShopProfile(payload.shopName, payload.shopPhone, payload.shopAddress)

                // Clear existing records for this user to reload fresh
                val existingTemps = db.sizingTemplateDao().getTemplatesList(userId)
                existingTemps.forEach { db.sizingTemplateDao().softDeleteTemplate(it.id) }

                val existingCusts = db.customerDao().getCustomersList(userId)
                existingCusts.forEach { db.customerDao().softDeleteCustomer(it.id) }

                val existingFields = db.measurementFieldDao().getFieldsList(userId)
                existingFields.forEach { db.measurementFieldDao().softDeleteField(it.id) }

                val existingMeas = db.customerMeasurementDao().getMeasurementsList(userId)
                existingMeas.forEach { db.customerMeasurementDao().softDeleteMeasurement(it.id) }

                val existingVals = db.measurementValueDao().getValuesList(userId)
                existingVals.forEach { db.measurementValueDao().softDeleteValue(it.id) }

                val existingInv = db.inventoryDao().getInventoryList(userId)
                existingInv.forEach { db.inventoryDao().softDeleteItem(it.id) }

                val existingLedger = db.ledgerDao().getLedgerList(userId)
                existingLedger.forEach { db.ledgerDao().softDeleteLedger(it.id) }

                val existingPayments = db.paymentRecordDao().getPaymentRecordsList(userId)
                existingPayments.forEach { db.paymentRecordDao().softDeletePaymentRecord(it.id) }

                val existingOrders = db.orderRecordDao().getOrderRecordsList(userId)
                existingOrders.forEach { db.orderRecordDao().softDeleteOrderRecord(it.id) }

                // Insert all imported entities
                db.sizingTemplateDao().insertTemplates(payload.templates)
                db.customerDao().insertCustomers(payload.customers)
                db.measurementFieldDao().insertFields(payload.fields)
                db.customerMeasurementDao().insertMeasurements(payload.customerMeasurements)
                db.measurementValueDao().insertValues(payload.measurementValues)
                db.inventoryDao().insertItems(payload.inventoryItems)
                db.ledgerDao().insertLedgers(payload.ledgerRecords)
                db.paymentRecordDao().insertPaymentRecords(payload.paymentRecords)
                db.orderRecordDao().insertOrderRecords(payload.orderRecords)

                if (userId != "guest_user") {
                    syncManager.triggerSync(userId)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return true
    }

    // --- Backward CSV Backup Compat (just in case) ---

    fun getExportCsv(dataType: String): String {
        return when (dataType) {
            "CUSTOMERS" -> CsvHelper.exportCustomersToCsv(customers.value)
            "INVENTORY" -> CsvHelper.exportInventoryToCsv(inventory.value)
            "LEDGER" -> CsvHelper.exportLedgerToCsv(ledgerRecords.value)
            else -> ""
        }
    }

    fun importCsvData(dataType: String, csvContent: String) {
        val userId = currentUser.value?.userId ?: "guest_user"
        viewModelScope.launch(Dispatchers.IO) {
            try {
                when (dataType) {
                    "CUSTOMERS" -> {
                        val parsed = CsvHelper.parseCustomersFromCsv(csvContent, userId)
                        db.customerDao().insertCustomers(parsed)
                    }
                    "INVENTORY" -> {
                        val parsed = CsvHelper.parseInventoryFromCsv(csvContent, userId)
                        db.inventoryDao().insertItems(parsed)
                    }
                    "LEDGER" -> {
                        val parsed = CsvHelper.parseLedgerFromCsv(csvContent, userId)
                        db.ledgerDao().insertLedgers(parsed)
                    }
                }
                if (userId != "guest_user") {
                    syncManager.triggerSync(userId)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val isUrduSelected = MutableStateFlow(false)

    fun setUrduLanguage(enabled: Boolean) {
        prefs.edit().putBoolean("is_urdu_selected", false).apply()
        isUrduSelected.value = false
    }

    // Urdu Phonetic Mapping (Roman Urdu -> Urdu Script)
    fun transliteratePhoneticToUrdu(input: String): String {
        if (input.isEmpty()) return ""
        val sb = StringBuilder()
        var i = 0
        while (i < input.length) {
            // Greedy match 2-character sequences first
            if (i + 1 < input.length) {
                val pair = input.substring(i, i + 2).lowercase()
                val match = when (pair) {
                    "kh" -> "خ"
                    "gh" -> "غ"
                    "sh" -> "ش"
                    "ch" -> "چ"
                    "th" -> "تھ"
                    "ph" -> "پھ"
                    "bh" -> "بھ"
                    "dh" -> "دھ"
                    "rh" -> "ڑھ"
                    "jh" -> "جھ"
                    "aa" -> "آ"
                    "ee" -> "ی"
                    "oo" -> "و"
                    else -> null
                }
                if (match != null) {
                    sb.append(match)
                    i += 2
                    continue
                }
            }

            // Greedy match single-character conversions
            val char = input[i]
            val lowerChar = char.lowercaseChar()
            val matchSingle = when (lowerChar) {
                'a' -> if (char.isUpperCase()) "ع" else "ا"
                'b' -> "ب"
                'p' -> "پ"
                't' -> if (char.isUpperCase()) "ٹ" else "ت"
                'j' -> "ج"
                'h' -> if (char.isUpperCase()) "ھ" else "ہ"
                'x' -> "خ"
                'd' -> if (char.isUpperCase()) "ڈ" else "د"
                'r' -> if (char.isUpperCase()) "ڑ" else "ر"
                'z' -> if (char.isUpperCase()) "ض" else "ز"
                's' -> if (char.isUpperCase()) "ص" else "س"
                'f' -> "ف"
                'q' -> "ق"
                'k' -> "ک"
                'g' -> if (char.isUpperCase()) "غ" else "گ"
                'l' -> "ل"
                'm' -> "م"
                'n' -> if (char.isUpperCase()) "ں" else "ن"
                'w' -> "و"
                'v' -> "و"
                'o' -> "و"
                'i' -> "ی"
                'y' -> "ی"
                'e' -> "ے"
                'u' -> "و"
                'c' -> "ک"
                ' ' -> " "
                else -> char.toString()
            }
            sb.append(matchSingle)
            i++
        }
        return sb.toString()
    }

    // --- Developer Contact Matching & DB-to-DB Sizing Sharing System ---

    val matchedContacts = MutableStateFlow<List<DeviceContact>>(emptyList())
    val contactMatchingLoading = MutableStateFlow(false)

    private var usersListenerRegistration: com.google.firebase.firestore.ListenerRegistration? = null

    fun startRealtimeContactSync(context: Context) {
        if (usersListenerRegistration != null) return
        try {
            val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            usersListenerRegistration = firestore.collection("global_owners_directory")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("TailorViewModel", "Realtime Sync error", error)
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        matchContacts(context)
                    }
                }
        } catch (e: Exception) {
            Log.e("TailorViewModel", "Error starting realtime contact sync", e)
        }
    }

    private fun normalizePhone(phone: String): String {
        val clean = phone.replace(Regex("[^0-9]"), "")
        return when {
            clean.startsWith("92") && clean.length == 12 -> "0" + clean.substring(2)
            clean.startsWith("0092") && clean.length == 14 -> "0" + clean.substring(4)
            else -> clean
        }
    }

    fun matchContacts(context: Context) {
        viewModelScope.launch {
            contactMatchingLoading.value = true
            val list = mutableListOf<DeviceContact>()
            try {
                if (androidx.core.content.ContextCompat.checkSelfPermission(
                        context,
                        android.Manifest.permission.READ_CONTACTS
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                ) {
                    val resolver = context.contentResolver
                    val cursor = resolver.query(
                        android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        arrayOf(
                            android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                            android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER
                        ),
                        null, null, null
                    )
                    cursor?.use {
                        val nameIdx = it.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                        val phoneIdx = it.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER)
                        while (it.moveToNext()) {
                            val name = if (nameIdx >= 0) it.getString(nameIdx) ?: "" else ""
                            val phone = if (phoneIdx >= 0) it.getString(phoneIdx) ?: "" else ""
                            if (name.isNotBlank() && phone.isNotBlank()) {
                                list.add(DeviceContact(name, phone))
                            }
                        }
                    }
                }
            } catch (ex: Exception) {
                Log.e("TailorViewModel", "Error fetching device address book", ex)
            }

            // Always add mock templates to demonstrate and test functionality seamlessly
            if (list.none { it.phone.contains("3001234567") }) {
                list.add(DeviceContact("Partner Master Tailor", "+92 300 1234567"))
            }
            if (list.none { it.phone.contains("5550199922") }) {
                list.add(DeviceContact("City Stitching Hub", "+1 (555) 019-9922"))
            }

            val cleanList = list.distinctBy { normalizePhone(it.phone) }

            try {
                val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                firestore.collection("global_owners_directory").get()
                    .addOnSuccessListener { snapshot ->
                        val matchedList = cleanList.map { contact ->
                            val cleanPhone = normalizePhone(contact.phone)
                            if (cleanPhone.length >= 7) {
                                val userDoc = snapshot.documents.find { doc ->
                                    val dbPhone = normalizePhone(doc.getString("phone") ?: "")
                                    dbPhone.isNotEmpty() && (dbPhone == cleanPhone || dbPhone.endsWith(cleanPhone) || cleanPhone.endsWith(dbPhone))
                                }
                                if (userDoc != null) {
                                    contact.copy(isAppUser = true, appUserId = userDoc.id)
                                } else {
                                    contact
                                }
                            } else {
                                contact
                            }
                        }
                        matchedContacts.value = matchedList.sortedByDescending { it.isAppUser }
                        contactMatchingLoading.value = false
                    }
                    .addOnFailureListener {
                        matchedContacts.value = cleanList
                        contactMatchingLoading.value = false
                    }
            } catch (e: Exception) {
                matchedContacts.value = cleanList
                contactMatchingLoading.value = false
            }
        }
    }

    fun shareCustomerMeasurements(receiverUserId: String, customer: Customer, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            if (blockedUserIds.value.contains(receiverUserId)) {
                withContext(Dispatchers.Main) {
                    onResult(false, "You have blocked this contact. Unblock them to share.")
                }
                return@launch
            }
            if (usersWhoBlockedMe.value.contains(receiverUserId)) {
                withContext(Dispatchers.Main) {
                    onResult(false, "This contact has blocked you or is not accepting shares.")
                }
                return@launch
            }
            try {
                // Read local customer measurements and active sizing values
                val mList = db.customerMeasurementDao().getMeasurementsForCustomer(customer.id, customer.userId)
                val valList = db.measurementValueDao().getValuesByCustomer(customer.id, customer.userId)

                val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                val batch = firestore.batch()

                // Register customer document under recipient's partition
                val customerDocRef = firestore.collection("users").document(receiverUserId)
                    .collection("customers").document(customer.id)
                val receiverCustomer = customer.copy(userId = receiverUserId, isSynced = true)
                batch.set(customerDocRef, receiverCustomer)

                // Inject child customer measurements under recipient's partition
                for (m in mList) {
                    val mDocRef = firestore.collection("users").document(receiverUserId)
                        .collection("customer_measurements").document(m.id)
                    batch.set(mDocRef, m.copy(userId = receiverUserId, isSynced = true))
                }

                // Inject related values under recipient's partition
                for (v in valList) {
                    val vDocRef = firestore.collection("users").document(receiverUserId)
                        .collection("measurement_values").document(v.id)
                    batch.set(vDocRef, v.copy(userId = receiverUserId, isSynced = true))
                }

                batch.commit()
                    .addOnSuccessListener {
                        onResult(true, null)
                    }
                    .addOnFailureListener { e ->
                        onResult(false, e.localizedMessage)
                    }
            } catch (e: Exception) {
                onResult(false, e.localizedMessage)
            }
        }
    }

    // --- Employee & Worker Management System Operations ---

    fun insertEmployee(name: String, phoneOrEmail: String, role: String, defaultPieceRate: Double, notes: String, rateItemsJson: String = "") {
        viewModelScope.launch(Dispatchers.IO) {
            val employee = Employee(
                id = UUID.randomUUID().toString(),
                name = name,
                phoneOrEmail = phoneOrEmail,
                role = role,
                defaultPieceRate = defaultPieceRate,
                notes = notes,
                createdAt = System.currentTimeMillis(),
                rateItemsJson = rateItemsJson
            )
            db.employeeDao().insertEmployee(employee)
        }
    }

    fun deleteEmployee(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            db.employeeDao().deleteEmployee(id)
        }
    }

    fun insertEmployeeWorkRecord(employeeId: String, suitType: String, piecesCount: Int, ratePaid: Double, notes: String, dateOverride: Long? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val totalAmount = piecesCount * ratePaid
            val record = EmployeeWorkRecord(
                id = UUID.randomUUID().toString(),
                employeeId = employeeId,
                suitType = suitType,
                piecesCount = piecesCount,
                ratePaid = ratePaid,
                totalAmount = totalAmount,
                date = dateOverride ?: System.currentTimeMillis(),
                notes = notes
            )
            db.employeeWorkRecordDao().insertWorkRecord(record)
        }
    }

    fun updateEmployeeWorkRecord(id: String, suitType: String, piecesCount: Int, ratePaid: Double, notes: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val totalAmount = piecesCount * ratePaid
            val cur = db.employeeWorkRecordDao().getWorkRecordById(id)
            if (cur != null) {
                val updated = cur.copy(
                    suitType = suitType,
                    piecesCount = piecesCount,
                    ratePaid = ratePaid,
                    totalAmount = totalAmount,
                    notes = notes
                )
                db.employeeWorkRecordDao().insertWorkRecord(updated)
            }
        }
    }

    fun deleteEmployeeWorkRecord(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            db.employeeWorkRecordDao().deleteWorkRecord(id)
        }
    }

    fun insertEmployeePaymentRecord(employeeId: String, paymentType: String, amount: Double, notes: String, dateOverride: Long? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val record = EmployeePaymentRecord(
                id = UUID.randomUUID().toString(),
                employeeId = employeeId,
                paymentType = paymentType,
                amount = amount,
                date = dateOverride ?: System.currentTimeMillis(),
                notes = notes
            )
            db.employeePaymentRecordDao().insertPaymentRecord(record)

            // Auto-close check: retrieve all active (unclosed) work and payment records for this employee
            val activeWork = db.employeeWorkRecordDao().getWorkRecordsForEmployee(employeeId).filter { !it.isClosed }
            val activePayments = db.employeePaymentRecordDao().getPaymentRecordsForEmployee(employeeId).filter { !it.isClosed }

            val totalActiveWages = activeWork.sumOf { it.totalAmount }
            val totalWagesPaidInActive = activePayments.filter { it.paymentType == "PAYMENT" }.sumOf { it.amount }
            val totalAdvancesInActive = activePayments.filter { it.paymentType == "ADVANCE" }.sumOf { it.amount }
            val activeOutstandingBalance = totalActiveWages - (totalWagesPaidInActive + totalAdvancesInActive)

            if (activeWork.isNotEmpty() && activeOutstandingBalance <= 0.0) {
                // Auto-close period because employee has been fully paid!
                val closingTime = System.currentTimeMillis()
                val df = java.text.SimpleDateFormat("dd/MM/yyyy hh:mm a", java.util.Locale.getDefault())
                val formattedDate = df.format(java.util.Date(closingTime))
                val closingTag = " [Closed Period on $formattedDate]"

                activeWork.forEach { r ->
                    val updated = r.copy(
                        isClosed = true,
                        notes = if (r.notes.isBlank()) "Closed on $formattedDate" else "${r.notes}$closingTag"
                    )
                    db.employeeWorkRecordDao().insertWorkRecord(updated)
                }

                activePayments.forEach { r ->
                    val updated = r.copy(
                        isClosed = true,
                        notes = if (r.notes.isBlank()) "Closed on $formattedDate" else "${r.notes}$closingTag"
                    )
                    db.employeePaymentRecordDao().insertPaymentRecord(updated)
                }

                db.employeeDao().updateEmployeeCycleStatus(employeeId, false)
            }
        }
    }

    fun deleteEmployeePaymentRecord(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            db.employeePaymentRecordDao().deletePaymentRecord(id)
        }
    }

    fun closeEmployeePeriod(employeeId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val activeWork = db.employeeWorkRecordDao().getWorkRecordsForEmployee(employeeId).filter { !it.isClosed }
            val activePayments = db.employeePaymentRecordDao().getPaymentRecordsForEmployee(employeeId).filter { !it.isClosed }

            val closingTime = System.currentTimeMillis()
            val df = java.text.SimpleDateFormat("dd/MM/yyyy hh:mm a", java.util.Locale.getDefault())
            val formattedDate = df.format(java.util.Date(closingTime))
            val closingTag = " [Closed Period on $formattedDate]"

            activeWork.forEach { r ->
                val updated = r.copy(
                    isClosed = true,
                    notes = if (r.notes.isBlank()) "Closed on $formattedDate" else "${r.notes}$closingTag"
                )
                db.employeeWorkRecordDao().insertWorkRecord(updated)
            }

            activePayments.forEach { r ->
                val updated = r.copy(
                    isClosed = true,
                    notes = if (r.notes.isBlank()) "Closed on $formattedDate" else "${r.notes}$closingTag"
                )
                db.employeePaymentRecordDao().insertPaymentRecord(updated)
            }

            db.employeeDao().updateEmployeeCycleStatus(employeeId, false)
        }
    }

    fun openEmployeePeriod(employeeId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            db.employeeDao().updateEmployeeCycleStatus(employeeId, true)
        }
    }

    // --- REALTIME CHAT & NOTIFICATION ENGINE ---

    private var chatListenerRegistration: com.google.firebase.firestore.ListenerRegistration? = null
    val currentChatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())

    fun listenToChat(partnerUserId: String) {
        val currentUserId = currentUser.value?.userId ?: return
        chatListenerRegistration?.remove()
        currentChatMessages.value = emptyList()

        if (partnerUserId.isBlank()) return

        val chatId = if (currentUserId < partnerUserId) "${currentUserId}_${partnerUserId}" else "${partnerUserId}_${currentUserId}"
        
        try {
            val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            chatListenerRegistration = firestore.collection("chats")
                .document(chatId)
                .collection("messages")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.ASCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("TailorViewModel", "Chat listener error", error)
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val list = snapshot.documents.mapNotNull { doc ->
                            try {
                                doc.toObject(ChatMessage::class.java)
                            } catch (e: Exception) {
                                null
                            }
                        }
                        currentChatMessages.value = list
                    }
                }
        } catch (e: Exception) {
            Log.e("TailorViewModel", "Error starting chat listener", e)
        }
    }

    fun stopListeningToChat() {
        chatListenerRegistration?.remove()
        chatListenerRegistration = null
        currentChatMessages.value = emptyList()
    }

    fun sendChatMessage(receiverId: String, content: String, type: String = "text", customerNames: List<String> = emptyList(), onResult: (Boolean, String?) -> Unit) {
        val currentUserId = currentUser.value?.userId ?: return
        if (blockedUserIds.value.contains(receiverId)) {
            onResult(false, "You have blocked this contact. Unblock them to send messages.")
            return
        }
        if (usersWhoBlockedMe.value.contains(receiverId)) {
            onResult(false, "This contact has blocked you. Messages cannot be sent.")
            return
        }
        val currentUserName = shopName.value.ifEmpty { "Owner" }

        val chatId = if (currentUserId < receiverId) "${currentUserId}_${receiverId}" else "${receiverId}_${currentUserId}"
        val messageId = UUID.randomUUID().toString()
        val timestamp = System.currentTimeMillis()

        val msg = ChatMessage(
            id = messageId,
            senderId = currentUserId,
            senderName = currentUserName,
            receiverId = receiverId,
            content = content,
            timestamp = timestamp,
            type = type,
            customerNames = customerNames
        )

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                
                // 1. Send the message
                firestore.collection("chats")
                    .document(chatId)
                    .collection("messages")
                    .document(messageId)
                    .set(msg)
                    .await()
                
                // 2. Add notification for receiver
                val notifId = UUID.randomUUID().toString()
                val notification = AppNotification(
                    id = notifId,
                    senderId = currentUserId,
                    senderName = currentUserName,
                    title = "New message from $currentUserName 📥",
                    message = if (type == "measurements") "Shared body measurement cards for: ${customerNames.joinToString(", ")}" else content,
                    timestamp = timestamp,
                    isRead = false
                )
                
                firestore.collection("users")
                    .document(receiverId)
                    .collection("notifications")
                    .document(notifId)
                    .set(notification)
                    .await()

                withContext(Dispatchers.Main) {
                    onResult(true, null)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onResult(false, e.localizedMessage)
                }
            }
        }
    }

    fun sendMultipleCustomerMeasurements(receiverId: String, customersList: List<com.example.data.model.Customer>, onResult: (Boolean, String?) -> Unit) {
        val currentUserId = currentUser.value?.userId ?: return
        if (blockedUserIds.value.contains(receiverId)) {
            onResult(false, "You have blocked this contact. Unblock them to share.")
            return
        }
        if (usersWhoBlockedMe.value.contains(receiverId)) {
            onResult(false, "This contact has blocked you. Sizing cards cannot be shared.")
            return
        }
        val currentUserName = shopName.value.ifEmpty { "Owner" }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()

                val customerNames = mutableListOf<String>()
                val sharedPayloads = mutableListOf<SharedCustomerPayload>()

                for (customer in customersList) {
                    customerNames.add(customer.name)
                    
                    val mList = db.customerMeasurementDao().getMeasurementsForCustomer(customer.id, customer.userId)
                    val valList = db.measurementValueDao().getValuesByCustomer(customer.id, customer.userId)

                    sharedPayloads.add(
                        SharedCustomerPayload(
                            customer = customer,
                            measurements = mList,
                            values = valList
                        )
                    )
                }

                val chatId = if (currentUserId < receiverId) "${currentUserId}_${receiverId}" else "${receiverId}_${currentUserId}"
                val messageId = UUID.randomUUID().toString()
                val timestamp = System.currentTimeMillis()

                val msg = ChatMessage(
                    id = messageId,
                    senderId = currentUserId,
                    senderName = currentUserName,
                    receiverId = receiverId,
                    content = "Shared body measurement cards for: ${customerNames.joinToString(", ")}",
                    timestamp = timestamp,
                    type = "measurements",
                    customerNames = customerNames,
                    sharedCustomers = sharedPayloads,
                    shareStatus = "pending"
                )

                firestore.collection("chats")
                    .document(chatId)
                    .collection("messages")
                    .document(messageId)
                    .set(msg)
                    .await()

                val notifId = UUID.randomUUID().toString()
                val notification = AppNotification(
                    id = notifId,
                    senderId = currentUserId,
                    senderName = currentUserName,
                    title = "New Sizing Cards Shared 📏",
                    message = "Shared body measurement cards for: ${customerNames.joinToString(", ")}",
                    timestamp = timestamp,
                    isRead = false
                )
                
                firestore.collection("users")
                    .document(receiverId)
                    .collection("notifications")
                    .document(notifId)
                    .set(notification)
                    .await()

                withContext(Dispatchers.Main) {
                    onResult(true, null)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("TailorViewModel", "Error sharing multiple measurements", e)
                    onResult(false, e.localizedMessage)
                }
            }
        }
    }

    fun acceptSharedCustomers(chatId: String, msg: ChatMessage, onResult: (Boolean, String?) -> Unit) {
        val currentUserId = currentUser.value?.userId ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val customersToInsert = mutableListOf<com.example.data.model.Customer>()
                val measurementsToInsert = mutableListOf<com.example.data.model.CustomerMeasurement>()
                val valuesToInsert = mutableListOf<com.example.data.model.MeasurementValue>()

                val now = System.currentTimeMillis()
                for (payload in msg.sharedCustomers) {
                    // Adapt all documents for the recipient's partition so their own local and firestore sync processes them correctly
                    customersToInsert.add(payload.customer.copy(userId = currentUserId, isSynced = false, updatedAt = now))
                    for (m in payload.measurements) {
                        measurementsToInsert.add(m.copy(userId = currentUserId, isSynced = false, updatedAt = now))
                    }
                    for (v in payload.values) {
                        valuesToInsert.add(v.copy(userId = currentUserId, isSynced = false, updatedAt = now))
                    }
                }

                // Write directly to local Room SQLite DB
                if (customersToInsert.isNotEmpty()) {
                    db.customerDao().insertCustomers(customersToInsert)
                }
                if (measurementsToInsert.isNotEmpty()) {
                    db.customerMeasurementDao().insertMeasurements(measurementsToInsert)
                }
                if (valuesToInsert.isNotEmpty()) {
                    db.measurementValueDao().insertValues(valuesToInsert)
                }

                // Update message shareStatus to accepted on Firebase
                val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                firestore.collection("chats")
                    .document(chatId)
                    .collection("messages")
                    .document(msg.id)
                    .update("shareStatus", "accepted")
                    .await()

                // Trigger cloud auto sync so the newly accepted records are standard-backed up immediately
                val syncUserId = if (currentUser.value?.isSubUser == true) currentUser.value?.ownerId ?: currentUserId else currentUserId
                syncManager.triggerSync(syncUserId)

                withContext(Dispatchers.Main) {
                    onResult(true, null)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("TailorViewModel", "Error accepting shared data", e)
                    onResult(false, e.localizedMessage)
                }
            }
        }
    }

    fun rejectSharedCustomers(chatId: String, msg: ChatMessage, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                firestore.collection("chats")
                    .document(chatId)
                    .collection("messages")
                    .document(msg.id)
                    .update("shareStatus", "rejected")
                    .await()

                withContext(Dispatchers.Main) {
                    onResult(true, null)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("TailorViewModel", "Error rejecting shared data", e)
                    onResult(false, e.localizedMessage)
                }
            }
        }
    }

    private var notificationsListenerRegistration: com.google.firebase.firestore.ListenerRegistration? = null
    val notifications = MutableStateFlow<List<AppNotification>>(emptyList())
    val unreadNotificationsCount = MutableStateFlow(0)
    val latestIncomingNotification = MutableSharedFlow<AppNotification>(extraBufferCapacity = 5)

    fun startListeningToNotifications() {
        val currentUserId = currentUser.value?.userId ?: return
        notificationsListenerRegistration?.remove()

        try {
            val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            notificationsListenerRegistration = firestore.collection("users")
                .document(currentUserId)
                .collection("notifications")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("TailorViewModel", "Notification listener error", error)
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val prevCount = notifications.value.filter { !it.isRead }.size
                        
                        val list = snapshot.documents.mapNotNull { doc ->
                            try {
                                doc.toObject(AppNotification::class.java)
                            } catch (e: Exception) {
                                null
                            }
                        }
                        notifications.value = list
                        
                        val newUnread = list.filter { !it.isRead }
                        unreadNotificationsCount.value = newUnread.size

                        if (newUnread.size > prevCount && newUnread.isNotEmpty()) {
                            val newest = newUnread.first()
                            viewModelScope.launch {
                                latestIncomingNotification.emit(newest)
                            }
                        }
                    }
                }
        } catch (e: Exception) {
            Log.e("TailorViewModel", "Error starting notifications listener", e)
        }
    }

    fun stopListeningToNotifications() {
        notificationsListenerRegistration?.remove()
        notificationsListenerRegistration = null
    }

    fun markNotificationsAsRead() {
        val currentUserId = currentUser.value?.userId ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                val batch = firestore.batch()
                val unread = notifications.value.filter { !it.isRead }
                for (notif in unread) {
                    val docRef = firestore.collection("users")
                        .document(currentUserId)
                        .collection("notifications")
                        .document(notif.id)
                    batch.update(docRef, "isRead", true)
                }
                batch.commit().await()
            } catch (e: Exception) {
                Log.e("TailorViewModel", "Error marking notifications as read", e)
            }
        }
    }

    fun clearNotifications() {
        val currentUserId = currentUser.value?.userId ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                val batch = firestore.batch()
                for (notif in notifications.value) {
                    val docRef = firestore.collection("users")
                        .document(currentUserId)
                        .collection("notifications")
                        .document(notif.id)
                    batch.delete(docRef)
                }
                batch.commit().await()
            } catch (e: Exception) {
                Log.e("TailorViewModel", "Error clearing notifications", e)
            }
        }
    }

    private var blocksListenerRegistration: com.google.firebase.firestore.ListenerRegistration? = null
    private var blockedMeListenerRegistration: com.google.firebase.firestore.ListenerRegistration? = null

    fun startListeningToBlocks() {
        val currentUserId = currentUser.value?.userId ?: return
        blocksListenerRegistration?.remove()
        blockedMeListenerRegistration?.remove()

        try {
            val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()

            // Listen to people I blocked
            blocksListenerRegistration = firestore.collection("users")
                .document(currentUserId)
                .collection("blocks")
                .addSnapshotListener { snapshot, error ->
                    if (error == null && snapshot != null) {
                        val ids = snapshot.documents.map { it.id }.toSet()
                        blockedUserIds.value = ids
                    }
                }

            // Listen to people who blocked me
            blockedMeListenerRegistration = firestore.collection("users")
                .document(currentUserId)
                .collection("blocked_by")
                .addSnapshotListener { snapshot, error ->
                    if (error == null && snapshot != null) {
                        val ids = snapshot.documents.map { it.id }.toSet()
                        usersWhoBlockedMe.value = ids
                    }
                }
        } catch (e: Exception) {
            Log.e("TailorViewModel", "Error starting blocks listener", e)
        }
    }

    fun stopListeningToBlocks() {
        blocksListenerRegistration?.remove()
        blocksListenerRegistration = null
        blockedMeListenerRegistration?.remove()
        blockedMeListenerRegistration = null
        blockedUserIds.value = emptySet()
        usersWhoBlockedMe.value = emptySet()
    }

    fun blockUser(blockedUserId: String, onResult: (Boolean, String?) -> Unit) {
        val currentUserId = currentUser.value?.userId ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                val batch = firestore.batch()

                // Add to my blocked list
                val blockDoc = firestore.collection("users")
                    .document(currentUserId)
                    .collection("blocks")
                    .document(blockedUserId)
                batch.set(blockDoc, mapOf("blocked" to true, "timestamp" to System.currentTimeMillis()))

                // Add to their blocked_by list
                val blockedByDoc = firestore.collection("users")
                    .document(blockedUserId)
                    .collection("blocked_by")
                    .document(currentUserId)
                batch.set(blockedByDoc, mapOf("blockedByMe" to true, "timestamp" to System.currentTimeMillis()))

                batch.commit().await()
                withContext(Dispatchers.Main) {
                    onResult(true, null)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onResult(false, e.localizedMessage)
                }
            }
        }
    }

    fun unblockUser(blockedUserId: String, onResult: (Boolean, String?) -> Unit) {
        val currentUserId = currentUser.value?.userId ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                val batch = firestore.batch()

                // Delete from my blocked list
                val blockDoc = firestore.collection("users")
                    .document(currentUserId)
                    .collection("blocks")
                    .document(blockedUserId)
                batch.delete(blockDoc)

                // Delete from their blocked_by list
                val blockedByDoc = firestore.collection("users")
                    .document(blockedUserId)
                    .collection("blocked_by")
                    .document(currentUserId)
                batch.delete(blockedByDoc)

                batch.commit().await()
                withContext(Dispatchers.Main) {
                    onResult(true, null)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onResult(false, e.localizedMessage)
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        usersListenerRegistration?.remove()
        usersListenerRegistration = null
        chatListenerRegistration?.remove()
        chatListenerRegistration = null
        notificationsListenerRegistration?.remove()
        notificationsListenerRegistration = null
        stopListeningToBlocks()
    }
}
