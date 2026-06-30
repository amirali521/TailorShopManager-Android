package com.example.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.viewinterop.AndroidView
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.TextDecoration
import com.example.data.model.*
import com.example.ui.viewmodel.TailorViewModel
import com.example.ui.viewmodel.SubUser
import com.example.ui.viewmodel.ChatMessage
import com.example.ui.viewmodel.AppNotification
import com.example.auth.UserSession
import com.example.util.AdMobBanner
import com.example.util.AdMobNativeAd
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.roundToInt

private fun String.capitalizeEveryWord(): String {
    if (this.isBlank()) return this
    return this.split(" ")
        .filter { it.isNotEmpty() }
        .joinToString(" ") { word ->
            word.lowercase(Locale.getDefault()).replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(
    viewModel: TailorViewModel,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val customers by viewModel.customers.collectAsState()
    val templates by viewModel.templates.collectAsState()
    val fields by viewModel.fields.collectAsState()
    val customerMeasurements by viewModel.customerMeasurements.collectAsState()
    val values by viewModel.measurementValues.collectAsState()
    val inventory by viewModel.inventory.collectAsState()
    val ledgerRecords by viewModel.ledgerRecords.collectAsState()
    val paymentRecords by viewModel.paymentRecords.collectAsState()
    val orderRecords by viewModel.orderRecords.collectAsState()
    val employees by viewModel.employees.collectAsState()
    val employeeWorkRecords by viewModel.employeeWorkRecords.collectAsState()
    val employeePaymentRecords by viewModel.employeePaymentRecords.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val isSubUser = currentUser?.isSubUser == true
    val shopName by viewModel.shopName.collectAsState()
    val shopPhone by viewModel.shopPhone.collectAsState()
    val shopAddress by viewModel.shopAddress.collectAsState()

    val isAppLockEnabled by viewModel.isAppLockEnabled.collectAsState()
    val appLockPin by viewModel.appLockPin.collectAsState()
    val isBiometricEnabled by viewModel.isBiometricEnabled.collectAsState()

    var isAppUnlocked by remember { mutableStateOf(true) }

    LaunchedEffect(isAppLockEnabled, appLockPin) {
        if (isAppLockEnabled && appLockPin.isNotBlank()) {
            isAppUnlocked = false
        } else {
            isAppUnlocked = true
        }
    }

    var selectedTab by remember { mutableStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }

    var customerFabOffset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    var materialFabOffset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    var templateFabOffset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }

    var customerFabWidth by remember { mutableStateOf(0f) }
    var customerFabHeight by remember { mutableStateOf(0f) }
    var materialFabWidth by remember { mutableStateOf(0f) }
    var materialFabHeight by remember { mutableStateOf(0f) }
    var templateFabWidth by remember { mutableStateOf(0f) }
    var templateFabHeight by remember { mutableStateOf(0f) }

    val showInterstitial = {
        (context as? android.app.Activity)?.let { activity ->
            com.example.util.AdManager.showInterstitialAd(activity)
        }
    }
    
    val isUrduMode by viewModel.isUrduSelected.collectAsState()
    val currencySymbol by viewModel.selectedCurrency.collectAsState()
    var showUrduKeyboardDialog by remember { mutableStateOf(false) }

    // Forced Profile setup state
    var forcedShopName by remember { mutableStateOf("") }
    var forcedShopPhone by remember { mutableStateOf("") }
    var forcedShopAddress by remember { mutableStateOf("") }
    var showForcedSetupDialog by remember { mutableStateOf(false) }
    var userDismissedSetup by remember { mutableStateOf(false) }

    val isProfileIncomplete = !isSubUser && (
        shopName.isBlank() || shopName == "Tailor Shop Master" || 
        shopPhone.isBlank() || shopPhone == "+1 (555) 019-9922" || 
        shopAddress.isBlank() || shopAddress == "City Tailoring Blvd, Suite 100"
    )

    LaunchedEffect(isProfileIncomplete, shopName, shopPhone, shopAddress, userDismissedSetup) {
        if (isProfileIncomplete && !userDismissedSetup) {
            if (forcedShopName.isBlank()) {
                forcedShopName = if (shopName == "Tailor Shop Master") "" else shopName
            }
            if (forcedShopPhone.isBlank()) {
                forcedShopPhone = if (shopPhone == "+1 (555) 019-9922") "" else shopPhone
            }
            if (forcedShopAddress.isBlank()) {
                forcedShopAddress = if (shopAddress == "City Tailoring Blvd, Suite 100") "" else shopAddress
            }
            showForcedSetupDialog = true
        } else {
            showForcedSetupDialog = false
        }
    }

    LaunchedEffect(Unit) {
        viewModel.latestIncomingNotification.collect { notif ->
            Toast.makeText(context, "🔔 Alert: ${notif.title}\n${notif.message}", Toast.LENGTH_LONG).show()
        }
    }

    var showNotifCenterDialog by remember { mutableStateOf(false) }

    // Dialog state
    var showAddCustomerDialog by remember { mutableStateOf(false) }
    var showAddInventoryDialog by remember { mutableStateOf(false) }
    var showCreateTemplateDialog by remember { mutableStateOf(false) }
    val activeEditingTemplateId by viewModel.activeEditingTemplateId.collectAsState()

    var showSignInAuthChoices by remember { mutableStateOf(false) }
    var showSubUserSignInDialog by remember { mutableStateOf(false) }
    var subUserLoginId by remember { mutableStateOf("") }
    var subUserLoginPasscode by remember { mutableStateOf("") }

    // Google Sign-In SDK configuration
    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("995723418350-2g2ilju4dda51uhihjtj43h1850qtfgf.apps.googleusercontent.com")
            .requestEmail()
            .build()
    }
    val googleSignInClient = remember {
        GoogleSignIn.getClient(context, gso)
    }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account?.idToken
            if (idToken != null) {
                viewModel.signInGoogle(idToken) { success, errorMsg ->
                    if (success) {
                        Toast.makeText(context, "Successfully connected cloud account: ${account.email}", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "Firebase authenticating failed: ${errorMsg ?: "Unknown error"}", Toast.LENGTH_LONG).show()
                    }
                }
            } else {
                Toast.makeText(context, "Google Sign-In failed: No ID Token retrieved from Google SDK.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Google Sign-In failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    if (!isAppUnlocked) {
        AppLockScreen(
            correctPin = appLockPin,
            isBiometricEnabled = isBiometricEnabled,
            registeredEmail = currentUser?.email ?: "your registered customer profile",
            isSubUser = currentUser?.isSubUser == true,
            onUnlocked = { isAppUnlocked = true }
        )
    } else {
        Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Tailor Workspace",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when (syncStatus) {
                                            "Uploaded & Synced" -> Color(0xFF386A20)
                                            "Syncing..." -> Color(0xFFDAC543)
                                            else -> Color(0xFFBA1A1A)
                                        }
                                    )
                            )
                            Text(
                                text = if (currentUser == null) "Cloud: Guest Sandbox" else "Cloud: $syncStatus",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    val unreadNotificationsCount by viewModel.unreadNotificationsCount.collectAsState()

                    BadgedBox(
                        badge = {
                            if (unreadNotificationsCount > 0) {
                                Badge(
                                    containerColor = MaterialTheme.colorScheme.error
                                ) {
                                    Text(unreadNotificationsCount.toString(), color = Color.White)
                                }
                            }
                        },
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("notification_bell_badge")
                    ) {
                        IconButton(onClick = { showNotifCenterDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Alerts Center",
                                tint = if (unreadNotificationsCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (showNotifCenterDialog) {
                        NotificationCenterDialog(
                            viewModel = viewModel,
                            onDismiss = { showNotifCenterDialog = false }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                )
            )
        },
        bottomBar = {
            androidx.compose.foundation.layout.Column {
                AdMobBanner()
                NavigationBar(
                    modifier = Modifier.testTag("dashboard_navigation_bar")
                ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0; showInterstitial() },
                    icon = { Icon(Icons.Default.People, contentDescription = "Customers") },
                    label = { Text("Customers") },
                    modifier = Modifier.testTag("tab_customers")
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1; showInterstitial() },
                    icon = { Icon(Icons.Default.Inventory2, contentDescription = "Inventory") },
                    label = { Text("Inventory") },
                    modifier = Modifier.testTag("tab_inventory")
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2; showInterstitial() },
                    icon = { Icon(Icons.Default.Straighten, contentDescription = "Template") },
                    label = { Text("Templates") },
                    modifier = Modifier.testTag("tab_template")
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3; showInterstitial() },
                    icon = { Icon(Icons.Default.AccountCircle, contentDescription = "Profile Setup & Backup") },
                    label = { Text("Profile") },
                    modifier = Modifier.testTag("tab_backups")
                )
            }
        }
    },
    floatingActionButton = {}
    ) { innerPadding ->
        SwipeToRefreshBox(
            isRefreshing = syncStatus == "Syncing...",
            onRefresh = { 
                viewModel.triggerSyncNow()
                Toast.makeText(context, "Cloud Refresh Triggered 🔄", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            BoxWithConstraints(
                modifier = Modifier.fillMaxSize()
            ) {
            val parentWidth = constraints.maxWidth.toFloat()
            val parentHeight = constraints.maxHeight.toFloat()
            val paddingPx = with(LocalDensity.current) { 16.dp.toPx() }

            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                AnimatedContent(
                    targetState = selectedTab,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "TabTransition",
                    modifier = Modifier.fillMaxSize()
                ) { tabIndex ->
                    when (tabIndex) {
                        0 -> CustomersTab(
                            customers = customers,
                            templates = templates,
                            fields = fields,
                            customerMeasurements = customerMeasurements,
                            values = values,
                            inventory = inventory,
                            ledgerRecords = ledgerRecords,
                            paymentRecords = paymentRecords,
                            orderRecords = orderRecords,
                            searchQuery = searchQuery,
                            onSearchChange = { searchQuery = it },
                            onSaveSize = { cId, mId, fId, valStr, subValStr, noteStr -> viewModel.saveMeasurementValue(cId, mId, fId, valStr, subValStr, noteStr) },
                            onUpdateLedger = { cId, bill, paid -> viewModel.updateLedger(cId, bill, paid) },
                            onAddPartialPayment = { cId, amount, note -> viewModel.addPartialPayment(cId, amount, note) },
                            onCreateOrder = { cId, name, price, adv, note, invId, qty -> viewModel.createOrder(cId, name, price, adv, note, invId, qty) },
                            onApplyMaterial = { cId, item, qty -> viewModel.applyInventorySaleToLedger(cId, item, qty) },
                            onCreateMeasurement = { cId, tId, title -> viewModel.insertCustomerMeasurement(cId, tId, title) },
                            onDeleteMeasurement = { viewModel.deleteCustomerMeasurement(it) },
                            onDeleteCustomer = { viewModel.softDeleteCustomer(it) },
                            shopName = shopName,
                            shopPhone = shopPhone,
                            shopAddress = shopAddress,
                            viewModel = viewModel
                        )
                        1 -> InventoryTab(
                            inventory = inventory,
                            onAddItem = { name, unit, buy, sell, qty ->
                                viewModel.insertInventoryItem(name, unit, buy, sell, qty)
                            },
                            onDeleteItem = { viewModel.softDeleteInventoryItem(it) },
                            onUpdateItem = { viewModel.updateInventoryItem(it) },
                            isSubUser = isSubUser
                        )
                        2 -> TemplateTab(
                            templates = templates,
                            fields = fields,
                            onCreateTemplate = { viewModel.insertTemplate(it) },
                            onDeleteTemplate = { viewModel.deleteTemplate(it) },
                            onAddField = { tId, label, type, opts, isDemand, hasSub, subType, subLabel, subOpts, subTarget ->
                                viewModel.insertField(tId, label, type, opts, isDemand, hasSub, subType, subLabel, subOpts, subTarget)
                            },
                            onDeleteField = { viewModel.deleteField(it) },
                            viewModel = viewModel,
                            onEditTemplate = { viewModel.activeEditingTemplateId.value = it }
                        )
                        3 -> BackupTab(
                            viewModel = viewModel,
                            onGoogleSignInClick = {
                                googleSignInLauncher.launch(googleSignInClient.signInIntent)
                            },
                            employees = employees,
                            workRecords = employeeWorkRecords,
                            paymentRecords = employeePaymentRecords,
                            customers = customers
                        )
                    }
                }
            }

            // Draggable Floating Action Buttons over active tabs
            val selectedCustomerForDetails by viewModel.selectedCustomerForDetails.collectAsState()
            when (selectedTab) {
                0 -> {
                    if (!isSubUser && !showAddCustomerDialog && selectedCustomerForDetails == null) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(16.dp)
                                .offset {
                                    IntOffset(
                                        customerFabOffset.x.roundToInt(),
                                        customerFabOffset.y.roundToInt()
                                    )
                                }
                                .pointerInput(parentWidth, parentHeight, customerFabWidth, customerFabHeight, paddingPx) {
                                    detectDragGestures { change, dragAmount ->
                                        change.consume()
                                        val newX = customerFabOffset.x + dragAmount.x
                                        val newY = customerFabOffset.y + dragAmount.y
                                        
                                        val minX = -(parentWidth - paddingPx * 2 - customerFabWidth)
                                        val maxX = 0f
                                        val minY = -(parentHeight - paddingPx * 2 - customerFabHeight)
                                        val maxY = 0f

                                        customerFabOffset = androidx.compose.ui.geometry.Offset(
                                            x = newX.coerceIn(minX, maxX),
                                            y = newY.coerceIn(minY, maxY)
                                        )
                                    }
                                }
                        ) {
                            ExtendedFloatingActionButton(
                                onClick = { showAddCustomerDialog = true },
                                icon = { Icon(Icons.Default.PersonAdd, "Add Customer") },
                                text = { Text("Customer") },
                                modifier = Modifier
                                    .testTag("add_customer_fab")
                                    .onSizeChanged { size ->
                                        customerFabWidth = size.width.toFloat()
                                        customerFabHeight = size.height.toFloat()
                                    }
                            )
                        }
                    }
                }
                1 -> {
                    if (!isSubUser && !showAddInventoryDialog) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(16.dp)
                                .offset {
                                    IntOffset(
                                        materialFabOffset.x.roundToInt(),
                                        materialFabOffset.y.roundToInt()
                                    )
                                }
                                .pointerInput(parentWidth, parentHeight, materialFabWidth, materialFabHeight, paddingPx) {
                                    detectDragGestures { change, dragAmount ->
                                        change.consume()
                                        val newX = materialFabOffset.x + dragAmount.x
                                        val newY = materialFabOffset.y + dragAmount.y
                                        
                                        val minX = -(parentWidth - paddingPx * 2 - materialFabWidth)
                                        val maxX = 0f
                                        val minY = -(parentHeight - paddingPx * 2 - materialFabHeight)
                                        val maxY = 0f

                                        materialFabOffset = androidx.compose.ui.geometry.Offset(
                                            x = newX.coerceIn(minX, maxX),
                                            y = newY.coerceIn(minY, maxY)
                                        )
                                    }
                                }
                        ) {
                            ExtendedFloatingActionButton(
                                onClick = { showAddInventoryDialog = true },
                                icon = { Icon(Icons.Default.PostAdd, "Add Material") },
                                text = { Text("Material") },
                                modifier = Modifier
                                    .testTag("add_inventory_fab")
                                    .onSizeChanged { size ->
                                        materialFabWidth = size.width.toFloat()
                                        materialFabHeight = size.height.toFloat()
                                    }
                            )
                        }
                    }
                }
                2 -> {
                    if (!isSubUser && !showCreateTemplateDialog && activeEditingTemplateId == null) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(16.dp)
                                .offset {
                                    IntOffset(
                                        templateFabOffset.x.roundToInt(),
                                        templateFabOffset.y.roundToInt()
                                    )
                                }
                                .pointerInput(parentWidth, parentHeight, templateFabWidth, templateFabHeight, paddingPx) {
                                    detectDragGestures { change, dragAmount ->
                                        change.consume()
                                        val newX = templateFabOffset.x + dragAmount.x
                                        val newY = templateFabOffset.y + dragAmount.y
                                        
                                        val minX = -(parentWidth - paddingPx * 2 - templateFabWidth)
                                        val maxX = 0f
                                        val minY = -(parentHeight - paddingPx * 2 - templateFabHeight)
                                        val maxY = 0f

                                        templateFabOffset = androidx.compose.ui.geometry.Offset(
                                            x = newX.coerceIn(minX, maxX),
                                            y = newY.coerceIn(minY, maxY)
                                        )
                                    }
                                }
                        ) {
                            ExtendedFloatingActionButton(
                                onClick = { showCreateTemplateDialog = true },
                                icon = { Icon(Icons.Default.LibraryAdd, "Add Template") },
                                text = { Text("New Template") },
                                modifier = Modifier
                                    .testTag("add_template_fab")
                                    .onSizeChanged { size ->
                                        templateFabWidth = size.width.toFloat()
                                        templateFabHeight = size.height.toFloat()
                                    }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showForcedSetupDialog) {
        Dialog(onDismissRequest = { showForcedSetupDialog = false; userDismissedSetup = true }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Setup Your Shop Profile ⚙",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Configure your shop name, contact number, and address below to personalize your client ledger receipts, invoice templates, and cloud sync services. You can dismiss this and configure or edit them at any time from your Account Profile tab.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )

                    OutlinedTextField(
                        value = forcedShopName,
                        onValueChange = { forcedShopName = it },
                        label = { Text("Shop / Studio Name") },
                        modifier = Modifier.fillMaxWidth().testTag("forced_shop_name_field"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = forcedShopPhone,
                        onValueChange = { forcedShopPhone = it },
                        label = { Text("Shop Contact Number") },
                        placeholder = { Text("e.g. 03000000000 or +923000000000") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth().testTag("forced_shop_phone_field"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = forcedShopAddress,
                        onValueChange = { forcedShopAddress = it },
                        label = { Text("Physical Studio / Workshop Address") },
                        modifier = Modifier.fillMaxWidth().testTag("forced_shop_address_field"),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                showForcedSetupDialog = false
                                userDismissedSetup = true
                            },
                            modifier = Modifier.weight(1f).height(48.dp).testTag("forced_shop_cancel_button"),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Text("Skip For Now", fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
                        }

                        Button(
                            onClick = {
                                val name = forcedShopName.trim()
                                val phone = forcedShopPhone.trim()
                                val address = forcedShopAddress.trim()

                                if (name.isBlank() || name == "Tailor Shop Master") {
                                    Toast.makeText(context, "Please enter a custom Shop Name", Toast.LENGTH_SHORT).show()
                                } else if (phone.isBlank() || phone == "+1 (555) 019-9922") {
                                    Toast.makeText(context, "Please enter a custom Shop Phone", Toast.LENGTH_SHORT).show()
                                } else if (address.isBlank() || address == "City Tailoring Blvd, Suite 100") {
                                    Toast.makeText(context, "Please enter a custom Shop Address", Toast.LENGTH_SHORT).show()
                                } else {
                                    viewModel.updateShopProfile(name, phone, address)
                                    Toast.makeText(context, "Registration Complete!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f).height(48.dp).testTag("forced_shop_register_button"),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Save Profile", fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
                        }
                    }
                }
            }
        }
    }

    if (showAddCustomerDialog) {
        AddCustomerDialog(
            onDismiss = { showAddCustomerDialog = false },
            onConfirm = { name, phone, address ->
                viewModel.insertCustomer(name, phone, address)
                showAddCustomerDialog = false
                Toast.makeText(context, "Customer Profile Created", Toast.LENGTH_SHORT).show()
            },
            viewModel = viewModel,
            isUrduMode = isUrduMode
        )
    }

    if (showAddInventoryDialog) {
        AddInventoryDialog(
            onDismiss = { showAddInventoryDialog = false },
            onConfirm = { name, unit, buy, sell, stock ->
                viewModel.insertInventoryItem(name, unit, buy, sell, stock)
                showAddInventoryDialog = false
                Toast.makeText(context, "Material Added to Stock", Toast.LENGTH_SHORT).show()
            },
            viewModel = viewModel,
            isUrduMode = isUrduMode
        )
    }

    if (showCreateTemplateDialog) {
        CreateTemplateDialog(
            onDismiss = { showCreateTemplateDialog = false },
            onConfirm = { name ->
                viewModel.insertTemplate(name)
                showCreateTemplateDialog = false
                Toast.makeText(context, "Custom Sizing Template Saved", Toast.LENGTH_SHORT).show()
            },
            viewModel = viewModel,
            isUrduMode = isUrduMode
        )
    }

    if (activeEditingTemplateId != null) {
        TemplateEditorFormDialog(
            templateId = activeEditingTemplateId!!,
            templates = templates,
            fields = fields,
            onDismiss = { viewModel.activeEditingTemplateId.value = null },
            onAddField = { tId, label, type, opts, isDemand, hasSub, subType, subLabel, subOpts, subTarget, hasNote ->
                viewModel.insertField(tId, label, type, opts, isDemand, hasSub, subType, subLabel, subOpts, subTarget, hasNote)
            },
            onDeleteField = { viewModel.deleteField(it) },
            onDeleteTemplate = { viewModel.deleteTemplate(it) },
            viewModel = viewModel,
            isUrduMode = isUrduMode
        )
    }
}
}
}

// ---------------- CUSTOMER DISPLAY TAB ----------------

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun CustomersTab(
    customers: List<Customer>,
    templates: List<SizingTemplate>,
    fields: List<MeasurementField>,
    customerMeasurements: List<CustomerMeasurement>,
    values: List<MeasurementValue>,
    inventory: List<InventoryItem>,
    ledgerRecords: List<LedgerRecord>,
    paymentRecords: List<PaymentRecord>,
    orderRecords: List<OrderRecord>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onSaveSize: (String, String, String, String, String, String) -> Unit,
    onUpdateLedger: (String, Double, Double) -> Unit,
    onAddPartialPayment: (String, Double, String) -> Unit,
    onCreateOrder: (String, String, Double, Double, String, String?, Int) -> Unit,
    onApplyMaterial: (String, InventoryItem, Int) -> Unit,
    onCreateMeasurement: (String, String, String) -> Unit,
    onDeleteMeasurement: (String) -> Unit,
    onDeleteCustomer: (Customer) -> Unit,
    shopName: String,
    shopPhone: String,
    shopAddress: String,
    viewModel: TailorViewModel
) {
    val selectedCustomerForDetails by viewModel.selectedCustomerForDetails.collectAsState()

    val customerDetails = selectedCustomerForDetails
    if (customerDetails != null) {
        val customerLedger = ledgerRecords.find { it.customerId == customerDetails.id }
        val customerValues = values.filter { it.customerId == customerDetails.id }
        val customerSheets = customerMeasurements.filter { it.customerId == customerDetails.id }

        CustomerDetailsPage(
            customer = customerDetails,
            templates = templates,
            fields = fields,
            sheets = customerSheets,
            values = customerValues,
            inventory = inventory,
            ledger = customerLedger,
            paymentRecords = paymentRecords,
            orderRecords = orderRecords,
            onSaveSize = onSaveSize,
            onUpdateLedger = onUpdateLedger,
            onAddPartialPayment = onAddPartialPayment,
            onCreateOrder = onCreateOrder,
            onApplyMaterial = onApplyMaterial,
            onCreateMeasurement = onCreateMeasurement,
            onDeleteMeasurement = onDeleteMeasurement,
            onDelete = {
                onDeleteCustomer(customerDetails)
                viewModel.selectedCustomerForDetails.value = null
            },
            shopName = shopName,
            shopPhone = shopPhone,
            shopAddress = shopAddress,
            viewModel = viewModel,
            onBack = { viewModel.selectedCustomerForDetails.value = null }
        )
    } else {
        val filteredCustomers = remember(customers, searchQuery) {
            customers.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                        it.phone.contains(searchQuery, ignoreCase = true)
            }
        }

        Column(modifier = Modifier.fillMaxSize()) {
            TextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text("Search customer name or contact...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("customer_search_input"),
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("customer_directory_total_row"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PeopleOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Total Customer",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    tonalElevation = 1.dp
                ) {
                    Text(
                        text = if (searchQuery.isBlank()) "${customers.size} Saved Total" else "${filteredCustomers.size} of ${customers.size} Found",
                        modifier = Modifier
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .testTag("total_customers_count_badge"),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (filteredCustomers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.PeopleOutline,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No Tailoring Customers",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Press '+' to setup customer profiles, ledger systems, and bespoke booklets.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(filteredCustomers, key = { it.id }) { customer ->
                        CustomerCard(
                            customer = customer,
                            onViewDetails = { viewModel.selectedCustomerForDetails.value = customer }
                        )
                    }
                }
            }
        }
    }
}

sealed class UnifiedTransaction {
    abstract val timestamp: Long
    abstract val title: String
    abstract val amountText: String
    abstract val note: String
    abstract val isOrder: Boolean

    data class OrderItem(val o: OrderRecord) : UnifiedTransaction() {
        override val timestamp: Long get() = o.orderDate
        override val title: String get() = o.itemName
        override val amountText: String get() = "- ${String.format(Locale.getDefault(), "%.2f", o.price)}"
        override val note: String get() = if (o.isCompleted) "Paid in Full" else "Pending Payment"
        override val isOrder: Boolean get() = true
    }

    data class PaymentItem(val p: PaymentRecord) : UnifiedTransaction() {
        override val timestamp: Long get() = p.paymentDate
        override val title: String get() = if (p.note.isNotEmpty()) p.note else "Partial Payment"
        override val amountText: String get() = "+ ${String.format(Locale.getDefault(), "%.2f", p.amountPaid)}"
        override val note: String get() = "Paid Advance/Part"
        override val isOrder: Boolean get() = false
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun CustomerCard(
    customer: Customer,
    onViewDetails: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("customer_card_${customer.name.lowercase().replace(" ", "_")}"),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Column(modifier = Modifier.fillMaxWidth().padding(end = 90.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "User Icon",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = customer.name.capitalizeEveryWord(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Phone,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = Color.Gray
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = customer.phone,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Place,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = Color.Gray
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = customer.address.capitalizeEveryWord(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }

            Button(
                onClick = onViewDetails,
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .testTag("view_details_button_${customer.id}")
                    .height(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Launch,
                    contentDescription = "View details",
                    modifier = Modifier.size(10.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = "Details",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun CustomerDetailsPage(
    customer: Customer,
    templates: List<SizingTemplate>,
    fields: List<MeasurementField>,
    sheets: List<CustomerMeasurement>,
    values: List<MeasurementValue>,
    inventory: List<InventoryItem>,
    ledger: LedgerRecord?,
    paymentRecords: List<PaymentRecord>,
    orderRecords: List<OrderRecord>,
    onSaveSize: (String, String, String, String, String, String) -> Unit,
    onUpdateLedger: (String, Double, Double) -> Unit,
    onAddPartialPayment: (String, Double, String) -> Unit,
    onCreateOrder: (String, String, Double, Double, String, String?, Int) -> Unit,
    onApplyMaterial: (String, InventoryItem, Int) -> Unit,
    onCreateMeasurement: (String, String, String) -> Unit,
    onDeleteMeasurement: (String) -> Unit,
    onDelete: () -> Unit,
    shopName: String,
    shopPhone: String,
    shopAddress: String,
    viewModel: TailorViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsState()
    val isSubUser = currentUser?.isSubUser == true

    // Sub-dialog indicators
    var showLedgerDialog by remember { mutableStateOf(false) }
    var showLinkMaterialDialog by remember { mutableStateOf(false) }
    var showAddMeasurementSheetDialog by remember { mutableStateOf(false) }
    var showAddPaymentDialog by remember { mutableStateOf(false) }
    var showAddOrderDialog by remember { mutableStateOf(false) }
    var showEditCustomerDialog by remember { mutableStateOf(false) }
    var showDeleteCustomerConfirmDialog by remember { mutableStateOf(false) }

    // Measurement booklet interaction states
    var activeFormMeasurementId by remember { mutableStateOf<String?>(null) }
    var activeReceiptMeasurementId by remember { mutableStateOf<String?>(null) }
    var pendingDeleteMeasurementId by remember { mutableStateOf<String?>(null) }
    var showLedgerReceiptDialog by remember { mutableStateOf(false) }
    var clickedTxForReceipt by remember { mutableStateOf<UnifiedTransaction?>(null) }

    // Subpage navigation & Help/Guide controls
    var showLedgerSubPage by remember { mutableStateOf(false) }
    var showLedgerGuideDialog by remember { mutableStateOf(false) }

    val customerOrders = remember(orderRecords, customer.id) { orderRecords.filter { it.customerId == customer.id } }
    val customerPayments = remember(paymentRecords, customer.id) { paymentRecords.filter { it.customerId == customer.id } }
    
    val transactions = remember(customerOrders, customerPayments) {
        val list = mutableListOf<UnifiedTransaction>()
        list.addAll(customerOrders.map { UnifiedTransaction.OrderItem(it) })
        list.addAll(customerPayments.map { UnifiedTransaction.PaymentItem(it) })
        list.sortByDescending { it.timestamp }
        list
    }

    if (showLedgerSubPage) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Top Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { showLedgerSubPage = false }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back to Workspace")
                    }
                    Text(
                        text = "${customer.name.uppercase(Locale.getDefault())}'s Ledger",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                // Help/Guide Button
                OutlinedButton(
                    onClick = { showLedgerGuideDialog = true },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                    modifier = Modifier.padding(end = 8.dp).height(32.dp)
                ) {
                    Icon(Icons.Default.HelpOutline, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Guide 📖", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // METRICS ROWS (fixed UI wrapping and NO currency sign)
                val snapshotTotal = ledger?.totalValue ?: 0.0
                val snapshotPaid = ledger?.amountPaid ?: 0.0
                val snapshotDebt = ledger?.pendingDebt ?: 0.0

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Total Job Cost / Bill Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Total Garment Bills (کل بل)", fontWeight = FontWeight.SemiBold, fontSize = 11.sp, color = Color.Gray)
                                Text("All tailorship jobs & orders created", fontSize = 10.sp, color = Color.Gray)
                            }
                            Text(
                                text = String.format(Locale.getDefault(), "%.2f", snapshotTotal),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                        }
                    }

                    // Total Cash Paid Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                        border = BorderStroke(0.5.dp, Color(0xFFC8E6C9))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Total Cash Paid (کل وصولی)", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF2E7D32))
                                Text("All advances & installment cash in hand", fontSize = 10.sp, color = Color(0xFF4CAF50))
                            }
                            Text(
                                text = String.format(Locale.getDefault(), "%.2f", snapshotPaid),
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                color = Color(0xFF2E7D32),
                                maxLines = 1
                            )
                        }
                    }

                    // Outstanding Debt Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (snapshotDebt > 0.0) Color(0xFFFFEBEE) else Color(0xFFE8F5E9)
                        ),
                        border = BorderStroke(
                            0.5.dp, 
                            if (snapshotDebt > 0.0) Color(0xFFFFCDD2) else Color(0xFFC8E6C9)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    if (snapshotDebt > 0.0) "Customer Debt - Balance Due (بقایا قرض)" else "No Debt Left - Completed (کوئی بقایا نہیں)", 
                                    fontWeight = FontWeight.Black, 
                                    fontSize = 12.sp, 
                                    color = if (snapshotDebt > 0.0) Color(0xFFC62828) else Color(0xFF2E7D32)
                                )
                                Text("The amount customer owes you", fontSize = 10.sp, color = Color.Gray)
                            }
                            Text(
                                text = String.format(Locale.getDefault(), "%.2f", snapshotDebt),
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp,
                                color = if (snapshotDebt > 0.0) Color(0xFFC62828) else Color(0xFF2E7D32),
                                maxLines = 1
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    "Ledger Actions (کھاتہ کے کام):",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // ACTIONS BUTTONS
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // 1. Create Order Button
                    Button(
                        onClick = { showAddOrderDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("create_order_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.AddShoppingCart, null, modifier = Modifier.size(18.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("➕ Create Sewing Order / Job", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text("نیا سلائی کا کام یا جوڑا آرڈر درج کریں", fontSize = 10.sp, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f))
                            }
                            Icon(Icons.Default.ArrowForward, null, modifier = Modifier.size(16.dp))
                        }
                    }

                    // 2. Add Payment Button
                    Button(
                        onClick = { showAddPaymentDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("pay_partial_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Payments, null, modifier = Modifier.size(18.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("💵 Receive Cash / Post Payment", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text("بقایا نقد رقم یا کوئی قسط وصول کریں", fontSize = 10.sp, color = Color.White.copy(alpha = 0.8f))
                            }
                            Icon(Icons.Default.ArrowForward, null, modifier = Modifier.size(16.dp))
                        }
                    }

                    // 3. Edit Bill Button
                    OutlinedButton(
                        onClick = { showLedgerDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("adjust_ledger_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Edit, null, modifier = Modifier.size(18.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Correct / Edit Bill", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text("غلطی کی اصلاح", fontSize = 10.sp, color = Color.Gray)
                            }
                            Icon(Icons.Default.ArrowForward, null, modifier = Modifier.size(16.dp))
                        }
                    }

                    // 4. View Statement Receipt Button
                    Button(
                        onClick = { showLedgerReceiptDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 46.dp)
                            .testTag("view_ledger_receipt_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.ReceiptLong, null, modifier = Modifier.size(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("📋 View Statement & Print Receipt", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text("پورے کھاتہ کی تفصیل دیکھیں اور پرنٹ کریں", fontSize = 10.sp, color = MaterialTheme.colorScheme.onTertiary.copy(alpha = 0.8f))
                            }
                            Icon(Icons.Default.ArrowForward, null, modifier = Modifier.size(14.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // HISTORY TITLE
                Text(
                    text = "Ledger History (${transactions.size})", 
                    fontWeight = FontWeight.ExtraBold, 
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                // SCROLLABLE CONTAINER FOR LEDGER TRANSACTIONS (Specific Size: holds 3-4 items)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    if (transactions.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No history recorded yet.", color = Color.Gray, fontSize = 13.sp)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize().padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(transactions) { tx ->
                                val txDateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(tx.timestamp))
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (tx.isOrder) 
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                                        else 
                                            Color(0xFF386A20).copy(alpha = 0.08f)
                                    ),
                                    border = BorderStroke(
                                        width = 0.5.dp, 
                                        color = if (tx.isOrder) 
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f) 
                                        else 
                                            Color(0xFF386A20).copy(alpha = 0.15f)
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { clickedTxForReceipt = tx }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = tx.title,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = txDateStr,
                                                    fontSize = 9.sp,
                                                    color = Color.Gray
                                                )
                                                Text(
                                                    text = "•  ${tx.note}",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = if (tx.isOrder) Color.DarkGray else Color(0xFF386A20)
                                                )
                                            }
                                        }
                                        Text(
                                            text = tx.amountText,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 12.sp,
                                            color = if (tx.isOrder) Color(0xFFBA1A1A) else Color(0xFF386A20)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Top Back Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Go back")
                }
                Text(
                    text = "${customer.name.uppercase(Locale.getDefault())}'s Workspace Details",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                // Header Info Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = "User Icon",
                                        modifier = Modifier.size(24.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = customer.name.capitalizeEveryWord(),
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                if (!isSubUser) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        IconButton(
                                            onClick = { showEditCustomerDialog = true },
                                            modifier = Modifier.testTag("edit_customer_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Edit Profile",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }

                                        IconButton(
                                            onClick = { showDeleteCustomerConfirmDialog = true },
                                            modifier = Modifier.testTag("delete_customer_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete Profile",
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Phone, null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = customer.phone, style = MaterialTheme.typography.bodyMedium)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Place, null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = customer.address.capitalizeEveryWord(), style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }

                // BEAUTIFUL TEMPLATE-LIKE FINANCIAL LEDGER BOOK CARD (CLICKABLE TO OPEN SUBPAGE)
                if (!isSubUser) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showLedgerSubPage = true }
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("FINANCIAL LEDGER BOOK", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                                        Text("Balances, Payments & History SUMMARY", fontSize = 11.sp, color = Color.Gray)
                                    }
                                    Icon(
                                        imageVector = Icons.Default.Payments,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Brief horizontal metrics snapshot
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val briefTotal = ledger?.totalValue ?: 0.0
                                    val briefPaid = ledger?.amountPaid ?: 0.0
                                    val briefDebt = ledger?.pendingDebt ?: 0.0

                                    // Debt Badge/Box
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (briefDebt > 0.0) Color(0xFFFFEBEE) else Color(0xFFE8F5E9)
                                        ),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Column(modifier = Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("DEBT due", fontSize = 8.sp, fontWeight = FontWeight.SemiBold, color = if (briefDebt > 0.0) Color(0xFFC62828) else Color(0xFF2E7D32))
                                            Text(
                                                text = String.format(Locale.getDefault(), "%.2f", briefDebt),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (briefDebt > 0.0) Color(0xFFC62828) else Color(0xFF2E7D32)
                                            )
                                        }
                                    }

                                    // Paid Badge/Box
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Column(modifier = Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("PAID snapshot", fontSize = 8.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF2E7D32))
                                            Text(
                                                text = String.format(Locale.getDefault(), "%.2f", briefPaid),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF2E7D32)
                                            )
                                        }
                                    }

                                    // Total Bills Badge/Box
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Column(modifier = Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("TOTAL billed", fontSize = 8.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
                                            Text(
                                                text = String.format(Locale.getDefault(), "%.2f", briefTotal),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Button(
                                    onClick = { showLedgerSubPage = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    modifier = Modifier.fillMaxWidth(),
                                    contentPadding = PaddingValues(vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.Launch, null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Open Ledger Book & Actions", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

            // Sizing Booklets
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Sizing Card Booklets",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    TextButton(
                        onClick = { showAddMeasurementSheetDialog = true },
                        modifier = Modifier.testTag("add_booklet_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Card", fontSize = 13.sp)
                    }
                }
            }

            if (sheets.isEmpty()) {
                item {
                    Text(
                        text = "No booklets setup for this client. Press 'Add Card' to select rules and save.",
                        fontSize = 13.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            } else {
                items(sheets) { sheet ->
                    val templateName = templates.find { it.id == sheet.templateId }?.name ?: "Bespoke Suit System"
                    val dateStr = SimpleDateFormat("dd MMM, yyyy", Locale.getDefault()).format(Date(sheet.date))
                    
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).testTag("sizing_booklet_card_${sheet.id}")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = templateName,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Created: $dateStr",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Receipt icon button
                                IconButton(
                                    onClick = { activeReceiptMeasurementId = sheet.id },
                                    modifier = Modifier.size(36.dp).testTag("receipt_button_${sheet.id}")
                                ) {
                                    Icon(Icons.Default.ReceiptLong, "View Receipt", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                }

                                // Edit icon button
                                IconButton(
                                    onClick = { activeFormMeasurementId = sheet.id },
                                    modifier = Modifier.size(36.dp).testTag("edit_button_${sheet.id}")
                                ) {
                                    Icon(Icons.Default.Edit, "Edit Sizes", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                                }

                                // Delete icon button
                                if (!isSubUser) {
                                    IconButton(
                                        onClick = { pendingDeleteMeasurementId = sheet.id },
                                        modifier = Modifier.size(36.dp).testTag("delete_button_${sheet.id}")
                                    ) {
                                        Icon(Icons.Default.DeleteOutline, "Delete Booklet", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }


        }
    }
}

    // Dialog Triggers
    if (pendingDeleteMeasurementId != null) {
        AlertDialog(
            onDismissRequest = { pendingDeleteMeasurementId = null },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error)
                    Text("Delete Sizing Card?")
                }
            },
            text = {
                Text(
                    text = "Are you sure you want to delete this sizing card and all its saved measurements? This action cannot be undone."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteMeasurement(pendingDeleteMeasurementId!!)
                        pendingDeleteMeasurementId = null
                        Toast.makeText(context, "Sizing Card Deleted", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Confirm Delete")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { pendingDeleteMeasurementId = null }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showLedgerGuideDialog) {
        AlertDialog(
            onDismissRequest = { showLedgerGuideDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.HelpOutline, null, tint = MaterialTheme.colorScheme.primary)
                    Text("Ledger Guide & Help (کھاتہ رہنمائی)")
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .fillMaxWidth()
                ) {
                    Text(
                        text = "English Instructions:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "1. Every garment stitch order or stock purchase created under this client adds to their accumulated bills.\n" +
                               "2. When a customer pays advance money, or installment cash, click 'Add Cash Receipt' to post payment reducing their debt.\n" +
                               "3. Outstanding balances represents unpaid sewing jobs and material costs in real-time.\n" +
                               "4. Transactions list provides a complete audit scroll of historical work and ledger inputs.",
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                    
                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                    
                    Text(
                        text = "اردو ہدایات (Urdu Help):",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "1. گاہک کا کوئی بھی سلائی آرڈر یا کپڑا/اضافی سامان خریدنے پر بل کی کل رقم گاہک کے کھاتہ میں جمع ہو جائے گی۔\n" +
                               "2. جب گاہک ایڈوانس رقم یا قسط ادا کرے، تو 'ادائیگی رسید' پر کلک کر کے رقم درج کریں تاکہ گاہک کا قرضہ کم ہو سکے۔\n" +
                               "3. بقایا قرضہ گاہک کے ذمے واجب الادا حتمی رقم کو حقیقی وقت میں دکھاتا ہے۔\n" +
                               "4. تمام پچھلی تاریخ کے آرڈرز اور نقد وصولیوں کی تفصیلات نیچے دی گئی لسٹ میں دیکھی جا سکتی ہیں۔",
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showLedgerGuideDialog = false }
                ) {
                    Text("Understood / سمجھ گیا")
                }
            }
        )
    }

    if (activeFormMeasurementId != null) {
        val activeSheet = sheets.find { it.id == activeFormMeasurementId }
        val sheetFields = activeSheet?.let { s -> fields.filter { it.templateId == s.templateId }.distinctBy { it.id } } ?: emptyList()
        val sheetValues = values.filter { it.measurementId == activeFormMeasurementId }
        
        SizeFormDialog(
            customer = customer,
            fields = sheetFields,
            values = sheetValues,
            onDismiss = { activeFormMeasurementId = null },
            onSave = { fieldId, newVal, newSubVal, newNoteVal ->
                onSaveSize(customer.id, activeFormMeasurementId!!, fieldId, newVal, newSubVal, newNoteVal)
            },
            viewModel = viewModel
        )
    }

    if (activeReceiptMeasurementId != null) {
        val activeSheet = sheets.find { it.id == activeReceiptMeasurementId }
        val sheetFields = activeSheet?.let { s -> fields.filter { it.templateId == s.templateId }.distinctBy { it.id } } ?: emptyList()
        val sheetValues = values.filter { it.measurementId == activeReceiptMeasurementId }
        val templateName = templates.find { it.id == activeSheet?.templateId }?.name ?: "Bespoke System"

        ReceiptDialog(
            shopName = shopName,
            shopPhone = shopPhone,
            shopAddress = shopAddress,
            customerName = customer.name,
            customerPhone = customer.phone,
            customerAddress = customer.address,
            sheetTitle = activeSheet?.title ?: "Sizing Layout",
            templateLabel = templateName,
            fields = sheetFields,
            values = sheetValues,
            ledger = ledger,
            lastEditedDate = activeSheet?.updatedAt ?: activeSheet?.date ?: System.currentTimeMillis(),
            proprietorName = currentUser?.displayName ?: "",
            onDismiss = { activeReceiptMeasurementId = null }
        )
    }

    if (showLedgerReceiptDialog) {
        PremiumReceiptDialog(
            shopName = shopName,
            shopPhone = shopPhone,
            shopAddress = shopAddress,
            customerName = customer.name,
            customerPhone = customer.phone,
            customerAddress = customer.address,
            orders = customerOrders,
            payments = customerPayments,
            ledger = ledger,
            proprietorName = currentUser?.displayName ?: "",
            onDismiss = { showLedgerReceiptDialog = false }
        )
    }

    if (clickedTxForReceipt != null) {
        val tx = clickedTxForReceipt!!
        val ordersUpToTx = customerOrders.filter { it.orderDate <= tx.timestamp && !it.isDeleted }
        val paymentsUpToTx = customerPayments.filter { it.paymentDate <= tx.timestamp && !it.isDeleted }
        val totalVal = ordersUpToTx.sumOf { it.price * it.quantity }
        val totalPaid = paymentsUpToTx.sumOf { it.amountPaid }
        val snapshotLedger = LedgerRecord(
            id = ledger?.id ?: "",
            userId = ledger?.userId ?: "",
            customerId = customer.id,
            totalValue = totalVal,
            amountPaid = totalPaid,
            pendingDebt = totalVal - totalPaid,
            updatedAt = tx.timestamp
        )
        PremiumReceiptDialog(
            shopName = shopName,
            shopPhone = shopPhone,
            shopAddress = shopAddress,
            customerName = customer.name,
            customerPhone = customer.phone,
            customerAddress = customer.address,
            orders = ordersUpToTx,
            payments = paymentsUpToTx,
            ledger = snapshotLedger,
            proprietorName = currentUser?.displayName ?: "",
            onDismiss = { clickedTxForReceipt = null }
        )
    }

    if (showLedgerDialog) {
        EditLedgerDialog(
            customer = customer,
            ledger = ledger,
            onDismiss = { showLedgerDialog = false },
            onConfirm = { total, paid ->
                onUpdateLedger(customer.id, total, paid)
                showLedgerDialog = false
            }
        )
    }

    val isUrduMode by viewModel.isUrduSelected.collectAsState()

    if (showLinkMaterialDialog) {
        LinkMaterialDialog(
            inventory = inventory,
            onDismiss = { showLinkMaterialDialog = false },
            onConfirm = { item, quantity ->
                onApplyMaterial(customer.id, item, quantity)
                showLinkMaterialDialog = false
                Toast.makeText(context, "${quantity} stock materials used, ledger billing updated", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showAddMeasurementSheetDialog) {
        AddMeasurementSheetDialog(
            templates = templates,
            fields = fields,
            onDismiss = { showAddMeasurementSheetDialog = false },
            onConfirmWithValues = { selectedTemplateId, title, valuesMap, notesMap ->
                viewModel.insertCustomerMeasurementWithValues(customer.id, selectedTemplateId, title, valuesMap, notesMap)
                showAddMeasurementSheetDialog = false
                Toast.makeText(context, "Sizing Card Booklet and Measurements Saved", Toast.LENGTH_SHORT).show()
            },
            viewModel = viewModel,
            isUrduMode = isUrduMode
        )
    }

    if (showAddPaymentDialog) {
        AddPartialPaymentDialog(
            customer = customer,
            onDismiss = { showAddPaymentDialog = false },
            onConfirm = { amount, note ->
                onAddPartialPayment(customer.id, amount, note)
                showAddPaymentDialog = false
                Toast.makeText(context, "Partial Payment of $${String.format(Locale.getDefault(), "%.2f", amount)} successfully posted", Toast.LENGTH_SHORT).show()
            },
            viewModel = viewModel,
            isUrduMode = isUrduMode
        )
    }

    if (showAddOrderDialog) {
        AddOrderDialog(
            customer = customer,
            inventory = inventory,
            onDismiss = { showAddOrderDialog = false },
            onConfirm = { name, price, advance, note, invItemId, quantity ->
                onCreateOrder(customer.id, name, price, advance, note, invItemId, quantity)
                showAddOrderDialog = false
                val completionMsg = if (advance >= price) "Order completed!" else "Order posted with partial payment."
                Toast.makeText(context, "Order '$name' recorded. $completionMsg", Toast.LENGTH_SHORT).show()
            },
            viewModel = viewModel,
            isUrduMode = isUrduMode
        )
    }

    if (showEditCustomerDialog) {
        var editName by remember { mutableStateOf(customer.name) }
        var editPhone by remember { mutableStateOf(customer.phone) }
        var editAddress by remember { mutableStateOf(customer.address) }

        AlertDialog(
            onDismissRequest = { showEditCustomerDialog = false },
            title = { Text("Edit Customer Information") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Full Name") },
                        modifier = Modifier.fillMaxWidth().testTag("edit_customer_name"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = editPhone,
                        onValueChange = { editPhone = it },
                        label = { Text("Phone Number") },
                        modifier = Modifier.fillMaxWidth().testTag("edit_customer_phone"),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                    )
                    OutlinedTextField(
                        value = editAddress,
                        onValueChange = { editAddress = it },
                        label = { Text("Address / Locality") },
                        modifier = Modifier.fillMaxWidth().testTag("edit_customer_address"),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editName.isBlank()) {
                            Toast.makeText(context, "Name cannot be empty", Toast.LENGTH_SHORT).show()
                        } else {
                            val updated = customer.copy(name = editName, phone = editPhone, address = editAddress)
                            viewModel.updateCustomer(updated)
                            viewModel.selectedCustomerForDetails.value = updated
                            showEditCustomerDialog = false
                            Toast.makeText(context, "Customer information updated", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.testTag("save_customer_edit_btn")
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditCustomerDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDeleteCustomerConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteCustomerConfirmDialog = false },
            title = { Text("Confirm Deletion", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete this customer? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteCustomerConfirmDialog = false
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteCustomerConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// ---------------- PRINT CUSTOM RECEIPT DIALOG ----------------

@Composable
fun ReceiptDialog(
    shopName: String,
    shopPhone: String,
    shopAddress: String,
    customerName: String,
    customerPhone: String,
    customerAddress: String,
    sheetTitle: String,
    templateLabel: String,
    fields: List<MeasurementField>,
    values: List<MeasurementValue>,
    ledger: LedgerRecord? = null,
    lastEditedDate: Long = System.currentTimeMillis(),
    proprietorName: String = "",
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current

    val receiptText = remember(shopName, shopPhone, shopAddress, customerName, customerPhone, sheetTitle, fields, values, ledger, lastEditedDate) {
        val sb = StringBuilder()
        sb.append("====================================\n")
        sb.append("   ${shopName.uppercase(Locale.getDefault())}\n")
        sb.append("====================================\n")
        sb.append("Phone  : $shopPhone\n")
        sb.append("Address: $shopAddress\n")
        sb.append("------------------------------------\n")
        sb.append("CLIENT : ${customerName.uppercase(Locale.getDefault())}\n")
        sb.append("Phone  : $customerPhone\n")
        sb.append("Address: $customerAddress\n")
        sb.append("------------------------------------\n")
        sb.append("BOOKLET: ${sheetTitle.uppercase(Locale.getDefault())}\n")
        sb.append("STYLE  : ${templateLabel.uppercase(Locale.getDefault())}\n")
        sb.append("DATE   : ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(lastEditedDate))}\n")
        sb.append("------------------------------------\n")
        sb.append("SIZING DETAILS (2-COLUMN COMPACT)\n")
        sb.append("------------------------------------\n")
        val valMap = values.associateBy { it.fieldId }
        val linesList = fields.chunked(2)
        for (pair in linesList) {
            val cell1 = pair.getOrNull(0)
            val val1 = cell1?.let {
                val v = valMap[it.id]
                if (v != null) {
                    if (v.value.isNotBlank() && v.subValue.isNotBlank()) "${v.value} (${v.subValue})"
                    else v.value.ifBlank { v.subValue }.ifBlank { "—" }
                } else "—"
            } ?: ""
            val str1 = cell1?.let { "${it.label}:$val1" } ?: ""
            
            val cell2 = pair.getOrNull(1)
            val val2 = cell2?.let {
                val v = valMap[it.id]
                if (v != null) {
                    if (v.value.isNotBlank() && v.subValue.isNotBlank()) "${v.value} (${v.subValue})"
                    else v.value.ifBlank { v.subValue }.ifBlank { "—" }
                } else "—"
            } ?: ""
            val str2 = cell2?.let { "${it.label}:$val2" } ?: ""
            
            sb.append(String.format(Locale.getDefault(), "%-17s | %-17s\n", str1, str2))
        }
        val globalNoteStr = valMap["global_notes_field"]?.value ?: ""
        if (globalNoteStr.isNotBlank()) {
            sb.append("------------------------------------\n")
            sb.append("ADDITIONAL NOTES:\n")
            sb.append("$globalNoteStr\n")
        }
        if (ledger != null) {
            sb.append("------------------------------------\n")
            sb.append("      FINANCIAL LEDGER SNAPSHOT     \n")
            sb.append("------------------------------------\n")
            sb.append(String.format(Locale.getDefault(), "TOTAL BILLED: %.2f\n", ledger.totalValue))
            sb.append(String.format(Locale.getDefault(), "TOTAL PAID  : %.2f\n", ledger.amountPaid))
            sb.append(String.format(Locale.getDefault(), "DEBT DUE    : %.2f\n", ledger.pendingDebt))
        }
        sb.append("====================================\n")
        sb.append("Generated with Tailor Workspace App\n")
        sb.toString()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // PREMIUM TOP HEADER BAR WITH CLOSE ICON
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Sizing Ticket Receipt",
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("receipt_close_top")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Dialogue",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // SCROLLABLE CONTAINER FOR TICKET SHEET (TAKES MAX SCROLLABLE HEIGHT)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    // HIGH-FIDELITY TICKET WRAPPED IN CRUNCHY RETRO DESIGN
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFCFAF2)),
                        border = BorderStroke(1.2.dp, Color(0xFFE5DECE))
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Section 1: Shop details left-aligned & Card Info right-aligned as requested
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(
                                    modifier = Modifier.weight(1.1f),
                                    verticalArrangement = Arrangement.spacedBy(1.dp)
                                ) {
                                    Text(
                                        text = shopName.ifEmpty { "Tailor Atelier" }.uppercase(Locale.getDefault()),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF8B6B3F)
                                    )
                                    val pName = if (proprietorName.isNotBlank()) proprietorName else (shopName.ifEmpty { "Tailor Master" })
                                    Text(
                                        text = "Proprietor: ${pName.uppercase(Locale.getDefault())}",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF5E584E)
                                    )
                                    Text(
                                        text = "Address: ${shopAddress.ifEmpty { "Main Atelier District" }}",
                                        color = Color(0xFF726B5F),
                                        fontSize = 11.sp
                                    )
                                    Text(
                                        text = "Phone: ${shopPhone.ifEmpty { "+00 000 000" }}",
                                        color = Color(0xFF726B5F),
                                        fontSize = 11.sp
                                    )
                                }

                                Column(
                                    modifier = Modifier.weight(0.9f),
                                    horizontalAlignment = Alignment.End,
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = "TEMPLATE: ${templateLabel.uppercase(Locale.getDefault())}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1B1A18),
                                        textAlign = TextAlign.End
                                    )
                                    Text(
                                        text = "DATE: ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(lastEditedDate))}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1B1A18),
                                        textAlign = TextAlign.End
                                    )
                                }
                            }

                            // Ribbon Row Spacer
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(Color(0xFF8B6B3F))
                            ) {}

                            // Section 2: Billing customer credentials
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(1.dp)
                            ) {
                                Text(
                                    text = "BILL TO CUSTOMER:",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF8B6B3F),
                                    fontSize = 8.sp
                                )
                                Text(
                                    text = customerName.uppercase(Locale.getDefault()),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1B1A18)
                                )
                                if (customerAddress.isNotEmpty()) {
                                    Text(
                                        text = "Addr: $customerAddress",
                                        fontSize = 8.sp,
                                        color = Color(0xFF3D3A37)
                                    )
                                }
                                Text(
                                    text = "Phone: $customerPhone",
                                    fontSize = 8.sp,
                                    color = Color(0xFF5E584E)
                                )
                            }

                            // Primary Sizing Grid (Two side-by-side columns: Measurements vs Demands!)
                            HorizontalDivider(thickness = 1.dp, color = Color(0xFFE5DECE))

                            val valMap = values.associateBy { it.fieldId }
                            val mFields = fields.filter { !it.isDemand }.distinctBy { it.id }
                            val dFields = fields.filter { it.isDemand }.distinctBy { it.id }

                            Row(
                                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // LEFT SIDE: MEASUREMENTS COLUMN
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFFF4F0E4))
                                        .padding(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(Icons.Default.SquareFoot, null, tint = Color(0xFF8B6B3F), modifier = Modifier.size(12.dp))
                                        Text("MEASUREMENTS", fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF8B6B3F))
                                    }
                                    HorizontalDivider(thickness = 1.dp, color = Color(0xFFE5DECE))

                                    if (mFields.isEmpty()) {
                                        Text("None added", fontSize = 10.sp, color = Color.Gray, modifier = Modifier.padding(top = 4.dp))
                                    } else {
                                        mFields.forEach { f ->
                                            val v = valMap[f.id]
                                            val valueText = if (v != null) {
                                                if (v.value.isNotBlank() && v.subValue.isNotBlank()) "${v.value} (${v.subValue})"
                                                else v.value.ifBlank { v.subValue }.ifBlank { "—" }
                                            } else "—"
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                             ) {
                                                Text(f.label, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF3D3A37), modifier = Modifier.weight(1.3f))
                                                Text(" .........", fontSize = 8.sp, color = Color(0xFFC7BEB0).copy(alpha = 0.7f), modifier = Modifier.weight(0.4f), maxLines = 1)
                                                Text(valueText, fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color(0xFF8B6B3F), modifier = Modifier.weight(1.3f), textAlign = TextAlign.End)
                                            }
                                        }
                                    }
                                }

                                // VERTICAL DIVIDER IN THE MIDDLE (DASHED STYLING EFFECT)
                                Box(
                                    modifier = Modifier
                                        .width(1.dp)
                                        .fillMaxHeight()
                                        .background(Color(0xFFE5DECE))
                                )

                                // RIGHT SIDE: DESIGN STYLE DEMANDS COLUMN
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFFEEEAE0))
                                        .padding(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(Icons.Default.DesignServices, null, tint = Color(0xFF715F47), modifier = Modifier.size(12.dp))
                                        Text("STYLE DEMANDS", fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF715F47))
                                    }
                                    HorizontalDivider(thickness = 1.dp, color = Color(0xFFE5DECE))

                                    if (dFields.isEmpty()) {
                                        Text("None added", fontSize = 10.sp, color = Color.Gray, modifier = Modifier.padding(top = 4.dp))
                                    } else {
                                        dFields.forEach { f ->
                                            val v = valMap[f.id]
                                            val valueText = if (v != null) {
                                                if (v.value.isNotBlank() && v.subValue.isNotBlank()) "${v.value} (${v.subValue})"
                                                else v.value.ifBlank { v.subValue }.ifBlank { "—" }
                                            } else "—"
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                             ) {
                                                Text(f.label, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF3D3A37), modifier = Modifier.weight(1.3f))
                                                Text(" .........", fontSize = 8.sp, color = Color(0xFFC7BEB0).copy(alpha = 0.7f), modifier = Modifier.weight(0.4f), maxLines = 1)
                                                Text(valueText, fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color(0xFF715F47), modifier = Modifier.weight(1.3f), textAlign = TextAlign.End)
                                            }
                                        }
                                    }
                                }
                            }

                            val globalNote = valMap["global_notes_field"]?.value ?: ""
                            if (globalNote.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .border(1.dp, Color(0xFFE5DECE), RoundedCornerShape(6.dp))
                                        .background(Color(0xFFFCFAF2))
                                        .padding(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "Additional Notes:",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF8B6B3F)
                                    )
                                    Text(
                                        text = globalNote,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Normal,
                                        color = Color(0xFF3C3C3C),
                                        lineHeight = 12.sp
                                    )
                                }
                            }

                            HorizontalDivider(thickness = 1.dp, color = Color(0xFFE5DECE))

                            // Compact signature card footer
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1.3f)) {
                                    Text("TICKET POLICY & DISCLAIMER", fontSize = 8.sp, fontWeight = FontWeight.Black, color = Color(0xFF5E584E))
                                    Text("These master specs are registered under tailor custody. For subsequent altering, please refer to the original copy.", fontSize = 8.sp, color = Color(0xFF726B5F), lineHeight = 10.sp)
                                }
                                Column(modifier = Modifier.weight(0.7f), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(modifier = Modifier.width(50.dp).height(1.dp).background(Color(0xFF726B5F)))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Master Signature", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color(0xFF726B5F))
                                }
                            }
                        }
                    }
                }

                // FIXED STICKY PREMIUM ACTIONS ROW WITH LARGER ICONS & COMFIER PADDING
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(receiptText))
                            Toast.makeText(context, "Sizing Card Copied to Clipboard", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            com.example.util.ReceiptGenerator.shareBespokeMeasurementReceipt(
                                context = context,
                                shopName = shopName,
                                shopPhone = shopPhone,
                                shopAddress = shopAddress,
                                customerName = customerName,
                                customerPhone = customerPhone,
                                customerAddress = customerAddress,
                                sheetTitle = sheetTitle,
                                templateLabel = templateLabel,
                                fields = fields,
                                values = values,
                                lastEditedDate = lastEditedDate
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Share, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            val receiptBitmap = com.example.util.ReceiptGenerator.generateMeasurementReceiptBitmap(
                                context = context,
                                shopName = shopName,
                                shopPhone = shopPhone,
                                shopAddress = shopAddress,
                                customerName = customerName,
                                customerPhone = customerPhone,
                                customerAddress = customerAddress,
                                sheetTitle = sheetTitle,
                                templateLabel = templateLabel,
                                fields = fields,
                                values = values,
                                lastEditedDate = lastEditedDate
                            )
                            doPrintReceipt(context, receiptBitmap, sheetTitle.ifEmpty { "Sizing_Ticket" })
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Print, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Print", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(42.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    Text("Close Window", fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
fun PremiumReceiptDialog(
    shopName: String,
    shopPhone: String,
    shopAddress: String,
    customerName: String,
    customerPhone: String,
    customerAddress: String,
    orders: List<OrderRecord>,
    payments: List<PaymentRecord>,
    ledger: LedgerRecord?,
    proprietorName: String = "",
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current

    var filterType by remember { mutableStateOf("All") } // "All", "7Days", "30Days", "Custom"
    var customStartDate by remember { mutableStateOf<Long?>(null) }
    var customEndDate by remember { mutableStateOf<Long?>(null) }

    val now = System.currentTimeMillis()
    val startLimit = remember(filterType, customStartDate, now) {
        when (filterType) {
            "7Days" -> now - 7L * 24 * 60 * 60 * 1000
            "30Days" -> now - 30L * 24 * 60 * 60 * 1000
            "Custom" -> customStartDate ?: 0L
            else -> 0L
        }
    }
    val endLimit = remember(filterType, customEndDate) {
        when (filterType) {
            "Custom" -> customEndDate ?: Long.MAX_VALUE
            else -> Long.MAX_VALUE
        }
    }

    val filteredOrders = remember(orders, startLimit, endLimit) {
        orders.filter { it.orderDate in startLimit..endLimit }
    }
    val filteredPayments = remember(payments, startLimit, endLimit) {
        payments.filter { it.paymentDate in startLimit..endLimit }
    }

    val showStartDatePicker = {
        val calendar = java.util.Calendar.getInstance()
        customStartDate?.let { calendar.timeInMillis = it }
        android.app.DatePickerDialog(
            context,
            { _, year, month, day ->
                val cal = java.util.Calendar.getInstance().apply {
                    set(java.util.Calendar.YEAR, year)
                    set(java.util.Calendar.MONTH, month)
                    set(java.util.Calendar.DAY_OF_MONTH, day)
                    set(java.util.Calendar.HOUR_OF_DAY, 0)
                    set(java.util.Calendar.MINUTE, 0)
                    set(java.util.Calendar.SECOND, 0)
                    set(java.util.Calendar.MILLISECOND, 0)
                }
                customStartDate = cal.timeInMillis
            },
            calendar.get(java.util.Calendar.YEAR),
            calendar.get(java.util.Calendar.MONTH),
            calendar.get(java.util.Calendar.DAY_OF_MONTH)
        ).show()
    }

    val showEndDatePicker = {
        val calendar = java.util.Calendar.getInstance()
        customEndDate?.let { calendar.timeInMillis = it }
        android.app.DatePickerDialog(
            context,
            { _, year, month, day ->
                val cal = java.util.Calendar.getInstance().apply {
                    set(java.util.Calendar.YEAR, year)
                    set(java.util.Calendar.MONTH, month)
                    set(java.util.Calendar.DAY_OF_MONTH, day)
                    set(java.util.Calendar.HOUR_OF_DAY, 23)
                    set(java.util.Calendar.MINUTE, 59)
                    set(java.util.Calendar.SECOND, 59)
                    set(java.util.Calendar.MILLISECOND, 999)
                }
                customEndDate = cal.timeInMillis
            },
            calendar.get(java.util.Calendar.YEAR),
            calendar.get(java.util.Calendar.MONTH),
            calendar.get(java.util.Calendar.DAY_OF_MONTH)
        ).show()
    }

    val receiptText = remember(shopName, shopPhone, shopAddress, customerName, customerPhone, customerAddress, filteredOrders, filteredPayments, ledger, filterType, customStartDate, customEndDate) {
        val sb = StringBuilder()
        sb.append("====================================\n")
        sb.append("   ${shopName.uppercase(Locale.getDefault())}\n")
        sb.append("   STATEMENT & LEDGER RECEIPT\n")
        sb.append("====================================\n")
        sb.append("Phone  : $shopPhone\n")
        sb.append("Address: $shopAddress\n")
        sb.append("------------------------------------\n")
        sb.append("CLIENT : ${customerName.uppercase(Locale.getDefault())}\n")
        sb.append("Phone  : $customerPhone\n")
        sb.append("Address: $customerAddress\n")
        sb.append("Date   : ${SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())}\n")
        sb.append("Filter : $filterType")
        if (filterType == "Custom") {
            val startStr = customStartDate?.let { SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(Date(it)) } ?: "Beginning"
            val endStr = customEndDate?.let { SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(Date(it)) } ?: "End"
            sb.append(" ($startStr to $endStr)")
        }
        sb.append("\n------------------------------------\n")
        
        if (filteredOrders.isNotEmpty()) {
            sb.append("ITEMS & ORDERS CHRONOLOGY:\n")
            filteredOrders.forEach { ord ->
                val dateStr = SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(Date(ord.orderDate))
                val nameFormatted = if (ord.itemName.length > 20) ord.itemName.substring(0, 17) + "..." else ord.itemName
                sb.append(String.format(Locale.getDefault(), "%-8s %-18s %-8.2f\n", dateStr, nameFormatted, ord.price))
            }
            sb.append("------------------------------------\n")
        }

        if (filteredPayments.isNotEmpty()) {
            sb.append("PAYMENT HISTORY CREDITS:\n")
            filteredPayments.forEach { pay ->
                val dateStr = SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(Date(pay.paymentDate))
                val noteStr = if (pay.note.isNotEmpty()) {
                    if (pay.note.length > 18) pay.note.substring(0, 15) + "..." else pay.note
                } else "Partial Payment"
                sb.append(String.format(Locale.getDefault(), "%-8s %-18s +%-7.2f\n", dateStr, noteStr, pay.amountPaid))
            }
            sb.append("------------------------------------\n")
        }

        val totalOrdersFiltered = filteredOrders.sumOf { it.price * it.quantity }
        val totalPaidFiltered = filteredPayments.sumOf { it.amountPaid }
        val periodNet = totalOrdersFiltered - totalPaidFiltered

        sb.append("SELECTED PERIOD SUMMARY:\n")
        sb.append(String.format(Locale.getDefault(), "PERIOD ORDER VALUE: %-12.2f\n", totalOrdersFiltered))
        sb.append(String.format(Locale.getDefault(), "PERIOD AMOUNT PAID : %-12.2f\n", totalPaidFiltered))
        sb.append(String.format(Locale.getDefault(), "PERIOD NET BALANCE : %-12.2f\n", periodNet))
        sb.append("------------------------------------\n")
        sb.append("LEDGER HISTORY OVERALL INTERESTS:\n")
        sb.append(String.format(Locale.getDefault(), "GLOBAL TOTAL BILL   : %-12.2f\n", ledger?.totalValue ?: 0.0))
        sb.append(String.format(Locale.getDefault(), "GLOBAL TOTAL PAID   : %-12.2f\n", ledger?.amountPaid ?: 0.0))
        sb.append("------------------------------------\n")
        sb.append(String.format(Locale.getDefault(), "CURRENT DEBT DUE    : %-12.2f\n", ledger?.pendingDebt ?: 0.0))
        sb.append("====================================\n")
        sb.append("Generated with Tailor Workspace App\n")
        sb.toString()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // PREMIUM TOP HEADER BAR WITH CLOSE ICON
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Receipt & Ledger Statement",
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("ledger_receipt_close_top")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Ledger Dialogue",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // SCROLLABLE BODY WRAPPER TO MAKE SURE THE RECEIPT CAN BE SCROLLED BUT BUTTONS ARE FIXED
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        // Date Filtering Presets Selector Card
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFFCFAF2))
                                .border(1.2.dp, Color(0xFFE5DECE), RoundedCornerShape(8.dp))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Receipt Date Filtering",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color(0xFF8B6B3F)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                val options = listOf(
                                    "All" to "All Time",
                                    "7Days" to "7 Days",
                                    "30Days" to "30 Days",
                                    "Custom" to "Custom"
                                )
                                options.forEach { (type, label) ->
                                    val isSelected = filterType == type
                                    val bgColor = if (isSelected) Color(0xFF8B6B3F) else Color(0xFFF4F0E4)
                                    val textColor = if (isSelected) Color.White else Color(0xFF5E584E)
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(bgColor)
                                            .clickable { filterType = type }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = textColor)
                                    }
                                }
                            }

                            if (filterType == "Custom") {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val startText = customStartDate?.let { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(it)) } ?: "Start Date"
                                    val endText = customEndDate?.let { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(it)) } ?: "End Date"

                                    Button(
                                        onClick = showStartDatePicker,
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEEEAE0), contentColor = Color(0xFF1B1A18)),
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                                    ) {
                                        Icon(Icons.Default.CalendarToday, null, tint = Color(0xFF8B6B3F), modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(startText, fontSize = 11.sp, maxLines = 1)
                                    }

                                    Button(
                                        onClick = showEndDatePicker,
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEEEAE0), contentColor = Color(0xFF1B1A18)),
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                                    ) {
                                        Icon(Icons.Default.CalendarToday, null, tint = Color(0xFF8B6B3F), modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(endText, fontSize = 11.sp, maxLines = 1)
                                    }
                                }
                            }
                        }

                        // HIGH-FIDELITY VISUAL RECEIPT CARD
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFCFAF2)),
                            border = BorderStroke(1.2.dp, Color(0xFFE5DECE))
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                 // Shop Header Info block
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column(
                                        modifier = Modifier.weight(1.2f),
                                        verticalArrangement = Arrangement.spacedBy(1.dp)
                                    ) {
                                        Text(
                                            text = shopName.ifEmpty { "Tailor Atelier" }.uppercase(Locale.getDefault()),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFF8B6B3F)
                                        )
                                        val pName = if (proprietorName.isNotBlank()) proprietorName else (shopName.ifEmpty { "Tailor Master" })
                                        Text(
                                            text = "Proprietor: ${pName.uppercase(Locale.getDefault())}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF5E584E),
                                            fontSize = 11.sp
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Address: ${shopAddress.ifEmpty { "Main Atelier District" }}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color(0xFF726B5F),
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            text = "Phone: ${shopPhone.ifEmpty { "+00 000 000 0" }}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color(0xFF726B5F),
                                            fontSize = 13.sp
                                        )
                                    }
                                    Column(
                                        modifier = Modifier.weight(0.8f),
                                        horizontalAlignment = Alignment.End
                                    ) {
                                        Text(
                                            text = "No: TX-${System.currentTimeMillis().toString().takeLast(6)}",
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp,
                                            color = Color(0xFF726B5F)
                                        )
                                    }
                                }

                                // Colored Aesthetic Thick Header bar
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(1.2.dp)
                                        .background(Color(0xFF8B6B3F))
                                ) {}

                                // Client Meta Information
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1.2f)) {
                                        Text("BILLED TO:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = Color(0xFF8B6B3F), fontSize = 11.sp)
                                        Text(customerName.uppercase(Locale.getDefault()), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF1B1A18))
                                        Text("Phone: $customerPhone", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF5E584E))
                                        if (customerAddress.isNotEmpty()) {
                                            Text("Address: $customerAddress", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF3D3A37))
                                        }
                                    }
                                    Column(modifier = Modifier.weight(0.8f), horizontalAlignment = Alignment.End) {
                                        Text("STATEMENT DATE:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = Color(0xFF8B6B3F), fontSize = 11.sp)
                                        Text(SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date()), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color(0xFF1B1A18))
                                        Spacer(modifier = Modifier.height(6.dp))

                                        val pendingDue = ledger?.pendingDebt ?: 0.0
                                        val statusLabel = if (pendingDue > 0.0) "DEBT OUTSTANDING" else "ALL PAID"
                                        val statusBg = if (pendingDue > 0.0) Color(0xFFFDF2F2) else Color(0xFFF0FDF4)
                                        val statusBorderColor = if (pendingDue > 0.0) Color(0xFFF3C8C8) else Color(0xFFBBF7D0)
                                        val statusColor = if (pendingDue > 0.0) Color(0xFFC62828) else Color(0xFF15803D)

                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(statusBg)
                                                .border(1.dp, statusBorderColor, RoundedCornerShape(4.dp))
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(statusLabel, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = statusColor)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                // Transaction Ledger Table Grid
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    // Table Header
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFFF4F0E4))
                                            .padding(vertical = 6.dp, horizontal = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("DESCRIPTION", modifier = Modifier.weight(1.8f), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF8B6B3F))
                                        Text("QTY", modifier = Modifier.weight(0.4f), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF8B6B3F), textAlign = TextAlign.Center)
                                        Text("PRICE", modifier = Modifier.weight(0.8f), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF8B6B3F), textAlign = TextAlign.End)
                                        Text("TOTAL", modifier = Modifier.weight(1.0f), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF8B6B3F), textAlign = TextAlign.End)
                                    }

                                    HorizontalDivider(thickness = 1.dp, color = Color(0xFFE5DECE))

                                    // Itemized lists
                                    if (filteredOrders.isEmpty() && filteredPayments.isEmpty()) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 16.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("No ledger records in selected range.", fontSize = 13.sp, color = Color(0xFF726B5F))
                                        }
                                    } else {
                                        filteredOrders.forEach { ord ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 6.dp, vertical = 3.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(ord.itemName, modifier = Modifier.weight(1.8f), fontSize = 13.sp, maxLines = 2, color = Color(0xFF1B1A18))
                                                Text(ord.quantity.toString(), modifier = Modifier.weight(0.4f), fontSize = 13.sp, textAlign = TextAlign.Center, color = Color(0xFF1B1A18))
                                                Text(String.format(Locale.getDefault(), "%.2f", ord.price), modifier = Modifier.weight(0.8f), fontSize = 13.sp, textAlign = TextAlign.End, color = Color(0xFF1B1A18))
                                                Text(String.format(Locale.getDefault(), "%.2f", ord.price * ord.quantity), modifier = Modifier.weight(1.0f), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End, color = Color(0xFF1B1A18))
                                            }
                                        }

                                        filteredPayments.forEach { pay ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 6.dp, vertical = 3.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                val memo = if (pay.note.isNotEmpty()) "Credit: ${pay.note}" else "Payment Credit"
                                                Text(memo, modifier = Modifier.weight(1.8f), fontSize = 13.sp, color = Color(0xFF15803D), maxLines = 2)
                                                Text("1", modifier = Modifier.weight(0.4f), fontSize = 13.sp, textAlign = TextAlign.Center, color = Color(0xFF15803D))
                                                Text(String.format(Locale.getDefault(), "-%.2f", pay.amountPaid), modifier = Modifier.weight(0.8f), fontSize = 13.sp, textAlign = TextAlign.End, color = Color(0xFF15803D))
                                                Text(String.format(Locale.getDefault(), "-%.2f", pay.amountPaid), modifier = Modifier.weight(1.0f), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End, color = Color(0xFF15803D))
                                            }
                                        }
                                    }

                                    HorizontalDivider(thickness = 1.dp, color = Color(0xFFE5DECE))

                                    // Selected Period summaries
                                    val totalOrdersSelected = filteredOrders.sumOf { it.price * it.quantity }
                                    val totalPaidSelected = filteredPayments.sumOf { it.amountPaid }
                                    val netSelected = totalOrdersSelected - totalPaidSelected

                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 6.dp, vertical = 3.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Selected Period Charges Total:", fontSize = 12.sp, color = Color(0xFF726B5F))
                                            Text(String.format(Locale.getDefault(), "%.2f", totalOrdersSelected), fontSize = 12.sp, color = Color(0xFF1B1A18))
                                        }
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Selected Period Credits Applied:", fontSize = 12.sp, color = Color(0xFF15803D))
                                            Text(String.format(Locale.getDefault(), "-%.2f", totalPaidSelected), fontSize = 12.sp, color = Color(0xFF15803D))
                                        }
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Selected Period Net Balance due:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1B1A18))
                                            Text(String.format(Locale.getDefault(), "%.2f", netSelected), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1B1A18))
                                        }
                                    }
                                }

                                // Big Footer Prominent Total block matching Pakistan invoice
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFF8B6B3F))
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1.4f)) {
                                        Text("LEDGER SUMMARY TERMS", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.White)
                                        Text("Official transaction balance record ledger statement.", fontSize = 11.sp, color = Color.White.copy(alpha = 0.85f), lineHeight = 13.sp)
                                    }
                                    Column(modifier = Modifier.weight(0.6f), horizontalAlignment = Alignment.End) {
                                        Text("TOTAL DUE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.End)
                                        Text(
                                            text = String.format(Locale.getDefault(), "%.2f", ledger?.pendingDebt ?: 0.0),
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.White,
                                            textAlign = TextAlign.End
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // FIXED STICKY ACTIONS ROW
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(receiptText))
                            Toast.makeText(context, "Ledger Statement Copied", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            com.example.util.ReceiptGenerator.shareBespokeLedgerReceipt(
                                context = context,
                                shopName = shopName,
                                shopPhone = shopPhone,
                                shopAddress = shopAddress,
                                customerName = customerName,
                                customerPhone = customerPhone,
                                customerAddress = customerAddress,
                                filteredOrders = filteredOrders,
                                filteredPayments = filteredPayments,
                                ledger = ledger,
                                filterType = filterType,
                                customStartDate = customStartDate,
                                customEndDate = customEndDate,
                                isWhatsAppDirect = true
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Share, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            val receiptBitmap = com.example.util.ReceiptGenerator.generateLedgerReceiptBitmap(
                                context = context,
                                shopName = shopName,
                                shopPhone = shopPhone,
                                shopAddress = shopAddress,
                                customerName = customerName,
                                customerPhone = customerPhone,
                                customerAddress = customerAddress,
                                filteredOrders = filteredOrders,
                                filteredPayments = filteredPayments,
                                ledger = ledger,
                                filterType = filterType,
                                customStartDate = customStartDate,
                                customEndDate = customEndDate
                            )
                            doPrintReceipt(context, receiptBitmap, "Ledger_Statement")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Print, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Print", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(42.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    Text("Close Statement", fontSize = 14.sp)
                }
            }
        }
    }
}

// ---------------- INVENTORY DISPLAY TAB ----------------

@Composable
fun InventoryTab(
    inventory: List<InventoryItem>,
    onAddItem: (String, String, Double, Double, Int) -> Unit,
    onDeleteItem: (InventoryItem) -> Unit,
    onUpdateItem: (InventoryItem) -> Unit = {},
    isSubUser: Boolean = false
) {
    val context = LocalContext.current
    var itemNeedEdit by remember { mutableStateOf<InventoryItem?>(null) }
    var itemToPendingDelete by remember { mutableStateOf<InventoryItem?>(null) }

    if (inventory.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.Inventory,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Empty Tailor Supplies",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Track threads, cloth rolls, button meters, and sewing materials to compute profit sheets.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        val totalProfit = inventory.sumOf { it.totalPotentialProfit }
        val totalCost = inventory.sumOf { it.purchasePrice * it.stockQuantity }
        val totalRevenue = inventory.sumOf { it.sellingPrice * it.stockQuantity }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Stock Statistics Summary", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Potential Profit: ${String.format(Locale.getDefault(), "%.2f", totalProfit)}",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 22.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Total Stock Purchase Cost: ${String.format(Locale.getDefault(), "%.2f", totalCost)}", style = MaterialTheme.typography.bodyMedium)
                        Text("Expected Total Income: ${String.format(Locale.getDefault(), "%.2f", totalRevenue)}", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            items(inventory, key = { it.id }) { item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("inventory_card_${item.name.lowercase().replace(" ", "_")}"),
                    border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Standard Unit: ${item.unitType}", fontSize = 13.sp, color = Color.Gray)
                            Text("Cost: ${item.purchasePrice} | Selling Price: ${item.sellingPrice}", fontSize = 13.sp)
                            Text(
                                "Profit/Unit: ${String.format(Locale.getDefault(), "%.2f", item.profitPerUnit)}", 
                                color = Color(0xFF386A20),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (item.stockQuantity <= 3) Color(0xFFFDE8E8) else Color(0xFFE8F0E5)
                                    )
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "Stock: ${item.stockQuantity}",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 14.sp,
                                    color = if (item.stockQuantity <= 3) Color(0xFFBA1A1A) else Color(0xFF386A20)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (!isSubUser) {
                                    IconButton(onClick = { itemNeedEdit = item }) {
                                        Icon(Icons.Default.Edit, "Edit stock material", tint = MaterialTheme.colorScheme.primary)
                                    }
                                    IconButton(onClick = { itemToPendingDelete = item }) {
                                        Icon(Icons.Default.Delete, "Remove stock material", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (itemNeedEdit != null) {
        val original = itemNeedEdit!!
        var editName by remember { mutableStateOf(original.name) }
        var editUnitType by remember { mutableStateOf(original.unitType) }
        var editPurchasePrice by remember { mutableStateOf(original.purchasePrice.toString()) }
        var editSellingPrice by remember { mutableStateOf(original.sellingPrice.toString()) }
        var editStock by remember { mutableStateOf(original.stockQuantity.toString()) }

        AlertDialog(
            onDismissRequest = { itemNeedEdit = null },
            title = { Text("Edit Inventory Item") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Item Name") },
                        modifier = Modifier.fillMaxWidth().testTag("edit_inventory_name"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = editUnitType,
                        onValueChange = { editUnitType = it },
                        label = { Text("Unit / Measure Unit") },
                        modifier = Modifier.fillMaxWidth().testTag("edit_inventory_unit"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = editPurchasePrice,
                        onValueChange = { editPurchasePrice = it },
                        label = { Text("Purchase Price") },
                        modifier = Modifier.fillMaxWidth().testTag("edit_inventory_purchase_price"),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = editSellingPrice,
                        onValueChange = { editSellingPrice = it },
                        label = { Text("Selling Price") },
                        modifier = Modifier.fillMaxWidth().testTag("edit_inventory_selling_price"),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = editStock,
                        onValueChange = { editStock = it },
                        label = { Text("Stock Quantity") },
                        modifier = Modifier.fillMaxWidth().testTag("edit_inventory_stock"),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val buy = editPurchasePrice.toDoubleOrNull() ?: 0.0
                        val sell = editSellingPrice.toDoubleOrNull() ?: 0.0
                        val stock = editStock.toIntOrNull() ?: 0
                        if (editName.isBlank()) {
                            Toast.makeText(context, "Item Name is required", Toast.LENGTH_SHORT).show()
                        } else {
                            onUpdateItem(
                                original.copy(
                                    name = editName,
                                    unitType = editUnitType,
                                    purchasePrice = buy,
                                    sellingPrice = sell,
                                    stockQuantity = stock
                                )
                            )
                            itemNeedEdit = null
                            Toast.makeText(context, "Inventory Item updated", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.testTag("save_inventory_edit_btn")
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { itemNeedEdit = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (itemToPendingDelete != null) {
        val original = itemToPendingDelete!!
        AlertDialog(
            onDismissRequest = { itemToPendingDelete = null },
            title = { Text("Delete Inventory Item?", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete '${original.name}' from your catalog?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteItem(original)
                        itemToPendingDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToPendingDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// ---------------- SIZING TEMPLATE MAESTRO TAB ----------------

@Composable
fun TemplateTab(
    templates: List<SizingTemplate>,
    fields: List<MeasurementField>,
    onCreateTemplate: (String) -> Unit,
    onDeleteTemplate: (String) -> Unit,
    onAddField: (String, String, String, String, Boolean, Boolean, String, String, String, String) -> Unit,
    onDeleteField: (String) -> Unit,
    viewModel: TailorViewModel,
    onEditTemplate: (String) -> Unit
) {
    val context = LocalContext.current
    var showTemplateGuideDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Top Header Row with Title and Guide Button
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Dynamic Sizing Templates",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Text(
                    text = "${templates.size} custom guidelines defined",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedButton(
                onClick = { showTemplateGuideDialog = true },
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Icon(Icons.Default.HelpOutline, null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Guide 📖", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (templates.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text("Zero templates setup. Press '+' on bottom right to spawn models.", color = Color.Gray, textAlign = TextAlign.Center)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(templates, key = { it.id }) { template ->
                    val templateFields = fields.filter { it.templateId == template.id }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onEditTemplate(template.id) },
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Icon(Icons.Default.Style, null, tint = MaterialTheme.colorScheme.primary)
                                    Column {
                                        Text(template.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                        Text("${templateFields.size} active specifications", fontSize = 12.sp, color = Color.Gray)
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(
                                        onClick = { onEditTemplate(template.id) },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, "Edit Template", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                    }
                                    IconButton(
                                        onClick = { onDeleteTemplate(template.id) },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(Icons.Default.DeleteOutline, "Delete Template", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Template Guide Dialog
    if (showTemplateGuideDialog) {
        AlertDialog(
            onDismissRequest = { showTemplateGuideDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.HelpOutline, null, tint = MaterialTheme.colorScheme.primary)
                    Text("Sizing Guide & Help (سائزنگ گائیڈ)")
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .fillMaxWidth()
                ) {
                    // Card 1
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Dynamic Sizing Templates",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Define templates (for Shirts, Kurtas, Suits). Click a template card to configure, add parameters, or clear them.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Card 2
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "Urdu Keyboard & Native Mode / اردو کی بورڈ اور مقامی طریقہ کار",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "For typing custom template parameters in Urdu, you can write phonetically (e.g., enter 'bazu' to get 'بازو'), tap the keyboard icon beside the input field to use our complete built-in Urdu touch keyboard, or download a standard Urdu keyboard (like Google Gboard) from the Play Store for a full native keyboard experience across your entire device.",
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Card 3: New Enhanced Instruction Card how to create template form
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.25f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "How to Create a Sizing Template Form\nٹیمپلیٹ بنانے کا طریقہ",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                text = "1. Navigate to the 'Templates' tab from bottom menu.\n" +
                                       "2. Tap the '+' button (New Template) at the bottom right corner.\n" +
                                       "3. Input a template name layout (e.g., Kurta, Shirt, Suit) and save.\n" +
                                       "4. Click on your newly created template card to expand its options.\n" +
                                       "5. Tap 'Add Custom Sizing Field' or 'Clear Field' to define measurements (e.g., Length, Chest, Sleeves, Collar / لمبائی، چھاتی، بازو، تیرا).\n" +
                                       "6. Once saved, these custom parameters will be instantly ready for all customer measurement log entries!",
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showTemplateGuideDialog = false }) {
                    Text("Understood / سمجھ گیا")
                }
            }
        )
    }
}

// ---------------- EMPLOYEES & WORKER MANAGER TAB ----------------

@Composable
fun EmployeesTab(
    viewModel: TailorViewModel,
    employees: List<Employee>,
    workRecords: List<EmployeeWorkRecord>,
    paymentRecords: List<EmployeePaymentRecord>,
    currentUser: UserSession?,
    customers: List<Customer>
) {
    val context = LocalContext.current
    val isEmployee = currentUser?.isSubUser == true

    val activeWorker = remember(employees, currentUser) {
        if (isEmployee && currentUser != null) {
            employees.find { it.phoneOrEmail.trim().lowercase() == currentUser.email.trim().lowercase() }
        } else {
            null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top stats bar
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = if (isEmployee) "Employee Sizing Dashboard" else "Staff & Wage Manager",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = if (isEmployee) {
                        "Logged in as: ${currentUser?.displayName ?: "Staff Member"} (${currentUser?.email})"
                    } else {
                        "Manage tailors, log pieces made, track cash advances, and sync measurements"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
        }

        if (isEmployee) {
            if (activeWorker == null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "Profile Not Found",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            "Your current logged helper ID is '${currentUser?.email}'. Please ask the tailory owner/admin to register your ID as '${currentUser?.email}' in their Staff Directory. Once registered, you will see your work log history and pending wages instantly.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            } else {
                EmployeeDashboardView(
                    worker = activeWorker,
                    workRecords = workRecords.filter { it.employeeId == activeWorker.id },
                    paymentRecords = paymentRecords.filter { it.employeeId == activeWorker.id }
                )
            }
        } else {
            OwnerControlStationView(
                viewModel = viewModel,
                employees = employees,
                workRecords = workRecords,
                paymentRecords = paymentRecords,
                customers = customers
            )
        }
    }
}

@Composable
fun EmployeeDashboardView(
    worker: Employee,
    workRecords: List<EmployeeWorkRecord>,
    paymentRecords: List<EmployeePaymentRecord>
) {
    val totalEarned = remember(workRecords) { workRecords.sumOf { it.totalAmount } }
    val totalPaid = remember(paymentRecords) { paymentRecords.filter { it.paymentType == "PAYMENT" }.sumOf { it.amount } }
    val totalAdvance = remember(paymentRecords) { paymentRecords.filter { it.paymentType == "ADVANCE" }.sumOf { it.amount } }

    val receivedPlusAdvance = totalPaid + totalAdvance
    val outstandingBalance = totalEarned - receivedPlusAdvance

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Wages & Advances Overview", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Total Earned (Pieces)", fontSize = 12.sp, color = Color.Gray)
                        Text("${totalEarned}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Wages Received", fontSize = 12.sp, color = Color.Gray)
                        Text("${totalPaid}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Cash Advance Taken", fontSize = 12.sp, color = Color.Gray)
                        Text("${totalAdvance}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(if (outstandingBalance >= 0) "Wage Cash Balance" else "Outstanding Advance", fontSize = 12.sp, color = Color.Gray)
                        Text(
                            "${kotlin.math.abs(outstandingBalance)}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Black,
                            color = if (outstandingBalance >= 0) MaterialTheme.colorScheme.primary else Color(0xFFC62828)
                        )
                    }
                }
            }
        }

        Text("Your Activity Logging History", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        
        var historyTabSelection by remember { mutableStateOf(0) }
        TabRow(selectedTabIndex = historyTabSelection) {
            Tab(selected = historyTabSelection == 0, onClick = { historyTabSelection = 0 }, text = { Text("Pieces Complete") })
            Tab(selected = historyTabSelection == 1, onClick = { historyTabSelection = 1 }, text = { Text("Payments Logs") })
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (historyTabSelection == 0) {
                if (workRecords.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No piece-rate work logged yet.", color = Color.Gray)
                    }
                } else {
                    workRecords.forEach { r ->
                        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(r.suitType, fontWeight = FontWeight.Bold)
                                    Text("Logged ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(r.date))}", fontSize = 11.sp, color = Color.Gray)
                                    if (r.notes.isNotBlank()) {
                                        Text("Note: ${r.notes}", fontSize = 12.sp, color = Color.Gray, fontStyle = FontStyle.Italic)
                                    }
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("${r.piecesCount} pc × ${r.ratePaid}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("${r.totalAmount}", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                                }
                            }
                        }
                    }
                }
            } else {
                if (paymentRecords.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No payouts recorded yet.", color = Color.Gray)
                    }
                } else {
                    paymentRecords.forEach { p ->
                        val isAdvance = p.paymentType == "ADVANCE"
                        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        if (isAdvance) "ADVANCE GIVEN" else "WAGE TRANSACTION",
                                        fontWeight = FontWeight.Bold,
                                        color = if (isAdvance) Color(0xFFC62828) else Color(0xFF2E7D32)
                                    )
                                    Text("Cleared ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(p.date))}", fontSize = 11.sp, color = Color.Gray)
                                    if (p.notes.isNotBlank()) {
                                        Text("Note: ${p.notes}", fontSize = 12.sp, color = Color.Gray, fontStyle = FontStyle.Italic)
                                    }
                                }
                                Text("${p.amount}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = if (isAdvance) Color(0xFFC62828) else Color(0xFF2E7D32))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OwnerControlStationView(
    viewModel: TailorViewModel,
    employees: List<Employee>,
    workRecords: List<EmployeeWorkRecord>,
    paymentRecords: List<EmployeePaymentRecord>,
    customers: List<Customer>
) {
    var staffViewTab by remember { mutableStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = staffViewTab) {
            Tab(selected = staffViewTab == 0, onClick = { staffViewTab = 0 }, text = { Text("Staff List") })
            Tab(selected = staffViewTab == 1, onClick = { staffViewTab = 1 }, text = { Text("Log Work") })
            Tab(selected = staffViewTab == 2, onClick = { staffViewTab = 2 }, text = { Text("Payments") })
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column(modifier = Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            when (staffViewTab) {
                0 -> StaffListLayout(viewModel, employees, workRecords, paymentRecords)
                1 -> LogWorkLayout(viewModel, employees, workRecords, paymentRecords)
                2 -> PayStaffLayout(viewModel, employees, paymentRecords)
            }
        }
    }
}

@Composable
fun StaffListLayout(
    viewModel: TailorViewModel,
    employees: List<Employee>,
    workRecords: List<EmployeeWorkRecord>,
    paymentRecords: List<EmployeePaymentRecord>
) {
    val context = LocalContext.current
    var showAddDialog by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var idEmail by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("Tailor") }
    var defaultRate by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var passcodePin by remember { mutableStateOf("") }
    var employeeToPendingDelete by remember { mutableStateOf<Employee?>(null) }

    var rateItemsList by remember { mutableStateOf(listOf<Pair<String, Double>>()) }
    var pendingItemName by remember { mutableStateOf("") }
    var pendingItemRate by remember { mutableStateOf("") }

    val subUsersList by viewModel.registeredSubUsers.collectAsState()
    LaunchedEffect(Unit) {
        viewModel.fetchSubUsers()
    }

    Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text("Staff Directory", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Button(
            onClick = { showAddDialog = true },
            modifier = Modifier.testTag("add_employee_btn")
        ) {
            Icon(Icons.Default.Add, null)
            Spacer(modifier = Modifier.width(4.dp))
            Text("Register Staff")
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Register Employee/Staff") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth().testTag("add_emp_name_input"))
                    OutlinedTextField(value = idEmail, onValueChange = { idEmail = it }, label = { Text("Staff Log ID / Email (For Login)") }, modifier = Modifier.fillMaxWidth().testTag("add_emp_email_input"))
                    
                    OutlinedTextField(
                        value = passcodePin, 
                        onValueChange = { if (it.length <= 6) passcodePin = it }, 
                        label = { Text("Assign Login PIN Code (4-6 digits)") }, 
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth().testTag("add_emp_pin_input")
                    )

                    OutlinedTextField(value = role, onValueChange = { role = it }, label = { Text("Role (cutter, tailor etc.)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth())

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text("Configure Custom Piece Rates", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    Text("Add multiple custom items (e.g. Large Size, Small Size, etc.) and their rates for this tailor.", fontSize = 11.sp, color = Color.Gray)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = pendingItemName,
                            onValueChange = { pendingItemName = it },
                            label = { Text("Name item") },
                            modifier = Modifier.weight(1.3f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = pendingItemRate,
                            onValueChange = { pendingItemRate = it },
                            label = { Text("Rate") },
                            modifier = Modifier.weight(1.0f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true
                        )
                    }
                    Button(
                        onClick = {
                            val rateNum = pendingItemRate.toDoubleOrNull()
                            if (pendingItemName.isNotBlank() && rateNum != null) {
                                rateItemsList = rateItemsList + Pair(pendingItemName.trim(), rateNum)
                                pendingItemName = ""
                                pendingItemRate = ""
                            } else {
                                Toast.makeText(context, "Please enter a valid item name and rate!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Manual Piece Rate")
                    }

                    if (rateItemsList.isNotEmpty()) {
                        Text("Configured Pricings:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        ) {
                            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                rateItemsList.forEachIndexed { index, pair ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("${pair.first}  →  Rate: ${pair.second}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                        IconButton(
                                            onClick = {
                                                rateItemsList = rateItemsList.toMutableList().apply { removeAt(index) }
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, "Remove Item", tint = Color.Red, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Button(
                            onClick = {
                                rateItemsList = listOf(
                                    Pair("Large Size", 3.0),
                                    Pair("Medium Size", 2.0),
                                    Pair("Small Size", 1.0)
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer, contentColor = MaterialTheme.colorScheme.onTertiaryContainer),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Use Standard Template (Large/Medium/Small)")
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (name.isNotBlank() && idEmail.isNotBlank()) {
                        val arr = org.json.JSONArray()
                        for (item in rateItemsList) {
                            val obj = org.json.JSONObject()
                            obj.put("name", item.first)
                            obj.put("rate", item.second)
                            arr.put(obj)
                        }
                        val rateItemsJson = arr.toString()

                        viewModel.insertEmployee(
                            name = name,
                            phoneOrEmail = idEmail,
                            role = role,
                            defaultPieceRate = if (rateItemsList.isNotEmpty()) rateItemsList.first().second else 0.0,
                            notes = notes,
                            rateItemsJson = rateItemsJson
                        )
                        if (passcodePin.length >= 4) {
                            viewModel.registerSubUser(name, idEmail, passcodePin) { ok, err ->
                                if (ok) {
                                    Toast.makeText(context, "Successfully authorized credentials for helper $name!", Toast.LENGTH_SHORT).show()
                                } else {
                                    android.util.Log.e("StaffListLayout", "Failed helper subuser registrar: $err")
                                }
                            }
                        } else if (passcodePin.isNotEmpty()) {
                            Toast.makeText(context, "Authorization PIN must be between 4 and 6 digits! Room employee created without login authority.", Toast.LENGTH_LONG).show()
                        }
                        name = ""
                        idEmail = ""
                        role = "Tailor"
                        defaultRate = ""
                        notes = ""
                        passcodePin = ""
                        rateItemsList = emptyList()
                        pendingItemName = ""
                        pendingItemRate = ""
                        showAddDialog = false
                    } else {
                        Toast.makeText(context, "Name and Log ID (Email/Phone) are required fields!", Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Text("Register")
                }
            },
            dismissButton = {
                Button(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (employees.isEmpty()) {
        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
            Text("No staff registered yet.", color = Color.Gray, fontStyle = FontStyle.Italic)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(employees.size) { index ->
                val emp = employees[index]
                val empWork = remember(workRecords) { workRecords.filter { it.employeeId == emp.id } }
                val empPayments = remember(paymentRecords) { paymentRecords.filter { it.employeeId == emp.id } }

                val earned = empWork.sumOf { it.totalAmount }
                val paid = empPayments.filter { it.paymentType == "PAYMENT" }.sumOf { it.amount }
                val advance = empPayments.filter { it.paymentType == "ADVANCE" }.sumOf { it.amount }
                val balance = earned - (paid + advance)

                val matchingSubUser = remember(subUsersList, emp.phoneOrEmail) {
                    subUsersList.find { it.emailOrPhone.trim().lowercase() == emp.phoneOrEmail.trim().lowercase() }
                }

                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(emp.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                Text("ID/Email: ${emp.phoneOrEmail} | Role: ${emp.role}", fontSize = 11.sp, color = Color.Gray)
                                if (matchingSubUser != null) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier.padding(top = 2.dp)
                                    ) {
                                        Icon(Icons.Default.Lock, null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.secondary)
                                        Text("Login Active | PIN: ${matchingSubUser.passcode}", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            IconButton(onClick = { 
                                employeeToPendingDelete = emp
                            }) {
                                Icon(Icons.Default.Delete, "Delete Employee", tint = Color.Red)
                            }
                        }
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Earned", fontSize = 10.sp, color = Color.Gray)
                                Text("${earned}", fontWeight = FontWeight.Bold)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Paid", fontSize = 10.sp, color = Color.Gray)
                                Text("${paid}", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Advance", fontSize = 10.sp, color = Color.Gray)
                                Text("${advance}", fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(if (balance >= 0) "Wage Due" else "Adv Balance", fontSize = 10.sp, color = Color.Gray)
                                Text("${kotlin.math.abs(balance)}", fontWeight = FontWeight.Bold, color = if (balance >= 0) MaterialTheme.colorScheme.primary else Color(0xFFC62828))
                            }
                        }
                        if (emp.notes.isNotBlank()) {
                            Text("Note: ${emp.notes}", fontSize = 11.sp, fontStyle = FontStyle.Italic, color = Color.Gray)
                        }
                    }
                }
            }
        }
    }

    if (employeeToPendingDelete != null) {
        val emp = employeeToPendingDelete!!
        AlertDialog(
            onDismissRequest = { employeeToPendingDelete = null },
            title = { Text("Delete Staff Member ✓", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to remove '${emp.name}'? This will permanently remove their credentials and work logs.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteEmployee(emp.id)
                        if (emp.phoneOrEmail.isNotBlank()) {
                            viewModel.deleteSubUser(emp.phoneOrEmail) { _, _ -> }
                        }
                        employeeToPendingDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { employeeToPendingDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

fun groupClosedRecords(
    closedWork: List<EmployeeWorkRecord>,
    closedPayments: List<EmployeePaymentRecord>
): Map<String, Pair<List<EmployeeWorkRecord>, List<EmployeePaymentRecord>>> {
    val groups = mutableMapOf<String, Pair<MutableList<EmployeeWorkRecord>, MutableList<EmployeePaymentRecord>>>()
    
    fun getPeriodTag(notes: String, fallbackTime: Long): String {
        val pattern = "Closed Period on ([0-9/ :a-zA-Z]+)".toRegex()
        val pattern2 = "Closed on ([0-9/ :a-zA-Z]+)".toRegex()
        val match = pattern.find(notes) ?: pattern2.find(notes)
        if (match != null) {
            return match.groupValues[1].trim()
        }
        val df = java.text.SimpleDateFormat("dd/MM/yyyy hh:mm a", java.util.Locale.getDefault())
        return df.format(java.util.Date(fallbackTime))
    }
    
    closedWork.forEach { w ->
        val tag = getPeriodTag(w.notes, w.date)
        val pair = groups.getOrPut(tag) { Pair(mutableListOf(), mutableListOf()) }
        pair.first.add(w)
    }
    
    closedPayments.forEach { p ->
        val tag = getPeriodTag(p.notes, p.date)
        val pair = groups.getOrPut(tag) { Pair(mutableListOf(), mutableListOf()) }
        pair.second.add(p)
    }
    
    return groups
}

@Composable
fun LogWorkLayout(
    viewModel: TailorViewModel,
    employees: List<Employee>,
    workRecords: List<EmployeeWorkRecord>,
    paymentRecords: List<EmployeePaymentRecord>
) {
    var selectedEmpId by remember { mutableStateOf("") }
    
    var suitIdentity by remember { mutableStateOf("") }
    var selectedSizeName by remember { mutableStateOf("") }
    var selectedSizeRate by remember { mutableStateOf(0.0) }
    var piecesCount by remember { mutableStateOf(1) }
    
    var isCustomSelected by remember { mutableStateOf(false) }
    var customDesc by remember { mutableStateOf("") }
    var customRate by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    var subTabState by remember { mutableStateOf(0) }
    var expandedHistoryPeriod by remember { mutableStateOf<String?>(null) }
    var editingRecordId by remember { mutableStateOf<String?>(null) }
    var showGuideDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current

    if (employees.isEmpty()) {
        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
            Text("Please add employees first under the Staff List.", color = Color.Gray, fontStyle = FontStyle.Italic)
        }
    } else {
        val currentSelected = remember(employees, selectedEmpId) {
            employees.find { it.id == selectedEmpId }
        }

        val rateItems = remember(currentSelected) {
            val list = mutableListOf<Pair<String, Double>>()
            if (currentSelected != null) {
                val jsonStr = currentSelected.rateItemsJson
                if (jsonStr.isNotBlank()) {
                    try {
                        val arr = org.json.JSONArray(jsonStr)
                        for (i in 0 until arr.length()) {
                            val obj = arr.getJSONObject(i)
                            list.add(Pair(obj.getString("name"), obj.getDouble("rate")))
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                if (list.isEmpty()) {
                    val rate = currentSelected.defaultPieceRate
                    list.add(Pair("Large Size", if (rate > 0.0) rate else 400.0))
                    list.add(Pair("Medium Size", if (rate > 0.0) rate else 300.0))
                    list.add(Pair("Small Size", if (rate > 0.0) rate else 200.0))
                }
            }
            list
        }

        val sizeOptions = remember(rateItems) {
            rateItems + Pair("Custom (Ad-Hoc)", 0.0)
        }

        LaunchedEffect(selectedEmpId, rateItems) {
            if (rateItems.isNotEmpty()) {
                selectedSizeName = rateItems.first().first
                selectedSizeRate = rateItems.first().second
                isCustomSelected = false
            } else {
                selectedSizeName = ""
                selectedSizeRate = 0.0
                isCustomSelected = false
            }
            suitIdentity = ""
            piecesCount = 1
            customDesc = ""
            customRate = ""
            notes = ""
        }

        if (showGuideDialog) {
            AlertDialog(
                onDismissRequest = { showGuideDialog = false },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text("Tailor Work & Cycle Guide", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "This guide details how tailor wages, active sessions, and worksheet cycles operate.",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("1. Active Work Session 📝", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                Text(
                                    "• While a session is open, you can log finished suit entries with specific size categories and piece counts.\n" +
                                    "• Active records display in real-time under \"Worksheet & History\".\n" +
                                    "• Any incorrect entry can be edited in-place or deleted immediately.",
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                )
                            }
                        }

                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("2. Payouts & Advances 💸", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                                Text(
                                    "• Record payments under the \"Payments\" tab.\n" +
                                    "• \"Wage Clearance\" marks regular pay for finished work.\n" +
                                    "• \"Advance Issued\" files cash given in advance (which resolves against their wage balance).",
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                )
                            }
                        }

                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f))
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("3. Weekly Period Closing (ہفتہ وار کلوزنگ) 🔒", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                                Text(
                                    "• Processing of Weekly Period Settle & Closing locks current entries and shifts finished work to Archives.\n" +
                                    "• It archives the active session records and resets current outstanding balances to zero, preparing the system for a fresh cycle.\n" +
                                    "• Once closed, you must start a new period by tapping \"Open New Period\" to record new shirts or trousers.",
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showGuideDialog = false }) {
                        Text("Got It", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Record Finished Tailor Work", 
                            fontWeight = FontWeight.Bold, 
                            style = MaterialTheme.typography.titleMedium
                        )
                        IconButton(onClick = { showGuideDialog = true }) {
                            Icon(Icons.Default.Help, contentDescription = "Show Guide", tint = MaterialTheme.colorScheme.primary)
                        }
                    }

                    Text("Select Tailor:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (e in employees) {
                            val isSel = e.id == selectedEmpId
                            FilterChip(
                                selected = isSel,
                                onClick = { selectedEmpId = e.id },
                                label = { Text(e.name) }
                            )
                        }
                    }

                    if (selectedEmpId.isNotBlank() && currentSelected != null) {
                        // Two Tabs Segmented Controller for selected Tailor
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val tabs = listOf("Log New Entry 📝", "Worksheet & History 📊")
                            tabs.forEachIndexed { index, title ->
                                val selected = subTabState == index
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                        .clickable { subTabState = index }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = title,
                                        color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }

                        if (subTabState == 0) {
                            // Tab 0: Entry Form
                            if (!currentSelected.isCycleOpen) {
                                // Cycle is CLOSED, waiting for new opening
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)),
                                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.secondary)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(20.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = "Period Closed",
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(48.dp)
                                        )
                                        Text(
                                            text = "Active Session Closed\n(موجودہ پیریڈ بند ہے)",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                        Text(
                                            text = "All previous logs have been completed, settled, and closed. Inputs are locked.\n\nTap below to start/open a new weekly period of work.",
                                            fontSize = 12.sp,
                                            textAlign = TextAlign.Center,
                                            color = Color.DarkGray
                                        )
                                        Button(
                                            onClick = {
                                                viewModel.openEmployeePeriod(selectedEmpId)
                                                Toast.makeText(context, "New Open Period Started for ${currentSelected.name}!", Toast.LENGTH_SHORT).show()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                            modifier = Modifier.fillMaxWidth().testTag("period_open_new_btn")
                                        ) {
                                            Icon(Icons.Default.PlayArrow, null)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Open New Period (نیا پیریڈ شروع کریں)", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            } else {
                                // Cycle is OPEN, show the streamlined, compact input form
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
                                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                        .padding(12.dp)
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = if (editingRecordId != null) "✏️ Edit Suit Entry (ترمیم کریں)" else "📝 Log Suit Entry (سوٹ کا اندراج)",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            if (editingRecordId != null) {
                                                TextButton(
                                                    onClick = {
                                                        editingRecordId = null
                                                        suitIdentity = ""
                                                        piecesCount = 1
                                                        customDesc = ""
                                                        customRate = ""
                                                        notes = ""
                                                    },
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                                ) {
                                                    Text("Cancel Edit", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                                                }
                                            }
                                        }

                                        // CLIENT/SUIT IDENTIFICATION NAME
                                        OutlinedTextField(
                                            value = suitIdentity,
                                            onValueChange = { suitIdentity = it },
                                            label = { Text("Client Name / Suit ID (گاہک یا سوٹ کی پہچان)", fontSize = 11.sp) },
                                            placeholder = { Text("e.g. Kurta for Akram", fontSize = 11.sp) },
                                            modifier = Modifier.fillMaxWidth().testTag("log_suit_identity_input"),
                                            singleLine = true,
                                            textStyle = TextStyle(fontSize = 13.sp)
                                        )

                                        // SIZE SELECTOR
                                        Text("Select Size Category (سائز منتخب کریں):", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                        Row(
                                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            sizeOptions.forEach { pair ->
                                                val isSel = (isCustomSelected && pair.first == "Custom (Ad-Hoc)") ||
                                                             (!isCustomSelected && pair.first == selectedSizeName)
                                                FilterChip(
                                                    selected = isSel,
                                                    onClick = {
                                                        if (pair.first == "Custom (Ad-Hoc)") {
                                                            isCustomSelected = true
                                                            selectedSizeName = "Custom (Ad-Hoc)"
                                                            selectedSizeRate = 0.0
                                                        } else {
                                                            isCustomSelected = false
                                                            selectedSizeName = pair.first
                                                            selectedSizeRate = pair.second
                                                        }
                                                    },
                                                    label = {
                                                        if (pair.first == "Custom (Ad-Hoc)") {
                                                            Text("Custom (Ad-Hoc)", fontSize = 11.sp)
                                                        } else {
                                                            Text("${pair.first} (${pair.second})", fontSize = 11.sp)
                                                        }
                                                    },
                                                    modifier = Modifier.testTag("size_chip_${pair.first.replace(" ", "_")}")
                                                )
                                            }
                                        }

                                        // CUSTOM FIELDS (IF SELECTED)
                                        if (isCustomSelected) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                OutlinedTextField(
                                                    value = customDesc,
                                                    onValueChange = { customDesc = it },
                                                    label = { Text("Garment Name", fontSize = 11.sp) },
                                                    modifier = Modifier.weight(1.3f).testTag("custom_size_name_input"),
                                                    singleLine = true,
                                                    textStyle = TextStyle(fontSize = 13.sp)
                                                )
                                                OutlinedTextField(
                                                    value = customRate,
                                                    onValueChange = { customRate = it },
                                                    label = { Text("Rate", fontSize = 11.sp) },
                                                    modifier = Modifier.weight(1.0f).testTag("custom_size_rate_input"),
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                    singleLine = true,
                                                    textStyle = TextStyle(fontSize = 13.sp)
                                                )
                                            }
                                        }

                                        // COUNT SELECTOR AND OPTIONAL NOTES
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("Pieces Count (تعداد):", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                IconButton(
                                                    onClick = { if (piecesCount > 1) piecesCount-- },
                                                    modifier = Modifier
                                                        .border(1.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                                        .size(28.dp)
                                                        .testTag("quantity_decrement_btn")
                                                ) {
                                                    Icon(Icons.Default.Remove, "Remove Piece", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                                                }
                                                Text(
                                                    text = "$piecesCount",
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    fontWeight = FontWeight.Black,
                                                    modifier = Modifier.testTag("quantity_pcs_text")
                                                )
                                                IconButton(
                                                    onClick = { piecesCount++ },
                                                    modifier = Modifier
                                                        .border(1.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                                        .size(28.dp)
                                                        .testTag("quantity_increment_btn")
                                                ) {
                                                    Icon(Icons.Default.Add, "Add Piece", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                                                }
                                            }
                                        }

                                        OutlinedTextField(
                                            value = notes,
                                            onValueChange = { notes = it },
                                            label = { Text("Optional Notes", fontSize = 11.sp) },
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true,
                                            textStyle = TextStyle(fontSize = 13.sp)
                                        )

                                        if (editingRecordId != null) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                OutlinedButton(
                                                    onClick = {
                                                        editingRecordId = null
                                                        suitIdentity = ""
                                                        piecesCount = 1
                                                        customDesc = ""
                                                        customRate = ""
                                                        notes = ""
                                                    },
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Text("Cancel (منسوخ کریں)", fontSize = 12.sp)
                                                }

                                                Button(
                                                    onClick = {
                                                        if (suitIdentity.isBlank()) {
                                                            Toast.makeText(context, "Please enter client identity name!", Toast.LENGTH_SHORT).show()
                                                            return@Button
                                                        }

                                                        val finalSize: String
                                                        val finalRate: Double

                                                        if (isCustomSelected) {
                                                            if (customDesc.isBlank()) {
                                                                Toast.makeText(context, "Please enter garment name description!", Toast.LENGTH_SHORT).show()
                                                                return@Button
                                                            }
                                                            val rateVal = customRate.toDoubleOrNull() ?: 0.0
                                                            if (rateVal <= 0.0) {
                                                                Toast.makeText(context, "Please enter a valid rate greater than 0!", Toast.LENGTH_SHORT).show()
                                                                return@Button
                                                            }
                                                            finalSize = customDesc.trim()
                                                            finalRate = rateVal
                                                        } else {
                                                            finalSize = selectedSizeName
                                                            finalRate = selectedSizeRate
                                                        }

                                                        viewModel.updateEmployeeWorkRecord(
                                                            id = editingRecordId!!,
                                                            suitType = "$finalSize: ${suitIdentity.trim()}",
                                                            piecesCount = piecesCount,
                                                            ratePaid = finalRate,
                                                            notes = notes.trim()
                                                        )

                                                        Toast.makeText(context, "Updated suit details successfully!", Toast.LENGTH_SHORT).show()

                                                        // Reset inputs
                                                        suitIdentity = ""
                                                        piecesCount = 1
                                                        customDesc = ""
                                                        customRate = ""
                                                        notes = ""
                                                        editingRecordId = null
                                                    },
                                                    modifier = Modifier.weight(1.3f).testTag("update_log_work_btn")
                                                ) {
                                                    Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("Update Record", fontSize = 12.sp)
                                                }
                                            }
                                        } else {
                                            Button(
                                                onClick = {
                                                    if (suitIdentity.isBlank()) {
                                                        Toast.makeText(context, "Please enter client identity name or suit details!", Toast.LENGTH_SHORT).show()
                                                        return@Button
                                                    }

                                                    val finalSize: String
                                                    val finalRate: Double

                                                    if (isCustomSelected) {
                                                        if (customDesc.isBlank()) {
                                                            Toast.makeText(context, "Please enter garment name description!", Toast.LENGTH_SHORT).show()
                                                            return@Button
                                                        }
                                                        val rateVal = customRate.toDoubleOrNull() ?: 0.0
                                                        if (rateVal <= 0.0) {
                                                            Toast.makeText(context, "Please enter a valid rate greater than 0!", Toast.LENGTH_SHORT).show()
                                                            return@Button
                                                        }
                                                        finalSize = customDesc.trim()
                                                        finalRate = rateVal
                                                    } else {
                                                        finalSize = selectedSizeName
                                                        finalRate = selectedSizeRate
                                                    }

                                                    // Save the record
                                                    viewModel.insertEmployeeWorkRecord(
                                                        employeeId = selectedEmpId,
                                                        suitType = "$finalSize: ${suitIdentity.trim()}",
                                                        piecesCount = piecesCount,
                                                        ratePaid = finalRate,
                                                        notes = notes.trim()
                                                    )

                                                    Toast.makeText(context, "Logged garment details successfully!", Toast.LENGTH_SHORT).show()

                                                    // Reset inputs
                                                    suitIdentity = ""
                                                    piecesCount = 1
                                                    customDesc = ""
                                                    customRate = ""
                                                    notes = ""
                                                },
                                                modifier = Modifier.fillMaxWidth().testTag("save_log_work_btn")
                                            ) {
                                                Icon(Icons.Default.Save, null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Save Record (اندراج محفوظ کریں)", fontSize = 12.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            // Tab 1: Worksheet Stats, Active List, and Closing History
                            val activeWork = workRecords.filter { it.employeeId == selectedEmpId && !it.isClosed }
                            val activePayments = paymentRecords.filter { it.employeeId == selectedEmpId && !it.isClosed }

                            // STATS & TOTALS TABLE
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = "Active Session Worksheet Statistics (خلاصہ سوٹ)", 
                                        fontWeight = FontWeight.Bold, 
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    HorizontalDivider()

                                    val sizeSummary = remember(activeWork) {
                                        val map = mutableMapOf<String, Pair<Int, Double>>() // sizeName -> Pair(totalPieces, rate)
                                        activeWork.forEach { r ->
                                            val sz = r.suitType.substringBefore(": ")
                                            val current = map[sz] ?: Pair(0, r.ratePaid)
                                            map[sz] = Pair(current.first + r.piecesCount, r.ratePaid)
                                        }
                                        map
                                    }

                                    if (sizeSummary.isEmpty()) {
                                        Text(
                                            text = "No active work records found in the current session.", 
                                            fontStyle = FontStyle.Italic, 
                                            color = Color.Gray, 
                                            fontSize = 12.sp
                                        )
                                    } else {
                                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text("Garment Size Category", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1.5f))
                                                Text("Pieces", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(0.7f), textAlign = TextAlign.Center)
                                                Text("Rate", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(0.7f), textAlign = TextAlign.End)
                                                Text("Amount", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(0.9f), textAlign = TextAlign.End)
                                            }
                                            HorizontalDivider()
                                            sizeSummary.forEach { (size, data) ->
                                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                    Text(size, fontWeight = FontWeight.Medium, fontSize = 12.sp, modifier = Modifier.weight(1.5f))
                                                    Text("${data.first}", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, modifier = Modifier.weight(0.7f), textAlign = TextAlign.Center)
                                                    Text(String.format(Locale.getDefault(), "%.1f", data.second), fontSize = 12.sp, modifier = Modifier.weight(0.7f), textAlign = TextAlign.End)
                                                    Text(String.format(Locale.getDefault(), "%.1f", data.first * data.second), fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(0.9f), textAlign = TextAlign.End, color = Color(0xFF2E7D32))
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    HorizontalDivider()

                                    val totalActivePieces = activeWork.sumOf { it.piecesCount }
                                    val totalActiveWages = activeWork.sumOf { it.totalAmount }
                                    val totalWagesPaidInActive = activePayments.filter { it.paymentType == "PAYMENT" }.sumOf { it.amount }
                                    val totalAdvancesInActive = activePayments.filter { it.paymentType == "ADVANCE" }.sumOf { it.amount }
                                    val activeOutstandingBalance = totalActiveWages - (totalWagesPaidInActive + totalAdvancesInActive)

                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Total Active Pieces (کل سوٹ):", fontSize = 12.sp)
                                        Text("$totalActivePieces Pcs", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Wages Earned (کل اجرت):", fontSize = 12.sp)
                                        Text(String.format(Locale.getDefault(), "%.2f", totalActiveWages), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Payments Settled (موصول شدہ):", fontSize = 12.sp, color = Color(0xFF2E7D32))
                                        Text(String.format(Locale.getDefault(), "%.2f", totalWagesPaidInActive), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF2E7D32))
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Cash Advances (پیشنگی):", fontSize = 12.sp, color = Color(0xFFC62828))
                                        Text(String.format(Locale.getDefault(), "%.2f", totalAdvancesInActive), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFFC62828))
                                    }
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = if (activeOutstandingBalance >= 0) "Wage Cash Balance Due:" else "Outstanding Advance:",
                                            fontWeight = FontWeight.Black,
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                        Text(
                                            text = String.format(Locale.getDefault(), "%.2f", kotlin.math.abs(activeOutstandingBalance)),
                                            fontWeight = FontWeight.Black,
                                            style = MaterialTheme.typography.titleLarge,
                                            color = if (activeOutstandingBalance >= 0) MaterialTheme.colorScheme.primary else Color(0xFFC62828)
                                        )
                                    }
                                }
                            }

                            // Active recorded clothing/suits in a unified list (not in single cards)
                            if (activeWork.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("Active Log Worksheet Tracker (موجودہ سوٹ کا ریکارڈ):", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
                                ) {
                                    Column {
                                        activeWork.forEachIndexed { index, rec ->
                                            val sizeLabel = rec.suitType.substringBefore(": ")
                                            val identificationLabel = rec.suitType.substringAfter(": ")
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(identificationLabel, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                    Text(
                                                        text = "Size: $sizeLabel | Qty: ${rec.piecesCount} | Rate: ${rec.ratePaid}",
                                                        fontSize = 11.sp,
                                                        color = Color.Gray
                                                    )
                                                    Text(
                                                        text = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault()).format(Date(rec.date)),
                                                        fontSize = 10.sp,
                                                        color = Color.Gray
                                                    )
                                                }
                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    Text(
                                                        text = String.format(Locale.getDefault(), "%.1f", rec.totalAmount),
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF2E7D32),
                                                        fontSize = 13.sp
                                                    )
                                                    if (currentSelected.isCycleOpen) {
                                                        IconButton(
                                                            onClick = {
                                                                editingRecordId = rec.id
                                                                suitIdentity = identificationLabel
                                                                piecesCount = rec.piecesCount
                                                                notes = rec.notes
                                                                
                                                                if (sizeOptions.any { it.first == sizeLabel }) {
                                                                    isCustomSelected = false
                                                                    selectedSizeName = sizeLabel
                                                                    selectedSizeRate = rec.ratePaid
                                                                } else {
                                                                    isCustomSelected = true
                                                                    selectedSizeName = "Custom (Ad-Hoc)"
                                                                    customDesc = sizeLabel
                                                                    customRate = rec.ratePaid.toString()
                                                                }
                                                                subTabState = 0
                                                            },
                                                            modifier = Modifier.size(24.dp)
                                                        ) {
                                                            Icon(Icons.Default.Edit, "Edit Item", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                                        }

                                                        IconButton(
                                                            onClick = { viewModel.deleteEmployeeWorkRecord(rec.id) },
                                                            modifier = Modifier.size(24.dp)
                                                        ) {
                                                            Icon(Icons.Default.Delete, "Delete Item", tint = Color.Red, modifier = Modifier.size(16.dp))
                                                        }
                                                    }
                                                }
                                            }
                                            if (index < activeWork.lastIndex) {
                                                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                                            }
                                        }
                                    }
                                }
                            }

                            // WEEKLY CLOSING ACTIONS (Only visible if the cycle is open and there are active records)
                            if (currentSelected.isCycleOpen && activeWork.isNotEmpty()) {
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f))
                                ) {
                                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Icon(Icons.Default.Lock, "Weekly Close", tint = MaterialTheme.colorScheme.error)
                                            Text(
                                                text = "Weekly Period Closing (ہفتہ وار کلوزنگ)", 
                                                fontWeight = FontWeight.Bold, 
                                                style = MaterialTheme.typography.titleMedium, 
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }
                                        Button(
                                            onClick = {
                                                viewModel.closeEmployeePeriod(selectedEmpId)
                                                Toast.makeText(context, "Weekly period closed successfully. All assets archived.", Toast.LENGTH_LONG).show()
                                                suitIdentity = ""
                                                piecesCount = 1
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                            modifier = Modifier.fillMaxWidth().testTag("period_weekly_closing_btn")
                                        ) {
                                            Icon(Icons.Default.CheckCircle, null)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Process Settle & Closing (صاحب کلوزنگ کریں)", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }

                            // HISTORICAL CLOSED PERIODS CARD SECTION
                            val closedWork = workRecords.filter { it.employeeId == selectedEmpId && it.isClosed }
                            val closedPayments = paymentRecords.filter { it.employeeId == selectedEmpId && it.isClosed }

                            Spacer(modifier = Modifier.height(16.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = "Archives & Closing History (ماضی کی کلوزنگ)", 
                                        fontWeight = FontWeight.Bold, 
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )

                            if (closedWork.isEmpty() && closedPayments.isEmpty()) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                                ) {
                                    Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "No closed period history available yet. All automatic and manual closed periods appear here.",
                                            fontSize = 11.sp,
                                            color = Color.Gray,
                                            fontStyle = FontStyle.Italic,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            } else {
                                val closedGroups = remember(closedWork, closedPayments) {
                                    groupClosedRecords(closedWork, closedPayments)
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 8.dp)) {
                                    closedGroups.forEach { (periodTag, data) ->
                                        val cWork = data.first
                                        val cPayments = data.second

                                        val totalPcs = cWork.sumOf { it.piecesCount }
                                        val totalWagesEarned = cWork.sumOf { it.totalAmount }

                                        val isExpanded = expandedHistoryPeriod == periodTag

                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = periodTag, 
                                                            fontWeight = FontWeight.Bold, 
                                                            fontSize = 13.sp,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                        Text(
                                                            text = "Wages Settled: ${String.format(Locale.getDefault(), "%.1f", totalWagesEarned)} | Items: $totalPcs pcs", 
                                                            fontSize = 11.sp, 
                                                            color = Color.DarkGray
                                                        )
                                                    }
                                                    IconButton(onClick = { 
                                                        expandedHistoryPeriod = if (isExpanded) null else periodTag 
                                                    }) {
                                                        Icon(
                                                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, 
                                                            contentDescription = "Expand details"
                                                        )
                                                    }
                                                }

                                                if (isExpanded) {
                                                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                                                    
                                                    if (cWork.isNotEmpty()) {
                                                        Text("Closed Worksheet Items", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                                        Column(modifier = Modifier.padding(vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                            cWork.forEach { cw ->
                                                                val size = cw.suitType.substringBefore(": ")
                                                                val identity = cw.suitType.substringAfter(": ")
                                                                Row(
                                                                    modifier = Modifier.fillMaxWidth(),
                                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                                    verticalAlignment = Alignment.CenterVertically
                                                                ) {
                                                                    Column {
                                                                        Text(identity, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                                                        Text("Size: $size | Qty: ${cw.piecesCount}", fontSize = 11.sp, color = Color.Gray)
                                                                    }
                                                                    Text(
                                                                        text = String.format(Locale.getDefault(), "%.1f", cw.totalAmount), 
                                                                        fontWeight = FontWeight.Bold, 
                                                                        fontSize = 12.sp,
                                                                        color = Color(0xFF2E7D32)
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }

                                                    if (cPayments.isNotEmpty()) {
                                                        Spacer(modifier = Modifier.height(6.dp))
                                                        Text("Payments & Clearances", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                                                        Column(modifier = Modifier.padding(vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                            cPayments.forEach { cp ->
                                                                Row(
                                                                    modifier = Modifier.fillMaxWidth(),
                                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                                ) {
                                                                    Column {
                                                                        Text(if (cp.paymentType == "ADVANCE") "Advance Cash" else "Wage Settlement", fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                                                                        if (cp.notes.isNotBlank()) {
                                                                            val cleanedNotes = if (cp.notes.contains(" [Closed Period")) cp.notes.substringBefore(" [Closed Period") else cp.notes
                                                                            if (cleanedNotes.isNotBlank()) {
                                                                                Text(cleanedNotes, fontSize = 10.sp, color = Color.Gray)
                                                                            }
                                                                        }
                                                                    }
                                                                    Text(
                                                                        text = String.format(Locale.getDefault(), "%.1f", cp.amount), 
                                                                        fontWeight = FontWeight.Bold, 
                                                                        fontSize = 11.sp,
                                                                        color = if (cp.paymentType == "ADVANCE") Color(0xFFC62828) else Color(0xFF2E7D32)
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                                }
                            }
                        }
                    } else {
                        Text(
                            text = "No tailor selected. Select a tailor above to see their pieces list and record work.", 
                            color = Color.Gray, 
                            fontStyle = FontStyle.Italic, 
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PayStaffLayout(viewModel: TailorViewModel, employees: List<Employee>, paymentRecords: List<EmployeePaymentRecord>) {
    var selectedEmpId by remember { mutableStateOf("") }
    var payAmount by remember { mutableStateOf("") }
    var isAdvance by remember { mutableStateOf(false) }
    var notes by remember { mutableStateOf("") }

    val context = LocalContext.current

    if (employees.isEmpty()) {
        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
            Text("Please register staff members first to log payments.", color = Color.Gray)
        }
    } else {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Issue Payout or Advance", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)

                    Text("Select Employee:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (e in employees) {
                            val isSel = e.id == selectedEmpId
                            FilterChip(
                                selected = isSel,
                                onClick = { selectedEmpId = e.id },
                                label = { Text(e.name) }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            selected = !isAdvance,
                            onClick = { isAdvance = false },
                            shape = RoundedCornerShape(8.dp),
                            color = if (!isAdvance) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, if (!isAdvance) MaterialTheme.colorScheme.primary else Color.LightGray),
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Text("Wage Clearance", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                        Surface(
                            selected = isAdvance,
                            onClick = { isAdvance = true },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isAdvance) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, if (isAdvance) MaterialTheme.colorScheme.primary else Color.LightGray),
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Text("Advance Issued", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = payAmount,
                        onValueChange = { payAmount = it },
                        label = { Text("Amount") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Reference Note (e.g. June Week 2)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            val amount = payAmount.toDoubleOrNull() ?: 0.0
                            if (selectedEmpId.isBlank() || amount <= 0.0) {
                                Toast.makeText(context, "Select employee and specify valid payment amount", Toast.LENGTH_SHORT).show()
                            } else {
                                val payType = if (isAdvance) "ADVANCE" else "PAYMENT"
                                viewModel.insertEmployeePaymentRecord(selectedEmpId, payType, amount, notes)
                                payAmount = ""
                                notes = ""
                                Toast.makeText(context, "Transaction logged successfully!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Record Payout Transaction")
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationCenterDialog(
    viewModel: TailorViewModel,
    onDismiss: () -> Unit
) {
    val notifications by viewModel.notifications.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.startListeningToNotifications()
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.7f)
                .padding(4.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Notifications, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text(
                            text = "Alerts & Activity History",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }

                // Notifications list
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (notifications.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    "Your alerts history is completely clear. Notifications appear here when other tailors share message updates or measurements with you.",
                                    color = Color.Gray,
                                    textAlign = TextAlign.Center,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    } else {
                        items(notifications.size) { idx ->
                            val notif = notifications[idx]
                            val formattedTime = remember(notif.timestamp) {
                                try {
                                    SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(notif.timestamp))
                                } catch (e: Exception) {
                                    ""
                                }
                            }
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (notif.isRead) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) 
                                                     else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.50f)
                                )
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Text(
                                            text = notif.title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = if (notif.isRead) MaterialTheme.colorScheme.onSurfaceVariant 
                                                    else MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                        if (!notif.isRead) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.primary)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = notif.message,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = formattedTime,
                                        fontSize = 9.sp,
                                        color = Color.Gray,
                                        modifier = Modifier.align(Alignment.End)
                                    )
                                }
                            }
                        }
                    }
                }

                // Footer Actions
                Divider()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            viewModel.markNotificationsAsRead()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Mark All Read")
                    }
                    Button(
                        onClick = {
                            viewModel.clearNotifications()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Clear All")
                    }
                }
            }
        }
    }
}

@Composable
fun ChatDialog(
    receiverId: String,
    receiverName: String,
    viewModel: TailorViewModel,
    customers: List<Customer>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val currentUserId = viewModel.currentUser.collectAsState().value?.userId ?: ""
    val messages by viewModel.currentChatMessages.collectAsState()
    var messageInput by remember { mutableStateOf("") }
    
    // Multi-select state
    var showMultiSelectPicker by remember { mutableStateOf(false) }
    val selectedCustIds = remember { mutableStateMapOf<String, Boolean>() }

    // Block status
    val blockedUserIds by viewModel.blockedUserIds.collectAsState()
    val usersWhoBlockedMe by viewModel.usersWhoBlockedMe.collectAsState()
    val isBlockedByMe = blockedUserIds.contains(receiverId)
    val isBlockedByThem = usersWhoBlockedMe.contains(receiverId)

    // Start listening to chat
    LaunchedEffect(receiverId) {
        viewModel.listenToChat(receiverId)
    }

    // Clean up
    DisposableEffect(receiverId) {
        onDispose {
            viewModel.stopListeningToChat()
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(4.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Chat, "Chat Header", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        Column {
                            Text(
                                text = "Bespoke Chat",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                            Text(
                                text = receiverName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    // Block/Unblock toggle button
                    TextButton(
                        onClick = {
                            if (isBlockedByMe) {
                                viewModel.unblockUser(receiverId) { success, err ->
                                    if (success) {
                                        Toast.makeText(context, "$receiverName unblocked.", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Error: $err", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } else {
                                viewModel.blockUser(receiverId) { success, err ->
                                    if (success) {
                                        Toast.makeText(context, "$receiverName blocked.", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Error: $err", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = if (isBlockedByMe) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Text(if (isBlockedByMe) "Unblock" else "Block", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, "Dismiss Chat", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }

                if (isBlockedByMe) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "You have blocked this contact.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            TextButton(
                                onClick = {
                                    viewModel.unblockUser(receiverId) { _, _ -> }
                                },
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("Unblock now", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onErrorContainer, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else if (isBlockedByThem) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                "This contact has blocked you or is not accepting messages.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Chat Messages Scrollable History
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (messages.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                Text("No previous chat history found. Message securely directly here!", color = Color.Gray, textAlign = TextAlign.Center, fontSize = 13.sp)
                            }
                        }
                    } else {
                        items(messages.size) { index ->
                            val msg = messages[index]
                            val isMe = msg.senderId == currentUserId
                            val alignment = if (isMe) Alignment.End else Alignment.Start
                            val bubbleColor = if (isMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                            val textColor = if (isMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant

                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = alignment
                            ) {
                                Text(
                                    text = if (isMe) "You" else msg.senderName,
                                    fontSize = 10.sp,
                                    color = Color.Gray,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                                Card(
                                    shape = RoundedCornerShape(
                                        topStart = 12.dp,
                                        topEnd = 12.dp,
                                        bottomStart = if (isMe) 12.dp else 0.dp,
                                        bottomEnd = if (isMe) 0.dp else 12.dp
                                    ),
                                    colors = CardDefaults.cardColors(containerColor = bubbleColor),
                                    modifier = Modifier.fillMaxWidth(0.85f)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        if (msg.type == "measurements") {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = null,
                                                    tint = if (isMe) Color.White else MaterialTheme.colorScheme.primary
                                                )
                                                Text(
                                                    text = "Sent Sizing Cards 📏",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = textColor
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "Shared body models for:\n${msg.customerNames.joinToString(", ")}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = textColor.copy(alpha = 0.9f)
                                            )

                                            val chatId = remember(currentUserId, receiverId) { if (currentUserId < receiverId) "${currentUserId}_${receiverId}" else "${receiverId}_${currentUserId}" }
                                            
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Surface(
                                                color = if (isMe) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.3f),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                                            ) {
                                                Column(modifier = Modifier.padding(8.dp)) {
                                                    when (msg.shareStatus) {
                                                        "accepted" -> {
                                                            Row(
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Default.Check,
                                                                    contentDescription = null,
                                                                    tint = Color(0xFF386A20),
                                                                    modifier = Modifier.size(16.dp)
                                                                )
                                                                Text(
                                                                    text = "Added to database",
                                                                    color = if (isMe) textColor else Color(0xFF386A20),
                                                                    fontSize = 11.sp,
                                                                    fontWeight = FontWeight.Bold
                                                                )
                                                            }
                                                        }
                                                        "rejected" -> {
                                                            Row(
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Default.Close,
                                                                    contentDescription = null,
                                                                    tint = MaterialTheme.colorScheme.error,
                                                                    modifier = Modifier.size(16.dp)
                                                                )
                                                                Text(
                                                                    text = "Declined / rejected",
                                                                    color = if (isMe) textColor else MaterialTheme.colorScheme.error,
                                                                    fontSize = 11.sp,
                                                                    fontWeight = FontWeight.Bold
                                                                )
                                                            }
                                                        }
                                                        else -> { // "pending"
                                                            if (!isMe) {
                                                                // Recipient sees Accept / Reject options
                                                                Column(
                                                                    modifier = Modifier.fillMaxWidth(),
                                                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                                                ) {
                                                                    Text(
                                                                        text = "Would you like to import these customers?",
                                                                        fontSize = 11.sp,
                                                                        color = textColor.copy(alpha = 0.8f)
                                                                    )
                                                                    Row(
                                                                        modifier = Modifier.fillMaxWidth(),
                                                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                                    ) {
                                                                        Button(
                                                                            onClick = {
                                                                                viewModel.acceptSharedCustomers(chatId, msg) { success, err ->
                                                                                    if (success) {
                                                                                        Toast.makeText(context, "Sizing cards accepted & added!", Toast.LENGTH_SHORT).show()
                                                                                    } else {
                                                                                        Toast.makeText(context, "Failed: $err", Toast.LENGTH_LONG).show()
                                                                                    }
                                                                                }
                                                                            },
                                                                            modifier = Modifier.weight(1.5f).height(32.dp).testTag("accept_share_button"),
                                                                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                                                                            colors = ButtonDefaults.buttonColors(
                                                                                containerColor = Color(0xFF386A20),
                                                                                contentColor = Color.White
                                                                            ),
                                                                            shape = RoundedCornerShape(6.dp)
                                                                        ) {
                                                                            Text("Accept", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                                        }
                                                                        OutlinedButton(
                                                                            onClick = {
                                                                                viewModel.rejectSharedCustomers(chatId, msg) { success, err ->
                                                                                    if (success) {
                                                                                        Toast.makeText(context, "Rejected share", Toast.LENGTH_SHORT).show()
                                                                                    }
                                                                                }
                                                                            },
                                                                            modifier = Modifier.weight(1.0f).height(32.dp).testTag("reject_share_button"),
                                                                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                                                                            colors = ButtonDefaults.outlinedButtonColors(
                                                                                contentColor = MaterialTheme.colorScheme.error
                                                                            ),
                                                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                                                                            shape = RoundedCornerShape(6.dp)
                                                                        ) {
                                                                            Text("Delete", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                                        }
                                                                    }
                                                                }
                                                            } else {
                                                                // Sender sees pending status
                                                                Text(
                                                                    text = "Waiting for recipient to accept ⏳",
                                                                    fontSize = 11.sp,
                                                                    fontStyle = FontStyle.Italic,
                                                                    color = textColor.copy(alpha = 0.7f)
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        } else {
                                            Text(
                                                text = msg.content,
                                                fontSize = 14.sp,
                                                color = textColor
                                            )
                                        }
                                        
                                        Spacer(modifier = Modifier.height(2.dp))
                                        val timeStr = remember(msg.timestamp) {
                                            try {
                                                SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(msg.timestamp))
                                            } catch (e: Exception) {
                                                ""
                                            }
                                        }
                                        Text(
                                            text = timeStr,
                                            fontSize = 9.sp,
                                            color = textColor.copy(alpha = 0.6f),
                                            modifier = Modifier.align(Alignment.End)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Input Area
                Surface(
                    tonalElevation = 4.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        if (isBlockedByMe || isBlockedByThem) {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isBlockedByMe) "Unblock this contact to start composing messages." else "Messaging is unavailable for this contact.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.Gray,
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                // "Send Measurements" Button
                                Button(
                                    onClick = {
                                        selectedCustIds.clear()
                                        showMultiSelectPicker = true
                                    },
                                    shape = CircleShape,
                                    contentPadding = PaddingValues(0.dp),
                                    modifier = Modifier.size(40.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Send Measurements",
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }

                                // Message Text Input
                                OutlinedTextField(
                                    value = messageInput,
                                    onValueChange = { messageInput = it },
                                    placeholder = { Text("Write message...") },
                                    maxLines = 3,
                                    shape = RoundedCornerShape(24.dp),
                                    modifier = Modifier.weight(1f),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = Color.LightGray
                                    )
                                )

                                // Send Button
                                Button(
                                    onClick = {
                                        val text = messageInput.trim()
                                        if (text.isNotEmpty()) {
                                            viewModel.sendChatMessage(receiverId, text) { ok, err ->
                                                if (ok) {
                                                    messageInput = ""
                                                } else {
                                                    Toast.makeText(context, "Error sending: $err", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                    },
                                    shape = CircleShape,
                                    contentPadding = PaddingValues(0.dp),
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Send,
                                        contentDescription = "Send Message",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showMultiSelectPicker) {
        AlertDialog(
            onDismissRequest = { showMultiSelectPicker = false },
            title = { Text("Select Customer Measurements", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Choose one or more customer sizing profiles to compile and share directly into $receiverName's app database.",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    if (customers.isEmpty()) {
                        Text("No customer records found to write or share.", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
                    } else {
                        LazyColumn(modifier = Modifier.heightIn(max = 280.dp)) {
                            items(customers.size) { cIdx ->
                                val c = customers[cIdx]
                                val isChecked = selectedCustIds[c.id] ?: false
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { selectedCustIds[c.id] = !isChecked }
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(c.name, fontWeight = FontWeight.Bold)
                                        Text("Ph: ${c.phone} | Address: ${c.address}", fontSize = 11.sp, color = Color.Gray)
                                    }
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = { selectedCustIds[c.id] = it }
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                val selectCount = selectedCustIds.filter { it.value }.size
                Button(
                    enabled = selectCount > 0,
                    onClick = {
                        val chosenList = customers.filter { selectedCustIds[it.id] == true }
                        if (chosenList.isNotEmpty()) {
                            viewModel.sendMultipleCustomerMeasurements(receiverId, chosenList) { success, errMsg ->
                                if (success) {
                                    Toast.makeText(context, "$selectCount customer sizing cards sent successfully over Chat!", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(context, "Failed to share sizing sheets: $errMsg", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                        showMultiSelectPicker = false
                    }
                ) {
                    Text("Send ($selectCount) Sizing Cards", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showMultiSelectPicker = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ContactMatcherLayout(viewModel: TailorViewModel, customers: List<Customer>) {
    val context = LocalContext.current
    val matchedContacts by viewModel.matchedContacts.collectAsState()
    val isLoading by viewModel.contactMatchingLoading.collectAsState()
    val blockedUserIds by viewModel.blockedUserIds.collectAsState()

    var activeChatReceiverId by remember { mutableStateOf("") }
    var activeChatReceiverName by remember { mutableStateOf("") }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.startRealtimeContactSync(context)
        viewModel.matchContacts(context)
        if (!isGranted) {
            Toast.makeText(context, "Contacts permission not accepted. Showing registered app users.", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.startRealtimeContactSync(context)
        viewModel.matchContacts(context)
    }

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Contact Partner Matcher", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = {
                    permissionLauncher.launch(android.Manifest.permission.READ_CONTACTS)
                }) {
                    Text("Scan")
                }
            }
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            val registeredContacts = matchedContacts.filter { it.isAppUser }
            if (registeredContacts.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("No registered app partners found.", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        registeredContacts.forEach { contact ->
                            val isBlocked = blockedUserIds.contains(contact.appUserId ?: "")
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)),
                                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(6.dp).fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(contact.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text(contact.phone, fontSize = 11.sp, color = Color.Gray)
                                        if (isBlocked) {
                                            Text("Blocked Partner 🚫", color = MaterialTheme.colorScheme.error, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        } else {
                                            Text("Registered App Partner ✓", color = MaterialTheme.colorScheme.primary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Button(
                                            onClick = {
                                                activeChatReceiverId = contact.appUserId ?: ""
                                                activeChatReceiverName = contact.name
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                            modifier = Modifier.height(28.dp)
                                        ) {
                                            Icon(Icons.Default.Chat, null, modifier = Modifier.size(11.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Chat", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                val partnerId = contact.appUserId ?: ""
                                                if (partnerId.isNotEmpty()) {
                                                    if (isBlocked) {
                                                        viewModel.unblockUser(partnerId) { _, _ -> }
                                                    } else {
                                                        viewModel.blockUser(partnerId) { _, _ -> }
                                                    }
                                                }
                                            },
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                contentColor = if (isBlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                            ),
                                            border = BorderStroke(
                                                1.dp,
                                                if (isBlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error.copy(alpha = 0.4f)
                                            ),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                            modifier = Modifier.height(28.dp)
                                        ) {
                                            Text(if (isBlocked) "Unblock" else "Block", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (activeChatReceiverId.isNotEmpty()) {
        ChatDialog(
            receiverId = activeChatReceiverId,
            receiverName = activeChatReceiverName,
            viewModel = viewModel,
            customers = customers,
            onDismiss = {
                activeChatReceiverId = ""
                activeChatReceiverName = ""
            }
        )
    }
}

// ---------------- PROFILE SETTINGS & BACKUP TAB ----------------

@Composable
fun BackupTab(
    viewModel: TailorViewModel,
    onGoogleSignInClick: () -> Unit,
    employees: List<Employee> = emptyList(),
    workRecords: List<EmployeeWorkRecord> = emptyList(),
    paymentRecords: List<EmployeePaymentRecord> = emptyList(),
    customers: List<Customer> = emptyList()
) {
    val context = LocalContext.current
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
    var isBackupPrivacyAccepted by remember { mutableStateOf(false) }
    var activeBackupWebViewUrl by remember { mutableStateOf<String?>(null) }

    val shopName by viewModel.shopName.collectAsState()
    val shopPhone by viewModel.shopPhone.collectAsState()
    val shopAddress by viewModel.shopAddress.collectAsState()

    val isAppLockEnabled by viewModel.isAppLockEnabled.collectAsState()
    val appLockPin by viewModel.appLockPin.collectAsState()
    val isBiometricEnabled by viewModel.isBiometricEnabled.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    var isLockEnabled by remember { mutableStateOf(isAppLockEnabled) }
    var isBiometricsEnabledState by remember { mutableStateOf(isBiometricEnabled) }
    var pinSetupText by remember { mutableStateOf("") }
    var oldPinText by remember { mutableStateOf("") }
    var newPinText by remember { mutableStateOf("") }
    var confirmPinText by remember { mutableStateOf("") }

    var subUserNameInput by remember { mutableStateOf("") }
    var subUserIdentifierInput by remember { mutableStateOf("") }
    var subUserPasscodeInput by remember { mutableStateOf("") }

    var inlineSubUserId by remember { mutableStateOf("") }
    var inlineSubUserPasscode by remember { mutableStateOf("") }
    var showStaffManagerDialog by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            try {
                val data = viewModel.exportAllJson()
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(data.toByteArray())
                }
                Toast.makeText(context, "Full System Export saved to JSON file successfully!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Export error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val data = inputStream?.bufferedReader()?.use { it.readText() } ?: ""
                if (data.isNotBlank()) {
                    val ok = viewModel.importAllJson(data)
                    if (ok) {
                        Toast.makeText(context, "Full System Database imported successfully!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Invalid JSON Portability format", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(context, "Selected backup file is empty", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Import error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    LaunchedEffect(isAppLockEnabled, isBiometricEnabled) {
        isLockEnabled = isAppLockEnabled
        isBiometricsEnabledState = isBiometricEnabled
    }

    var customName by remember { mutableStateOf(shopName) }
    var customPhone by remember { mutableStateOf(shopPhone) }
    var customAddress by remember { mutableStateOf(shopAddress) }

    var jsonConsoleInput by remember { mutableStateOf("") }

    LaunchedEffect(shopName, shopPhone, shopAddress) {
        customName = shopName
        customPhone = shopPhone
        customAddress = shopAddress
    }

    val isUrduMode by viewModel.isUrduSelected.collectAsState()
    var showUrduKeyboardDialog by remember { mutableStateOf(false) }

    if (activeBackupWebViewUrl != null) {
        PrivacyWebViewOverlay(
            url = activeBackupWebViewUrl!!,
            onDismiss = { activeBackupWebViewUrl = null },
            onConfirm = {
                isBackupPrivacyAccepted = true
                activeBackupWebViewUrl = null
            }
        )
    }

    if (showUrduKeyboardDialog) {
        AlertDialog(
            onDismissRequest = { showUrduKeyboardDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Language, null, tint = MaterialTheme.colorScheme.primary)
                    Text("Urdu Phonetic Keyboard Info", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text(
                    text = "For typing custom template parameters in Urdu, you can write phonetically (e.g., enter 'bazu' to get 'بازو'), tap the keyboard icon beside the input field to use our complete built-in Urdu touch keyboard, or download a standard Urdu keyboard (like Google Gboard) from the Play Store for a full native keyboard experience across your entire device.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(onClick = { showUrduKeyboardDialog = false }) {
                    Text("Got it")
                }
            }
        )
    }

    @Composable
    fun AppLockContentCard() {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("App PIN Lock & Security", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Secure your tailory records containing client fit data and ledger totals with an offline passcode.", fontSize = 13.sp, color = Color.Gray)

                HorizontalDivider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Enable Security Passcode LOCK")
                    Switch(
                        checked = isLockEnabled,
                        onCheckedChange = { 
                            isLockEnabled = it 
                            if (!it) {
                                viewModel.setAppLock(false, "", false)
                                Toast.makeText(context, "App Security Screen Disabled", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.testTag("app_lock_switch")
                    )
                }

                if (isLockEnabled) {
                    if (appLockPin.isBlank()) {
                        Text("Create Fresh Security PIN:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        OutlinedTextField(
                            value = pinSetupText,
                            onValueChange = { if (it.length <= 6) pinSetupText = it },
                            label = { Text("Set 4-6 digit PIN") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth().testTag("setup_pin_field")
                        )
                        OutlinedTextField(
                            value = confirmPinText,
                            onValueChange = { if (it.length <= 6) confirmPinText = it },
                            label = { Text("Confirm 4-6 digit PIN") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth().testTag("confirm_setup_pin_field")
                        )
                        Button(
                            onClick = {
                                if (pinSetupText.length in 4..6 && pinSetupText == confirmPinText) {
                                    viewModel.setAppLock(true, pinSetupText, isBiometricsEnabledState)
                                    Toast.makeText(context, "PIN Locker Activated", Toast.LENGTH_SHORT).show()
                                    pinSetupText = ""
                                    confirmPinText = ""
                                } else {
                                    Toast.makeText(context, "PIN details do not match or are less than 4 digits", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth().testTag("activate_passcode_btn")
                        ) {
                            Text("Activate Passcode")
                        }
                    } else {
                        Text("Update Security Passcode:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        OutlinedTextField(
                            value = oldPinText,
                            onValueChange = { if (it.length <= 6) oldPinText = it },
                            label = { Text("Old Lock PIN") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth().testTag("old_pin_field")
                        )
                        OutlinedTextField(
                            value = newPinText,
                            onValueChange = { if (it.length <= 6) newPinText = it },
                            label = { Text("New Lock PIN (4-6 digits)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth().testTag("new_pin_field")
                        )
                        OutlinedTextField(
                            value = confirmPinText,
                            onValueChange = { if (it.length <= 6) confirmPinText = it },
                            label = { Text("Confirm New PIN") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth().testTag("confirm_new_pin_field")
                        )
                        Button(
                            onClick = {
                                if (oldPinText == appLockPin) {
                                    if (newPinText.length in 4..6 && newPinText == confirmPinText) {
                                        viewModel.setAppLock(true, newPinText, isBiometricsEnabledState)
                                        Toast.makeText(context, "Passcode Changed Successfully", Toast.LENGTH_SHORT).show()
                                        oldPinText = ""
                                        newPinText = ""
                                        confirmPinText = ""
                                    } else {
                                        Toast.makeText(context, "New passwords do not match or represent bad format", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    Toast.makeText(context, "Invalid OLD lock passcode entered", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth().testTag("save_new_pin_btn")
                        ) {
                            Text("Save New PIN Settings")
                        }
                    }

                    HorizontalDivider()

                    val biometricManager = remember(context) { BiometricManager.from(context) }
                    val isBiometricHardwareAvailable = remember(biometricManager) {
                        biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) != BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE
                    }

                    if (isBiometricHardwareAvailable) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Optional Fingerprints Lock")
                                Text("Use enrollable system fingerprint to bypass manual passcode PIN keyboard input", fontSize = 11.sp, color = Color.Gray)
                            }
                            Switch(
                                checked = isBiometricsEnabledState,
                                onCheckedChange = { 
                                    isBiometricsEnabledState = it 
                                    viewModel.setAppLock(true, appLockPin, it)
                                    Toast.makeText(context, "Biometric security settings updated", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.testTag("biometric_switch")
                            )
                        }
                    } else {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f),
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = "Fingerprint sensor hardware was not detected on this device. Biometrics setup disabled; security lock options default solely to 4, 5, or 6-digit offline PIN passcode authorization.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun UnifiedJsonBackupCard() {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Unified JSON Portability", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("All systems (Customers, Custom Templates, Ledgers, inventory lists, and Shop Branding details) are encapsulated in a single portability file.", fontSize = 13.sp, color = Color.Gray)

                HorizontalDivider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            try {
                                exportLauncher.launch("tailor_workspace_backup.json")
                            } catch (e: Exception) {
                                Toast.makeText(context, "Launch Export Failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1f).testTag("export_unified_button")
                    ) {
                        Icon(Icons.Default.FileDownload, null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Export File", fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            try {
                                importLauncher.launch(arrayOf("application/json", "application/octet-stream", "*/*"))
                            } catch (e: Exception) {
                                Toast.makeText(context, "Launch Import Failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        modifier = Modifier.weight(1f).testTag("import_unified_button")
                    ) {
                        Icon(Icons.Default.FileUpload, null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Import File", fontSize = 12.sp)
                    }
                }

                OutlinedTextField(
                    value = jsonConsoleInput,
                    onValueChange = { jsonConsoleInput = it },
                    label = { Text("Unified JSON Backup Code Console (Paste or Edit package)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .testTag("json_backup_input"),
                    textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace),
                    maxLines = 10
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            try {
                                val fullDbJson = viewModel.exportAllJson()
                                jsonConsoleInput = fullDbJson
                                Toast.makeText(context, "Loaded Live Database JSON to text console", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Error exporting live data: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1f).testTag("load_json_to_console_btn")
                    ) {
                        Icon(Icons.Default.FileDownload, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Copy Live to Console", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            if (jsonConsoleInput.isBlank()) {
                                Toast.makeText(context, "Paste or generate JSON first inside the console", Toast.LENGTH_SHORT).show()
                            } else {
                                try {
                                    val success = viewModel.importAllJson(jsonConsoleInput)
                                    if (success) {
                                        Toast.makeText(context, "Console JSON Imported Successfully!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Malformed or unsupported Console JSON structure", Toast.LENGTH_LONG).show()
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Import failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier.weight(1f).testTag("apply_json_from_console_btn")
                    ) {
                        Icon(Icons.Default.FileUpload, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Import Console Info", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Preference Header with Title
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Control Center Preferences",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Configure app locks, branding, staff helpers and backups",
                fontSize = 11.sp,
                color = Color.Gray
            )
        }

        // Tabbing system variables
        val user = currentUser
        val isSubUser = user?.isSubUser == true
        val tabsList = remember(user, isSubUser) {
            when {
                user == null -> listOf(
                    Triple("Cloud Backup", Icons.Default.CloudQueue, "tab_cloud_backups"),
                    Triple("PIN Locker", Icons.Default.Lock, "tab_pin_lock"),
                    Triple("JSON Backup", Icons.Default.FileDownload, "tab_json_backup")
                )
                isSubUser -> listOf(
                    Triple("Wage Log", Icons.Default.AssignmentInd, "tab_wage_log"),
                    Triple("PIN Locker", Icons.Default.Lock, "tab_pin_lock"),
                    Triple("JSON Backup", Icons.Default.FileDownload, "tab_json_backup")
                )
                else -> listOf(
                    Triple("Brand Identity", Icons.Default.Business, "tab_brand_lang"),
                    Triple("PIN Locker", Icons.Default.Lock, "tab_pin_lock"),
                    Triple("Staff Station", Icons.Default.Engineering, "tab_staff_station"),
                    Triple("Data Interchange", Icons.Default.SyncAlt, "tab_data_share")
                )
            }
        }

        var activeTabSubIndex by remember { mutableStateOf(0) }
        LaunchedEffect(tabsList) {
            if (activeTabSubIndex >= tabsList.size) {
                activeTabSubIndex = 0
            }
        }

        ScrollableTabRow(
            selectedTabIndex = activeTabSubIndex,
            edgePadding = 0.dp,
            containerColor = Color.Transparent,
            modifier = Modifier.fillMaxWidth()
        ) {
            tabsList.forEachIndexed { idx, (itemTitle, itemIcon, tag) ->
                Tab(
                    selected = activeTabSubIndex == idx,
                    onClick = { activeTabSubIndex = idx },
                    modifier = Modifier.testTag(tag),
                    text = { Text(itemTitle, fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                    icon = { Icon(itemIcon, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (user != null) {
                var showDeleteConfirmDialog by remember { mutableStateOf(false) }
                var deleteConfirmInputText by remember { mutableStateOf("") }

                if (showDeleteConfirmDialog) {
                    AlertDialog(
                        onDismissRequest = { 
                            showDeleteConfirmDialog = false
                            deleteConfirmInputText = ""
                        },
                        title = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error)
                                Text("Delete Account Completely?", fontWeight = FontWeight.Bold)
                            }
                        },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    "This will completely delete your registered account from Google Cloud, disconnect all devices, and purge your cloud backups.",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    "Your local app database and active preferences will also be completely wiped. This action is irreversible and cannot be undone.",
                                    fontSize = 13.sp,
                                    color = Color.Gray
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "To prevent children or accidental deletion, please type 'CONFIRM' below to unlock the delete action:",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.error
                                )
                                OutlinedTextField(
                                    value = deleteConfirmInputText,
                                    onValueChange = { deleteConfirmInputText = it },
                                    label = { Text("Enter CONFIRM to verify") },
                                    placeholder = { Text("Type 'CONFIRM'") },
                                    modifier = Modifier.fillMaxWidth().testTag("delete_account_confirm_input"),
                                    singleLine = true
                                )
                            }
                        },
                        confirmButton = {
                            val isConfirmed = deleteConfirmInputText.trim() == "CONFIRM"
                            Button(
                                onClick = {
                                    showDeleteConfirmDialog = false
                                    deleteConfirmInputText = ""
                                    viewModel.deleteAccount { success, errorMsg ->
                                        if (success) {
                                            Toast.makeText(context, "Account and data purged completely.", Toast.LENGTH_LONG).show()
                                        } else {
                                            Toast.makeText(context, "Purge failed: $errorMsg", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                },
                                enabled = isConfirmed,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    disabledContainerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.4f)
                                )
                            ) {
                                Text("Yes, Delete Completely", fontWeight = FontWeight.Bold)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { 
                                showDeleteConfirmDialog = false
                                deleteConfirmInputText = ""
                            }) {
                                Text("Cancel")
                            }
                        }
                    )
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Connected: ${user.displayName}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = if (user.isSubUser) "Access: Staff Helper (Auto-Synced)" else "Access: Shop Owner (Auto-Synced)",
                                    fontSize = 11.sp,
                                    color = Color.Gray,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            TextButton(
                                onClick = {
                                    viewModel.signOut()
                                    Toast.makeText(context, "Signed out safely", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.testTag("profile_signout_btn")
                            ) {
                                Icon(Icons.Default.ExitToApp, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Sign Out", fontSize = 12.sp, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                            }
                        }

                        HorizontalDivider(thickness = 0.8.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Account Deletion",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = "Permanently remove your Google account and purge cloud storage.",
                                    fontSize = 10.sp,
                                    color = Color.Gray
                                )
                            }
                            OutlinedButton(
                                onClick = { showDeleteConfirmDialog = true },
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier.testTag("delete_account_btn").height(32.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp)
                            ) {
                                Icon(Icons.Default.DeleteForever, null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Delete Account", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            val activeTabTag = tabsList.getOrNull(activeTabSubIndex)?.third ?: ""

            when {
                user == null -> {
                    when (activeTabTag) {
                        "tab_cloud_backups" -> {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp))
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Text(
                                        text = "Sign In to Workspace Cloud Backup",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Connect to cloud synchronization services to back up, share, and manage tailor shop records seamlessly.",
                                        fontSize = 13.sp,
                                        color = Color.Gray
                                    )

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = isBackupPrivacyAccepted,
                                            onCheckedChange = { isBackupPrivacyAccepted = it },
                                            modifier = Modifier.testTag("backup_privacy_checkbox")
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "I agree to the ",
                                            fontSize = 13.sp,
                                            color = Color.Gray
                                        )
                                        Text(
                                            text = "Privacy Policy",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline,
                                            modifier = Modifier
                                                .clickable {
                                                    try {
                                                        activeBackupWebViewUrl = "https://tailor-shop-manager-eight.vercel.app/privacy.html"
                                                    } catch (e: Exception) {
                                                        e.printStackTrace()
                                                    }
                                                }
                                                .testTag("backup_privacy_link")
                                        )
                                    }

                                    HorizontalDivider()

                                    Text(
                                        text = "Primary Shop Owner Access",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )

                                    Button(
                                        onClick = onGoogleSignInClick,
                                        enabled = isBackupPrivacyAccepted,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp)
                                            .testTag("google_signin_profile_btn")
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.AccountCircle,
                                                contentDescription = "Google Sign In",
                                                modifier = Modifier.size(24.dp),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Sign In with Google", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    HorizontalDivider()

                                    Text(
                                        text = "Staff / Helper direct login",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )

                                    OutlinedTextField(
                                        value = inlineSubUserId,
                                        onValueChange = { inlineSubUserId = it },
                                        label = { Text("Staff ID (Email or Phone)") },
                                        modifier = Modifier.fillMaxWidth().testTag("sub_user_inline_id"),
                                        singleLine = true
                                    )

                                    OutlinedTextField(
                                        value = inlineSubUserPasscode,
                                        onValueChange = { if (it.length <= 6) inlineSubUserPasscode = it },
                                        label = { Text("Staff PIN / Passcode") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                                        modifier = Modifier.fillMaxWidth().testTag("sub_user_inline_passcode"),
                                        singleLine = true
                                    )

                                    Button(
                                        onClick = {
                                            if (inlineSubUserId.isBlank() || inlineSubUserPasscode.isBlank()) {
                                                Toast.makeText(context, "Provide Staff unique ID and registered PIN code", Toast.LENGTH_SHORT).show()
                                            } else {
                                                viewModel.signInAsSubUser(inlineSubUserId, inlineSubUserPasscode) { success, errorMsg ->
                                                    if (success) {
                                                        Toast.makeText(context, "Helper session authenticated successfully!", Toast.LENGTH_SHORT).show()
                                                        inlineSubUserId = ""
                                                        inlineSubUserPasscode = ""
                                                    } else {
                                                        Toast.makeText(context, "Direct authentication failed: $errorMsg", Toast.LENGTH_LONG).show()
                                                    }
                                                }
                                            }
                                        },
                                        enabled = isBackupPrivacyAccepted,
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                        modifier = Modifier.fillMaxWidth().testTag("sub_user_inline_signin_btn")
                                    ) {
                                        Icon(Icons.Default.Login, null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Sign In as Helper")
                                    }
                                }
                            }
                        }
                        "tab_pin_lock" -> {
                            AppLockContentCard()
                        }
                        "tab_json_backup" -> {
                            UnifiedJsonBackupCard()
                        }
                    }
                }
                
                isSubUser -> {
                    when (activeTabTag) {
                        "tab_wage_log" -> {
                            val activeWorker = remember(employees, user) {
                                employees.find { it.phoneOrEmail.trim().lowercase() == user.email.trim().lowercase() }
                            }
                            if (activeWorker == null) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(
                                            "Profile Not Found",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                        Text(
                                            "Your current logged helper ID is '${user.email}'. Please ask the tailory owner/admin to register your ID as '${user.email}' in their Staff Directory. Once registered, you will see your work log history and pending wages instantly.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    }
                                }
                            } else {
                                EmployeeDashboardView(
                                    worker = activeWorker,
                                    workRecords = workRecords.filter { it.employeeId == activeWorker.id },
                                    paymentRecords = paymentRecords.filter { it.employeeId == activeWorker.id }
                                )
                            }
                        }
                        "tab_pin_lock" -> {
                            AppLockContentCard()
                        }
                        "tab_json_backup" -> {
                            UnifiedJsonBackupCard()
                        }
                    }
                }
                
                else -> {
                    when (activeTabTag) {
                        "tab_brand_lang" -> {
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text("Shop Identity Profiles setup", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                    Text("Configure your branding details. These will print automatically at the top of client custom sizing receipts.", fontSize = 13.sp, color = Color.Gray)
                                    
                                    HorizontalDivider()

                                    OutlinedTextField(
                                        value = customName,
                                        onValueChange = { customName = it },
                                        label = { Text("Shop Business Name") },
                                        modifier = Modifier.fillMaxWidth().testTag("profile_shop_name_input"),
                                        singleLine = true
                                    )

                                    OutlinedTextField(
                                        value = customPhone,
                                        onValueChange = { customPhone = it },
                                        label = { Text("Contact Owner Mobile") },
                                        modifier = Modifier.fillMaxWidth().testTag("profile_shop_phone_input"),
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                                    )

                                    OutlinedTextField(
                                        value = customAddress,
                                        onValueChange = { customAddress = it },
                                        label = { Text("Physical Shop Address") },
                                        modifier = Modifier.fillMaxWidth().testTag("profile_shop_address_input"),
                                        singleLine = true
                                    )

                                    Button(
                                        onClick = {
                                            viewModel.updateShopProfile(customName, customPhone, customAddress)
                                            Toast.makeText(context, "Shop Brand Guidelines Saved", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.fillMaxWidth().testTag("save_profile_button")
                                    ) {
                                        Icon(Icons.Default.Save, null)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Save Branding Settings")
                                    }
                                }
                            }
                        }
                        "tab_pin_lock" -> {
                            AppLockContentCard()
                        }
                        "tab_staff_station" -> {
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text("Staff Helper Accounts Authority", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

                                    Button(
                                        onClick = { showStaffManagerDialog = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("open_staff_station_btn")
                                    ) {
                                        Icon(Icons.Default.Engineering, null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Manage Staff", fontWeight = FontWeight.Bold)
                                    }
                                    

                                    
                                    val subUsersList by viewModel.registeredSubUsers.collectAsState()
                                    LaunchedEffect(Unit) {
                                        viewModel.fetchSubUsers()
                                    }

                                    var helperToPendingDelete by remember { mutableStateOf<SubUser?>(null) }

                                    if (subUsersList.isNotEmpty()) {
                                        HorizontalDivider()
                                        Text("Authorized Helpers List:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                subUsersList.forEach { sUser ->
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Text(sUser.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                            Text("ID: ${sUser.emailOrPhone}", fontSize = 11.sp, color = Color.Gray)
                                                        }
                                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                            Text("PIN: ${sUser.passcode}", fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                                            IconButton(
                                                                onClick = {
                                                                    helperToPendingDelete = sUser
                                                                },
                                                                modifier = Modifier.size(32.dp)
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Default.DeleteOutline,
                                                                    contentDescription = "Remove Helper Credentials",
                                                                    tint = MaterialTheme.colorScheme.error,
                                                                    modifier = Modifier.size(18.dp)
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        if (helperToPendingDelete != null) {
                                            val currentHelper = helperToPendingDelete!!
                                            AlertDialog(
                                                onDismissRequest = { helperToPendingDelete = null },
                                                title = { Text("Delete Helper Credentials ✓", fontWeight = FontWeight.Bold) },
                                                text = { Text("Are you sure you want to remove helper credentials for '${currentHelper.name}'? They will no longer be able to sign in.") },
                                                confirmButton = {
                                                    Button(
                                                        onClick = {
                                                            viewModel.deleteSubUser(currentHelper.emailOrPhone) { success, err ->
                                                                if (success) {
                                                                    Toast.makeText(context, "Removed helper ${currentHelper.name}", Toast.LENGTH_SHORT).show()
                                                                } else {
                                                                    Toast.makeText(context, "Failed to remove helper: $err", Toast.LENGTH_LONG).show()
                                                                }
                                                            }
                                                            helperToPendingDelete = null
                                                        },
                                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                                    ) {
                                                        Text("Delete")
                                                    }
                                                },
                                                dismissButton = {
                                                    TextButton(onClick = { helperToPendingDelete = null }) {
                                                        Text("Cancel")
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        "tab_data_share" -> {
                            var showSharingHelpDialog by remember { mutableStateOf(false) }

                            if (showSharingHelpDialog) {
                                AlertDialog(
                                    onDismissRequest = { showSharingHelpDialog = false },
                                    title = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(Icons.Default.HelpOutline, null, tint = MaterialTheme.colorScheme.primary)
                                            Text("Matcher & Sharing Guide")
                                        }
                                    },
                                    text = {
                                        Column(
                                            verticalArrangement = Arrangement.spacedBy(12.dp),
                                            modifier = Modifier.verticalScroll(rememberScrollState())
                                        ) {
                                            Text(
                                                text = "Contact Partner Matcher & Sharing",
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.titleMedium
                                            )
                                            Text(
                                                text = "Connect and share catalog designs, measurements sheets, customer ledgers, or entire backup logs with other bespoke tailors in your local contacts directory. Ensure your phone number is configured correctly in settings to allow registered users to contact you.",
                                                fontSize = 13.sp,
                                                color = Color.Gray
                                            )
                                            
                                            HorizontalDivider()
                                            
                                            Text(
                                                text = "How to Create a Sizing Template Form\nٹیمپلیٹ بنانے کا طریقہ",
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.titleMedium,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = "1. Navigate to the 'Templates' tab from the bottom menu.\n" +
                                                       "2. Tap the '+' (New Template) button at the bottom right corner.\n" +
                                                       "3. Enter a custom name for the form (e.g., Shirt, Kurta, Suit) and click save.\n" +
                                                       "4. Click on your newly created template card to open the editor.\n" +
                                                       "5. Add specific structural parameters/fields (e.g., Length, Collar, Chest, Shoulder) according to your bespoke sizing specifications.\n" +
                                                       "6. Now, whenever you add or update customer measurements, this custom template form will be ready for layout and data logging!",
                                                fontSize = 13.sp,
                                                lineHeight = 18.sp
                                            )
                                        }
                                    },
                                    confirmButton = {
                                        Button(onClick = { showSharingHelpDialog = false }) {
                                            Text("Understood")
                                        }
                                    }
                                )
                            }

                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Contact Matcher & Sharing",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.weight(1f)
                                        )
                                        OutlinedButton(
                                            onClick = { showSharingHelpDialog = true },
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                            modifier = Modifier.height(32.dp)
                                        ) {
                                            Icon(Icons.Default.HelpOutline, null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Guide 📖", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    HorizontalDivider()
                                    ContactMatcherLayout(viewModel, customers)
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            UnifiedJsonBackupCard()
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            // Beautiful Material 3 Styled Native Ad
            AdMobNativeAd()
        }
    }

    if (showStaffManagerDialog) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showStaffManagerDialog = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(onClick = { showStaffManagerDialog = false }, modifier = Modifier.testTag("staff_dialog_back_btn")) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                        Text(
                            text = "Staff & Wage Manager",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    
                    HorizontalDivider()
                    
                    Box(modifier = Modifier.weight(1f).padding(16.dp)) {
                        OwnerControlStationView(
                            viewModel = viewModel,
                            employees = employees,
                            workRecords = workRecords,
                            paymentRecords = paymentRecords,
                            customers = customers
                        )
                    }
                }
            }
        }
    }
}

// ---------------- CUSTOM SWIPE TO REFRESH LAYOUT ----------------

@Composable
fun SwipeToRefreshBox(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val threshold = with(density) { 80.dp.toPx() }
    
    var rawOffset by remember { mutableStateOf(0f) }
    val displayedOffset = if (isRefreshing) with(density) { 48.dp.toPx() } else rawOffset

    val nestedScrollConnection = remember(onRefresh, threshold, isRefreshing) {
        object : androidx.compose.ui.input.nestedscroll.NestedScrollConnection {
            override fun onPostScroll(
                consumed: androidx.compose.ui.geometry.Offset,
                available: androidx.compose.ui.geometry.Offset,
                source: androidx.compose.ui.input.nestedscroll.NestedScrollSource
            ): androidx.compose.ui.geometry.Offset {
                if (isRefreshing) return androidx.compose.ui.geometry.Offset.Zero
                
                if (available.y > 0) {
                    val newOffset = rawOffset + available.y * 0.5f
                    rawOffset = newOffset.coerceAtMost(threshold * 1.5f)
                    return androidx.compose.ui.geometry.Offset(0f, available.y)
                }
                
                if (available.y < 0 && rawOffset > 0) {
                    val newOffset = rawOffset + available.y
                    rawOffset = newOffset.coerceAtLeast(0f)
                    return androidx.compose.ui.geometry.Offset(0f, available.y)
                }
                
                return androidx.compose.ui.geometry.Offset.Zero
            }

            override suspend fun onPostFling(
                consumed: androidx.compose.ui.unit.Velocity,
                available: androidx.compose.ui.unit.Velocity
            ): androidx.compose.ui.unit.Velocity {
                if (rawOffset >= threshold && !isRefreshing) {
                    onRefresh()
                }
                rawOffset = 0f
                return androidx.compose.ui.unit.Velocity.Zero
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(nestedScrollConnection)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationY = displayedOffset
                }
        ) {
            content()
        }

        if (displayedOffset > 0 || isRefreshing) {
            val progress = (displayedOffset / threshold).coerceIn(0f, 1f)
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                shape = CircleShape,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 12.dp)
                    .graphicsLayer {
                        translationY = (displayedOffset * 0.6f).coerceAtMost(with(density) { 40.dp.toPx() })
                        rotationZ = (displayedOffset * 1.8f) % 360f
                        scaleX = if (isRefreshing) 1f else progress
                        scaleY = if (isRefreshing) 1f else progress
                    }
                    .size(40.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (isRefreshing) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Swipe indicator",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

// ---------------- CUSTOM FORMS DIALOG COMPONENT DETAILS ----------------

@Composable
fun AddCustomerDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit,
    viewModel: TailorViewModel,
    isUrduMode: Boolean
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Register bespoke client") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                UrduTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Client Full Name") },
                    modifier = Modifier.fillMaxWidth().testTag("input_customer_name"),
                    singleLine = true,
                    isUrduMode = isUrduMode,
                    viewModel = viewModel
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Client Mobile Line") },
                    modifier = Modifier.fillMaxWidth().testTag("input_customer_phone"),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )
                UrduTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Street Address") },
                    modifier = Modifier.fillMaxWidth().testTag("input_customer_address"),
                    singleLine = true,
                    isUrduMode = isUrduMode,
                    viewModel = viewModel
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && phone.isNotBlank()) {
                        onConfirm(name, phone, address)
                    }
                },
                modifier = Modifier.testTag("submit_customer_button")
            ) {
                Text("Save Client")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AddInventoryDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, Double, Double, Int) -> Unit,
    viewModel: TailorViewModel,
    isUrduMode: Boolean
) {
    var name by remember { mutableStateOf("") }
    var unitType by remember { mutableStateOf("Meters") }
    var buyPrice by remember { mutableStateOf("") }
    var sellPrice by remember { mutableStateOf("") }
    var stock by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Supply Item") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                UrduTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Material Name (e.g. Silk Cloth)") },
                    modifier = Modifier.fillMaxWidth().testTag("input_inventory_name"),
                    singleLine = true,
                    isUrduMode = isUrduMode,
                    viewModel = viewModel
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Unit Standard:")
                    RadioButton(selected = unitType == "Meters", onClick = { unitType = "Meters" })
                    Text("Meters")
                    RadioButton(selected = unitType == "Suits", onClick = { unitType = "Suits" })
                    Text("Suits")
                }

                OutlinedTextField(
                    value = buyPrice,
                    onValueChange = { buyPrice = it },
                    label = { Text("Purchase cost per unit") },
                    modifier = Modifier.fillMaxWidth().testTag("input_inventory_buy"),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )

                OutlinedTextField(
                    value = sellPrice,
                    onValueChange = { sellPrice = it },
                    label = { Text("Selling price with stitching") },
                    modifier = Modifier.fillMaxWidth().testTag("input_inventory_sell"),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )

                OutlinedTextField(
                    value = stock,
                    onValueChange = { stock = it },
                    label = { Text("Stock quantity standard") },
                    modifier = Modifier.fillMaxWidth().testTag("input_inventory_stock"),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val buy = buyPrice.toDoubleOrNull() ?: 0.0
                    val sell = sellPrice.toDoubleOrNull() ?: 0.0
                    val st = stock.toIntOrNull() ?: 0
                    if (name.isNotBlank()) {
                        onConfirm(name, unitType, buy, sell, st)
                    }
                },
                modifier = Modifier.testTag("submit_inventory_button")
            ) {
                Text("Add Stock")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun CreateTemplateDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    viewModel: TailorViewModel,
    isUrduMode: Boolean
) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Spawn Sizing Template") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                UrduTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Template Name (e.g. Kid's Shalwar)") },
                    modifier = Modifier.fillMaxWidth().testTag("input_template_name"),
                    singleLine = true,
                    isUrduMode = isUrduMode,
                    viewModel = viewModel
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(name)
                    }
                },
                modifier = Modifier.testTag("submit_template_button")
            ) {
                Text("Create Card")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun UrduTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    trailingIcon: @Composable (() -> Unit)? = null,
    isUrduMode: Boolean = false,
    viewModel: TailorViewModel,
    placeholder: @Composable (() -> Unit)? = null
) {
    // Represent selection/composition state to ensure seamless phonetic typing if isUrduMode is active
    var lastProcessedText by remember { mutableStateOf("") }
    var textFieldValueState by remember(value) {
        mutableStateOf(
            TextFieldValue(
                text = value,
                selection = TextRange(value.length)
            )
        )
    }

    OutlinedTextField(
        value = textFieldValueState,
        onValueChange = { tfv ->
            if (isUrduMode) {
                val rawText = tfv.text
                val processedText = viewModel.transliteratePhoneticToUrdu(rawText)
                val newSelection = if (processedText != lastProcessedText) {
                    TextRange(processedText.length)
                } else {
                    tfv.selection
                }
                lastProcessedText = processedText
                val newState = tfv.copy(
                    text = processedText,
                    selection = newSelection,
                    composition = null
                )
                textFieldValueState = newState
                onValueChange(processedText)
            } else {
                textFieldValueState = tfv
                onValueChange(tfv.text)
            }
        },
        label = label,
        modifier = modifier.fillMaxWidth(),
        singleLine = singleLine,
        keyboardOptions = keyboardOptions,
        placeholder = placeholder,
        trailingIcon = trailingIcon
    )
}

@Composable
fun AddFieldDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, Boolean, Boolean, String, String, String, String) -> Unit,
    viewModel: TailorViewModel
) {
    var label by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("NUMBER") }
    var options by remember { mutableStateOf("") }
    var isDemand by remember { mutableStateOf(false) }

    var hasSubInput by remember { mutableStateOf(false) }
    var subInputType by remember { mutableStateOf("TEXT") } 
    var subInputLabel by remember { mutableStateOf("") }
    var subInputOptions by remember { mutableStateOf("") }
    var subInputTargetOption by remember { mutableStateOf("") }

    val isUrduMode by viewModel.isUrduSelected.collectAsState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Specification Parameter", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Parameter Classification", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (!isDemand) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                                .border(1.dp, if (!isDemand) MaterialTheme.colorScheme.primary else Color.LightGray.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .clickable { 
                                    isDemand = false 
                                    selectedType = "NUMBER"
                                }
                                .padding(10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.SquareFoot, null, tint = if (!isDemand) MaterialTheme.colorScheme.onPrimaryContainer else Color.Gray, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Fitting Size", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (!isDemand) MaterialTheme.colorScheme.onPrimaryContainer else Color.DarkGray)
                                Text("(e.g., Chest, Waist)", fontSize = 8.sp, color = if (!isDemand) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else Color.Gray)
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isDemand) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                                .border(1.dp, if (isDemand) MaterialTheme.colorScheme.primary else Color.LightGray.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .clickable { 
                                    isDemand = true 
                                    selectedType = "RADIO"
                                }
                                .padding(10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.DesignServices, null, tint = if (isDemand) MaterialTheme.colorScheme.onPrimaryContainer else Color.Gray, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Style Demand", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isDemand) MaterialTheme.colorScheme.onPrimaryContainer else Color.DarkGray)
                                Text("(e.g., Collar type, Pocket)", fontSize = 8.sp, color = if (isDemand) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else Color.Gray)
                            }
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = Color.LightGray.copy(alpha = 0.5f))

                UrduTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text(if (isDemand) "Demand/Style Label (e.g. Cuff Type)" else "Measurement Label (e.g. Chest)") },
                    modifier = Modifier.fillMaxWidth().testTag("input_field_label"),
                    isUrduMode = isUrduMode,
                    viewModel = viewModel
                )

                Text("Display Input Format", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)

                listOf("TEXT", "NUMBER", "RADIO", "CHECKBOX").forEach { type ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedType = type }
                    ) {
                        RadioButton(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            modifier = Modifier.testTag("radio_type_$type")
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(type, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            val descText = when (type) {
                                "NUMBER" -> "Ideal for numerical values"
                                "TEXT" -> "Ideal for arbitrary comments or custom notes"
                                "RADIO" -> "Single choice options (e.g. Slim, Regular)"
                                "CHECKBOX" -> "Yes/No or Multiple choice select options"
                                else -> ""
                            }
                            Text(descText, fontSize = 9.sp, color = Color.Gray)
                        }
                    }
                }

                if (selectedType == "RADIO" || selectedType == "CHECKBOX") {
                    UrduTextField(
                        value = options,
                        onValueChange = { options = it },
                        label = { Text("Available Options (comma-separated)") },
                        placeholder = { Text("e.g. Regular Cuff, French Cuff, Double Cuff") },
                        modifier = Modifier.fillMaxWidth().testTag("input_field_options"),
                        isUrduMode = isUrduMode,
                        viewModel = viewModel
                    )
                }

                Card(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.25f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Checkbox(
                                checked = hasSubInput,
                                onCheckedChange = { hasSubInput = it }
                            )
                            Text("Add Sub-Input / Nested Option", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.tertiary)
                        }

                        if (hasSubInput) {
                            UrduTextField(
                                value = subInputLabel,
                                onValueChange = { subInputLabel = it },
                                label = { Text("Sub-Input Label (e.g. Pocket Depth)") },
                                modifier = Modifier.fillMaxWidth(),
                                isUrduMode = isUrduMode,
                                viewModel = viewModel
                            )

                            Text("Sub-Input Format", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                listOf("TEXT", "NUMBER", "RADIO").forEach { st ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.clickable { subInputType = st }
                                    ) {
                                        RadioButton(
                                            selected = subInputType == st,
                                            onClick = { subInputType = st }
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(st, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                    }
                                }
                            }

                            if (subInputType == "RADIO") {
                                UrduTextField(
                                    value = subInputOptions,
                                    onValueChange = { subInputOptions = it },
                                    label = { Text("Sub Options (comma-separated)") },
                                    placeholder = { Text("e.g. Deep, Shallow, Standard") },
                                    modifier = Modifier.fillMaxWidth(),
                                    isUrduMode = isUrduMode,
                                    viewModel = viewModel
                                )
                            }

                            if (selectedType == "RADIO") {
                                UrduTextField(
                                    value = subInputTargetOption,
                                    onValueChange = { subInputTargetOption = it },
                                    label = { Text("Target Option to trigger sub-input") },
                                    placeholder = { Text("e.g. French Cuff (leave blank for any)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    isUrduMode = isUrduMode,
                                    viewModel = viewModel
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (label.isNotBlank()) {
                        onConfirm(
                            label, 
                            selectedType, 
                            options, 
                            isDemand,
                            hasSubInput,
                            subInputType,
                            subInputLabel,
                            subInputOptions,
                            subInputTargetOption
                        )
                    }
                },
                modifier = Modifier.testTag("submit_field_button")
            ) {
                Text("Embed Specification")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddMeasurementSheetDialog(
    templates: List<SizingTemplate>,
    fields: List<MeasurementField>,
    onDismiss: () -> Unit,
    onConfirmWithValues: (String, String, Map<String, Pair<String, String>>, Map<String, String>) -> Unit,
    viewModel: TailorViewModel,
    isUrduMode: Boolean
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val templatesWithFields = remember(templates, fields) {
        templates.filter { t -> fields.any { it.templateId == t.id } }
    }
    val defaultTemplateId = remember(templatesWithFields) {
        val savedDefaultId = viewModel.getDefaultTemplateId()
        if (savedDefaultId != null && templatesWithFields.any { it.id == savedDefaultId }) {
            savedDefaultId
        } else {
            templatesWithFields.firstOrNull()?.id ?: ""
        }
    }
    var selectedTemplateId by remember(defaultTemplateId) { mutableStateOf(defaultTemplateId) }
    var title by remember { mutableStateOf("") }

    LaunchedEffect(selectedTemplateId) {
        val templateName = templatesWithFields.find { it.id == selectedTemplateId }?.name ?: ""
        title = templateName
    }

    val tempValues = remember { mutableStateMapOf<String, String>() }
    val tempSubValues = remember { mutableStateMapOf<String, String>() }
    val tempNoteValues = remember { mutableStateMapOf<String, String>() }

    // Clear and prepare fresh state fields for the template selection
    LaunchedEffect(selectedTemplateId) {
        tempValues.clear()
        tempSubValues.clear()
        tempNoteValues.clear()
    }

    val templateFields = remember(selectedTemplateId, fields) {
        fields.filter { it.templateId == selectedTemplateId }.distinctBy { it.id }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .padding(4.dp)
                .heightIn(max = 620.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.SquareFoot, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                    Text(
                        text = "Add Sizes & Fabric Rules",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black
                    )
                }

                HorizontalDivider()

                Text("1. Choose Sizing Guideline Template:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)

                if (templatesWithFields.isEmpty()) {
                    Text("No templates with saved measurements available. Define parameters under 'Templates' tab first.", color = Color.Gray, fontSize = 12.sp)
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            templatesWithFields.forEach { template ->
                                val isSelected = selectedTemplateId == template.id
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                        .clickable { selectedTemplateId = template.id }
                                        .border(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = template.name,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                UrduTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Booklet Card Title (defaults to template)") },
                    modifier = Modifier.fillMaxWidth().testTag("input_booklet_title"),
                    singleLine = true,
                    isUrduMode = isUrduMode,
                    viewModel = viewModel
                )

                HorizontalDivider()

                Text("2. Input Measurements:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)

                if (templateFields.isEmpty()) {
                    Text("No structural fields defined in this template. Add fields in the 'Templates' tab.", color = Color.Gray, fontSize = 13.sp)
                } else {
                    val measurementsFields = templateFields.filter { !it.isDemand }
                    val demandsFields = templateFields.filter { it.isDemand }

                    // PART 1: MEASUREMENTS
                    if (measurementsFields.isNotEmpty()) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f))
                                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.SquareFoot, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                Text("Fitting Measurements", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))

                            measurementsFields.forEach { field ->
                                val currentInput = tempValues[field.id] ?: ""
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(field.label, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)

                                    when (field.type) {
                                        "TEXT", "NUMBER" -> {
                                            UrduTextField(
                                                value = currentInput,
                                                onValueChange = {
                                                    tempValues[field.id] = it
                                                },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .testTag("size_input_${field.label.lowercase().replace(" ", "_")}"),
                                                singleLine = true,
                                                keyboardOptions = KeyboardOptions(
                                                    keyboardType = if (field.type == "NUMBER") KeyboardType.Decimal else KeyboardType.Text
                                                ),
                                                isUrduMode = isUrduMode,
                                                viewModel = viewModel
                                            )
                                        }
                                        "RADIO" -> {
                                            val optionList = field.options.split(",").map { it.trim() }
                                            FlowRow(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                optionList.forEach { option ->
                                                     Box(
                                                         modifier = Modifier
                                                             .clip(RoundedCornerShape(16.dp))
                                                             .background(if (currentInput == option) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                                             .clickable {
                                                                 tempValues[field.id] = option
                                                             }
                                                             .border(1.dp, if (currentInput == option) MaterialTheme.colorScheme.primary else Color.LightGray.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                                                             .padding(horizontal = 10.dp, vertical = 6.dp)
                                                     ) {
                                                         Text(option, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (currentInput == option) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
                                                     }
                                                }
                                            }
                                        }
                                        "CHECKBOX" -> {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.clickable {
                                                    val nextVal = if (currentInput == "Yes") "No" else "Yes"
                                                    tempValues[field.id] = nextVal
                                                }
                                            ) {
                                                Checkbox(
                                                    checked = currentInput == "Yes",
                                                    onCheckedChange = { matched ->
                                                        val nextVal = if (matched) "Yes" else "No"
                                                        tempValues[field.id] = nextVal
                                                    }
                                                )
                                                Text("Yes", fontSize = 13.sp)
                                            }
                                        }
                                    }

                                    // Render subfield conditionally if hasSubInput is true
                                    if (field.hasSubInput) {
                                        val isTriggered = field.subInputTargetOption.isBlank() || 
                                                currentInput.equals(field.subInputTargetOption, ignoreCase = true)

                                        if (isTriggered) {
                                            Card(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(top = 8.dp, bottom = 4.dp),
                                                colors = CardDefaults.cardColors(
                                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                                ),
                                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                                            ) {
                                                Column(
                                                    modifier = Modifier.padding(10.dp),
                                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    val subCurrent = tempSubValues[field.id] ?: ""
                                                    Text(
                                                        text = "↳ ${field.subInputLabel}",
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 11.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )

                                                    when (field.subInputType) {
                                                        "TEXT", "NUMBER" -> {
                                                            UrduTextField(
                                                                value = subCurrent,
                                                                onValueChange = {
                                                                    tempSubValues[field.id] = it
                                                                },
                                                                modifier = Modifier.fillMaxWidth(),
                                                                singleLine = true,
                                                                keyboardOptions = KeyboardOptions(
                                                                    keyboardType = if (field.subInputType == "NUMBER") KeyboardType.Decimal else KeyboardType.Text
                                                                ),
                                                                isUrduMode = isUrduMode,
                                                                viewModel = viewModel
                                                            )
                                                        }
                                                        "RADIO" -> {
                                                            val subRadioOpts = field.subInputOptions.split(",").map { it.trim() }
                                                            FlowRow(
                                                                modifier = Modifier.fillMaxWidth(),
                                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                                verticalArrangement = Arrangement.spacedBy(4.dp)
                                                            ) {
                                                                subRadioOpts.forEach { opt ->
                                                                    Box(
                                                                        modifier = Modifier
                                                                            .clip(RoundedCornerShape(8.dp))
                                                                            .background(if (subCurrent == opt) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
                                                                            .clickable {
                                                                                tempSubValues[field.id] = opt
                                                                            }
                                                                            .border(1.dp, if (subCurrent == opt) MaterialTheme.colorScheme.primary else Color.LightGray.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                                                            .padding(horizontal = 8.dp, vertical = 6.dp)
                                                                    ) {
                                                                        Text(opt, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = if (subCurrent == opt) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface)
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // Render note field conditionally if hasNoteInput is true
                                    if (field.hasNoteInput) {
                                        val noteInput = tempNoteValues[field.id] ?: ""
                                        Spacer(modifier = Modifier.height(4.dp))
                                        UrduTextField(
                                            value = noteInput,
                                            onValueChange = {
                                                tempNoteValues[field.id] = it
                                            },
                                            label = { Text("Note (Optional) 📝") },
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true,
                                            isUrduMode = isUrduMode,
                                            viewModel = viewModel
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // PART 2: STYLE DEMANDS
                    if (demandsFields.isNotEmpty()) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.15f))
                                .border(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.DesignServices, null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(16.dp))
                                Text("Style Design Demands", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.tertiary)
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f))

                            demandsFields.forEach { field ->
                                val currentInput = tempValues[field.id] ?: ""
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(field.label, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)

                                    when (field.type) {
                                        "TEXT", "NUMBER" -> {
                                            UrduTextField(
                                                value = currentInput,
                                                onValueChange = {
                                                    tempValues[field.id] = it
                                                },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .testTag("size_input_${field.label.lowercase().replace(" ", "_")}"),
                                                singleLine = true,
                                                keyboardOptions = KeyboardOptions(
                                                    keyboardType = if (field.type == "NUMBER") KeyboardType.Decimal else KeyboardType.Text
                                                ),
                                                isUrduMode = isUrduMode,
                                                viewModel = viewModel
                                            )
                                        }
                                        "RADIO" -> {
                                            val optionList = field.options.split(",").map { it.trim() }
                                            FlowRow(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                optionList.forEach { option ->
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(16.dp))
                                                            .background(if (currentInput == option) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                                            .clickable {
                                                                tempValues[field.id] = option
                                                            }
                                                            .border(1.dp, if (currentInput == option) MaterialTheme.colorScheme.tertiary else Color.LightGray.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                                    ) {
                                                        Text(option, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (currentInput == option) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
                                                    }
                                                }
                                            }
                                        }
                                        "CHECKBOX" -> {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.clickable {
                                                    val nextVal = if (currentInput == "Yes") "No" else "Yes"
                                                    tempValues[field.id] = nextVal
                                                }
                                            ) {
                                                Checkbox(
                                                    checked = currentInput == "Yes",
                                                    onCheckedChange = { matched ->
                                                        val nextVal = if (matched) "Yes" else "No"
                                                        tempValues[field.id] = nextVal
                                                    },
                                                    colors = CheckboxDefaults.colors(
                                                        checkedColor = MaterialTheme.colorScheme.tertiary
                                                    )
                                                )
                                                Text("Yes", fontSize = 13.sp)
                                            }
                                        }
                                    }

                                    // Render subfield conditionally if hasSubInput is true
                                    if (field.hasSubInput) {
                                        val isTriggered = field.subInputTargetOption.isBlank() || 
                                                currentInput.equals(field.subInputTargetOption, ignoreCase = true)

                                        if (isTriggered) {
                                            Card(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(top = 8.dp, bottom = 4.dp),
                                                colors = CardDefaults.cardColors(
                                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                                ),
                                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                                            ) {
                                                Column(
                                                    modifier = Modifier.padding(10.dp),
                                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    val subCurrent = tempSubValues[field.id] ?: ""
                                                    Text(
                                                        text = "↳ ${field.subInputLabel}",
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 11.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )

                                                    when (field.subInputType) {
                                                        "TEXT", "NUMBER" -> {
                                                            UrduTextField(
                                                                value = subCurrent,
                                                                onValueChange = {
                                                                    tempSubValues[field.id] = it
                                                                },
                                                                modifier = Modifier.fillMaxWidth(),
                                                                singleLine = true,
                                                                keyboardOptions = KeyboardOptions(
                                                                    keyboardType = if (field.subInputType == "NUMBER") KeyboardType.Decimal else KeyboardType.Text
                                                                ),
                                                                isUrduMode = isUrduMode,
                                                                viewModel = viewModel
                                                            )
                                                        }
                                                        "RADIO" -> {
                                                            val subRadioOpts = field.subInputOptions.split(",").map { it.trim() }
                                                            FlowRow(
                                                                modifier = Modifier.fillMaxWidth(),
                                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                                verticalArrangement = Arrangement.spacedBy(4.dp)
                                                            ) {
                                                                subRadioOpts.forEach { opt ->
                                                                    Box(
                                                                        modifier = Modifier
                                                                            .clip(RoundedCornerShape(8.dp))
                                                                            .background(if (subCurrent == opt) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
                                                                            .clickable {
                                                                                tempSubValues[field.id] = opt
                                                                            }
                                                                            .border(1.dp, if (subCurrent == opt) MaterialTheme.colorScheme.secondary else Color.LightGray.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                                                            .padding(horizontal = 8.dp, vertical = 6.dp)
                                                                    ) {
                                                                        Text(opt, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = if (subCurrent == opt) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface)
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // Render note field conditionally if hasNoteInput is true
                                    if (field.hasNoteInput) {
                                        val noteInput = tempNoteValues[field.id] ?: ""
                                        Spacer(modifier = Modifier.height(4.dp))
                                        UrduTextField(
                                            value = noteInput,
                                            onValueChange = {
                                                tempNoteValues[field.id] = it
                                            },
                                            label = { Text("Note (Optional) 📝") },
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true,
                                            isUrduMode = isUrduMode,
                                            viewModel = viewModel
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Global Unified Additional Notes field
                val globalNotesInput = tempValues["global_notes_field"] ?: ""
                Spacer(modifier = Modifier.height(6.dp))
                UrduTextField(
                    value = globalNotesInput,
                    onValueChange = {
                        tempValues["global_notes_field"] = it
                    },
                    label = { Text("Additional Notes (Optional) / اضافی نوٹس 📝") },
                    modifier = Modifier.fillMaxWidth().testTag("global_additional_notes_input"),
                    singleLine = false,
                    isUrduMode = isUrduMode,
                    viewModel = viewModel
                )

                HorizontalDivider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            if (selectedTemplateId.isNotBlank() && title.isNotBlank()) {
                                // Package inputs
                                val packagedValues = mutableMapOf<String, Pair<String, String>>()
                                val packagedNotes = mutableMapOf<String, String>()
                                templateFields.forEach { f ->
                                    val mainVal = tempValues[f.id] ?: ""
                                    val subVal = tempSubValues[f.id] ?: ""
                                    val noteVal = tempNoteValues[f.id] ?: ""
                                    if (mainVal.isNotBlank() || subVal.isNotBlank() || noteVal.isNotBlank()) {
                                        packagedValues[f.id] = Pair(mainVal, subVal)
                                        if (noteVal.isNotBlank()) {
                                            packagedNotes[f.id] = noteVal
                                        }
                                    }
                                }
                                val globalNoteVal = tempValues["global_notes_field"] ?: ""
                                if (globalNoteVal.isNotBlank()) {
                                    packagedValues["global_notes_field"] = Pair(globalNoteVal, "")
                                }
                                onConfirmWithValues(selectedTemplateId, title, packagedValues, packagedNotes)
                            }
                        },
                        enabled = selectedTemplateId.isNotBlank() && title.isNotBlank(),
                        modifier = Modifier.weight(1.5f).testTag("submit_booklet_button")
                    ) {
                        Icon(Icons.Default.Save, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save All Sizes")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SizeFormDialog(
    customer: Customer,
    fields: List<MeasurementField>,
    values: List<MeasurementValue>,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String) -> Unit,
    viewModel: TailorViewModel
) {
    val tempValues = remember { mutableStateMapOf<String, String>() }
    val tempSubValues = remember { mutableStateMapOf<String, String>() }
    val tempNoteValues = remember { mutableStateMapOf<String, String>() }

    LaunchedEffect(values) {
        tempValues.clear()
        tempSubValues.clear()
        tempNoteValues.clear()
        values.forEach {
            tempValues[it.fieldId] = it.value
            tempSubValues[it.fieldId] = it.subValue
            tempNoteValues[it.fieldId] = it.note
        }
    }

    val isUrduMode by viewModel.isUrduSelected.collectAsState()

    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .padding(4.dp)
                .heightIn(max = 620.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.SquareFoot, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                    Text(
                        text = "Edit Client Sizes",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black
                    )
                }
                Text(
                    text = "Customer: ${customer.name}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    fontWeight = FontWeight.SemiBold
                )

                HorizontalDivider()

                if (fields.isEmpty()) {
                    Text("No structural rules defined in this template. Expand 'Templates' tab to add items first.", color = Color.Gray, fontSize = 13.sp)
                } else {
                    val distinctFields = fields.distinctBy { it.id }
                    val measurementsFields = distinctFields.filter { !it.isDemand }
                    val demandsFields = distinctFields.filter { it.isDemand }

                    // PART 1: MEASUREMENTS
                    if (measurementsFields.isNotEmpty()) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f))
                                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.SquareFoot, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                Text("Fitting Measurements", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                            
                            measurementsFields.forEach { field ->
                                val currentInput = tempValues[field.id] ?: ""
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(field.label, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                                    
                                    when (field.type) {
                                        "TEXT", "NUMBER" -> {
                                            UrduTextField(
                                                value = currentInput,
                                                onValueChange = {
                                                    tempValues[field.id] = it
                                                    onSave(field.id, it, tempSubValues[field.id] ?: "", tempNoteValues[field.id] ?: "")
                                                },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .testTag("size_input_${field.label.lowercase().replace(" ", "_")}"),
                                                singleLine = true,
                                                keyboardOptions = KeyboardOptions(
                                                    keyboardType = if (field.type == "NUMBER") KeyboardType.Decimal else KeyboardType.Text
                                                ),
                                                isUrduMode = isUrduMode,
                                                viewModel = viewModel
                                            )
                                        }
                                        "RADIO" -> {
                                            val optionList = field.options.split(",").map { it.trim() }
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                optionList.forEach { option ->
                                                    AssistChip(
                                                        onClick = {
                                                            tempValues[field.id] = option
                                                            onSave(field.id, option, tempSubValues[field.id] ?: "", tempNoteValues[field.id] ?: "")
                                                        },
                                                        label = { Text(option) },
                                                        colors = if (currentInput == option) {
                                                            AssistChipDefaults.assistChipColors(
                                                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                                                labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                                            )
                                                        } else {
                                                            AssistChipDefaults.assistChipColors()
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                        "CHECKBOX" -> {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.clickable {
                                                    val nextVal = if (currentInput == "Yes") "No" else "Yes"
                                                    tempValues[field.id] = nextVal
                                                    onSave(field.id, nextVal, tempSubValues[field.id] ?: "", tempNoteValues[field.id] ?: "")
                                                }
                                            ) {
                                                Checkbox(
                                                    checked = currentInput == "Yes",
                                                    onCheckedChange = { matched ->
                                                        val nextVal = if (matched) "Yes" else "No"
                                                        tempValues[field.id] = nextVal
                                                        onSave(field.id, nextVal, tempSubValues[field.id] ?: "", tempNoteValues[field.id] ?: "")
                                                    }
                                                )
                                                Text("Yes", fontSize = 13.sp)
                                            }
                                        }
                                    }

                                    // Render subfield conditionally if hasSubInput is true
                                    if (field.hasSubInput) {
                                        val isTriggered = field.subInputTargetOption.isBlank() || 
                                                currentInput.equals(field.subInputTargetOption, ignoreCase = true)

                                        if (isTriggered) {
                                            Card(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(top = 8.dp, bottom = 4.dp),
                                                colors = CardDefaults.cardColors(
                                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                                ),
                                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                                            ) {
                                                Column(
                                                    modifier = Modifier.padding(10.dp),
                                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    val subCurrent = tempSubValues[field.id] ?: ""
                                                    Text(
                                                        text = "↳ ${field.subInputLabel}",
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 11.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )

                                                    when (field.subInputType) {
                                                        "TEXT", "NUMBER" -> {
                                                            UrduTextField(
                                                                value = subCurrent,
                                                                onValueChange = {
                                                                    tempSubValues[field.id] = it
                                                                    onSave(field.id, currentInput, it, tempNoteValues[field.id] ?: "")
                                                                },
                                                                modifier = Modifier.fillMaxWidth(),
                                                                singleLine = true,
                                                                keyboardOptions = KeyboardOptions(
                                                                    keyboardType = if (field.subInputType == "NUMBER") KeyboardType.Decimal else KeyboardType.Text
                                                                ),
                                                                isUrduMode = isUrduMode,
                                                                viewModel = viewModel
                                                            )
                                                        }
                                                        "RADIO" -> {
                                                            val subRadioOpts = field.subInputOptions.split(",").map { it.trim() }
                                                            FlowRow(
                                                                modifier = Modifier.fillMaxWidth(),
                                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                                verticalArrangement = Arrangement.spacedBy(4.dp)
                                                            ) {
                                                                subRadioOpts.forEach { opt ->
                                                                    Box(
                                                                        modifier = Modifier
                                                                            .clip(RoundedCornerShape(8.dp))
                                                                            .background(if (subCurrent == opt) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
                                                                            .clickable {
                                                                                tempSubValues[field.id] = opt
                                                                                onSave(field.id, currentInput, opt, tempNoteValues[field.id] ?: "")
                                                                            }
                                                                            .border(1.dp, if (subCurrent == opt) MaterialTheme.colorScheme.primary else Color.LightGray.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                                                            .padding(horizontal = 8.dp, vertical = 6.dp)
                                                                    ) {
                                                                        Text(opt, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = if (subCurrent == opt) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface)
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // Render note field conditionally if hasNoteInput is true
                                    if (field.hasNoteInput) {
                                        val noteInput = tempNoteValues[field.id] ?: ""
                                        Spacer(modifier = Modifier.height(4.dp))
                                        UrduTextField(
                                            value = noteInput,
                                            onValueChange = {
                                                tempNoteValues[field.id] = it
                                                onSave(field.id, currentInput, tempSubValues[field.id] ?: "", it)
                                            },
                                            label = { Text("Note (Optional) 📝") },
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true,
                                            isUrduMode = isUrduMode,
                                            viewModel = viewModel
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // PART 2: STYLE DEMANDS
                    if (demandsFields.isNotEmpty()) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.15f))
                                .border(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.DesignServices, null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(16.dp))
                                Text("Style Design Demands", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.tertiary)
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f))

                            demandsFields.forEach { field ->
                                val currentInput = tempValues[field.id] ?: ""
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(field.label, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)

                                    when (field.type) {
                                        "TEXT", "NUMBER" -> {
                                            UrduTextField(
                                                value = currentInput,
                                                onValueChange = {
                                                    tempValues[field.id] = it
                                                    onSave(field.id, it, tempSubValues[field.id] ?: "", tempNoteValues[field.id] ?: "")
                                                },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .testTag("size_input_${field.label.lowercase().replace(" ", "_")}"),
                                                singleLine = true,
                                                keyboardOptions = KeyboardOptions(
                                                    keyboardType = if (field.type == "NUMBER") KeyboardType.Decimal else KeyboardType.Text
                                                ),
                                                isUrduMode = isUrduMode,
                                                viewModel = viewModel
                                            )
                                        }
                                        "RADIO" -> {
                                            val optionList = field.options.split(",").map { it.trim() }
                                            FlowRow(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                optionList.forEach { option ->
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(16.dp))
                                                            .background(if (currentInput == option) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                                            .clickable {
                                                                tempValues[field.id] = option
                                                                onSave(field.id, option, tempSubValues[field.id] ?: "", tempNoteValues[field.id] ?: "")
                                                            }
                                                            .border(1.dp, if (currentInput == option) MaterialTheme.colorScheme.tertiary else Color.LightGray.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                                    ) {
                                                        Text(option, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (currentInput == option) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
                                                    }
                                                }
                                            }
                                        }
                                        "CHECKBOX" -> {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.clickable {
                                                    val nextVal = if (currentInput == "Yes") "No" else "Yes"
                                                    tempValues[field.id] = nextVal
                                                    onSave(field.id, nextVal, tempSubValues[field.id] ?: "", tempNoteValues[field.id] ?: "")
                                                }
                                            ) {
                                                Checkbox(
                                                    checked = currentInput == "Yes",
                                                    onCheckedChange = { matched ->
                                                        val nextVal = if (matched) "Yes" else "No"
                                                        tempValues[field.id] = nextVal
                                                        onSave(field.id, nextVal, tempSubValues[field.id] ?: "", tempNoteValues[field.id] ?: "")
                                                    },
                                                    colors = CheckboxDefaults.colors(
                                                        checkedColor = MaterialTheme.colorScheme.tertiary
                                                    )
                                                )
                                                Text("Yes", fontSize = 13.sp)
                                            }
                                        }
                                    }

                                    // Render subfield conditionally if hasSubInput is true
                                    if (field.hasSubInput) {
                                        val isTriggered = field.subInputTargetOption.isBlank() || 
                                                currentInput.equals(field.subInputTargetOption, ignoreCase = true)

                                        if (isTriggered) {
                                            Card(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(top = 8.dp, bottom = 4.dp),
                                                colors = CardDefaults.cardColors(
                                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                                ),
                                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                                            ) {
                                                Column(
                                                    modifier = Modifier.padding(10.dp),
                                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    val subCurrent = tempSubValues[field.id] ?: ""
                                                    Text(
                                                        text = "↳ ${field.subInputLabel}",
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 11.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )

                                                    when (field.subInputType) {
                                                        "TEXT", "NUMBER" -> {
                                                            UrduTextField(
                                                                value = subCurrent,
                                                                onValueChange = {
                                                                    tempSubValues[field.id] = it
                                                                    onSave(field.id, currentInput, it, tempNoteValues[field.id] ?: "")
                                                                },
                                                                modifier = Modifier.fillMaxWidth(),
                                                                singleLine = true,
                                                                keyboardOptions = KeyboardOptions(
                                                                    keyboardType = if (field.subInputType == "NUMBER") KeyboardType.Decimal else KeyboardType.Text
                                                                ),
                                                                isUrduMode = isUrduMode,
                                                                viewModel = viewModel
                                                            )
                                                        }
                                                        "RADIO" -> {
                                                            val subRadioOpts = field.subInputOptions.split(",").map { it.trim() }
                                                            FlowRow(
                                                                modifier = Modifier.fillMaxWidth(),
                                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                                verticalArrangement = Arrangement.spacedBy(4.dp)
                                                            ) {
                                                                subRadioOpts.forEach { opt ->
                                                                    Box(
                                                                        modifier = Modifier
                                                                            .clip(RoundedCornerShape(8.dp))
                                                                            .background(if (subCurrent == opt) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
                                                                            .clickable {
                                                                                tempSubValues[field.id] = opt
                                                                                onSave(field.id, currentInput, opt, tempNoteValues[field.id] ?: "")
                                                                            }
                                                                            .border(1.dp, if (subCurrent == opt) MaterialTheme.colorScheme.secondary else Color.LightGray.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                                                            .padding(horizontal = 8.dp, vertical = 6.dp)
                                                                    ) {
                                                                        Text(opt, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = if (subCurrent == opt) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface)
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // Render note field conditionally if hasNoteInput is true
                                    if (field.hasNoteInput) {
                                        val noteInput = tempNoteValues[field.id] ?: ""
                                        Spacer(modifier = Modifier.height(4.dp))
                                        UrduTextField(
                                            value = noteInput,
                                            onValueChange = {
                                                tempNoteValues[field.id] = it
                                                onSave(field.id, currentInput, tempSubValues[field.id] ?: "", it)
                                            },
                                            label = { Text("Note (Optional) 📝") },
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true,
                                            isUrduMode = isUrduMode,
                                            viewModel = viewModel
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Global Unified Additional Notes field for Edit Dialog
                val globalNotesInput = tempValues["global_notes_field"] ?: ""
                Spacer(modifier = Modifier.height(6.dp))
                UrduTextField(
                    value = globalNotesInput,
                    onValueChange = {
                        tempValues["global_notes_field"] = it
                        onSave("global_notes_field", it, "", "")
                    },
                    label = { Text("Additional Notes (Optional) / اضافی نوٹس 📝") },
                    modifier = Modifier.fillMaxWidth().testTag("global_additional_notes_edit"),
                    singleLine = false,
                    isUrduMode = isUrduMode,
                    viewModel = viewModel
                )

                HorizontalDivider()

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("close_size_form")
                ) {
                    Text("Ready & Saved")
                }
            }
        }
    }
}

@Composable
fun EditLedgerDialog(
    customer: Customer,
    ledger: LedgerRecord?,
    onDismiss: () -> Unit,
    onConfirm: (Double, Double) -> Unit
) {
    var totalText by remember { mutableStateOf(ledger?.totalValue?.toString() ?: "0.0") }
    var paidText by remember { mutableStateOf(ledger?.amountPaid?.toString() ?: "0.0") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Update Bill (${customer.name})") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = totalText,
                    onValueChange = { totalText = it },
                    label = { Text("Total Bill Value") },
                    modifier = Modifier.fillMaxWidth().testTag("ledger_total_input"),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )

                OutlinedTextField(
                    value = paidText,
                    onValueChange = { paidText = it },
                    label = { Text("Amount Paid") },
                    modifier = Modifier.fillMaxWidth().testTag("ledger_paid_input"),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val tot = totalText.toDoubleOrNull() ?: 0.0
                    val paid = paidText.toDoubleOrNull() ?: 0.0
                    onConfirm(tot, paid)
                },
                modifier = Modifier.testTag("submit_ledger_button")
            ) {
                Text("Modify Bill")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun LinkMaterialDialog(
    inventory: List<InventoryItem>,
    onDismiss: () -> Unit,
    onConfirm: (InventoryItem, Int) -> Unit
) {
    var selectedItem by remember { mutableStateOf<InventoryItem?>(null) }
    var quantityText by remember { mutableStateOf("1") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Use Material Stock") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.heightIn(max = 400.dp)
            ) {
                Text("Select material to link to this order:", fontWeight = FontWeight.SemiBold)
                
                if (inventory.isEmpty()) {
                    Text("No inventory materials in stock. Register materials in 'Inventory' tab first.", color = Color.Gray, fontSize = 13.sp)
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(inventory) { item ->
                            val isSelected = selectedItem?.id == item.id
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                border = BorderStroke(1.dp, Color.LightGray),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedItem = item }
                                    .testTag("link_item_${item.name.lowercase().replace(" ", "_")}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(item.name, fontWeight = FontWeight.Bold)
                                    Text("Stock: ${item.stockQuantity} (${item.unitType})")
                                }
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = quantityText,
                    onValueChange = { quantityText = it },
                    label = { Text("Quantity of material used") },
                    modifier = Modifier.fillMaxWidth().testTag("link_quantity_input"),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val item = selectedItem
                    val qty = quantityText.toIntOrNull() ?: 1
                    if (item != null) {
                        onConfirm(item, qty)
                    }
                },
                enabled = selectedItem != null,
                modifier = Modifier.testTag("submit_link_material_button")
            ) {
                Text("Deduct & Add to Bill")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AddPartialPaymentDialog(
    customer: Customer,
    onDismiss: () -> Unit,
    onConfirm: (Double, String) -> Unit,
    viewModel: TailorViewModel,
    isUrduMode: Boolean
) {
    var paymentAmountText by remember { mutableStateOf("") }
    var paymentNoteText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Receipt / Partial Payment (${customer.name})") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = paymentAmountText,
                    onValueChange = { paymentAmountText = it },
                    label = { Text("Payment/Receipt Amount") },
                    modifier = Modifier.fillMaxWidth().testTag("payment_amount_input"),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )

                UrduTextField(
                    value = paymentNoteText,
                    onValueChange = { paymentNoteText = it },
                    label = { Text("Note / Description") },
                    placeholder = { Text("e.g. Cash, Advance, Final installment") },
                    modifier = Modifier.fillMaxWidth().testTag("payment_note_input"),
                    singleLine = true,
                    isUrduMode = isUrduMode,
                    viewModel = viewModel
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = paymentAmountText.toDoubleOrNull() ?: 0.0
                    onConfirm(amount, paymentNoteText.trim())
                },
                modifier = Modifier.testTag("submit_payment_button")
            ) {
                Text("Post Payment")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AddOrderDialog(
    customer: Customer,
    inventory: List<InventoryItem>,
    onDismiss: () -> Unit,
    onConfirm: (String, Double, Double, String, String?, Int) -> Unit,
    viewModel: TailorViewModel,
    isUrduMode: Boolean
) {
    var orderNameText by remember { mutableStateOf("") }
    var priceText by remember { mutableStateOf("") }
    var advanceText by remember { mutableStateOf("") }
    var noteText by remember { mutableStateOf("") }
    var selectedInventoryItem by remember { mutableStateOf<InventoryItem?>(null) }
    var showInventorySelect by remember { mutableStateOf(false) }
    var qty by remember { mutableStateOf(1) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Customer Order (${customer.name})") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth()
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Source Material / Product Selection", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { showInventorySelect = true },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text("Select Stock Item", fontSize = 11.sp)
                            }
                            OutlinedButton(
                                onClick = {
                                    selectedInventoryItem = null
                                    orderNameText = ""
                                    priceText = ""
                                    qty = 1
                                },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text("Custom Sewing", fontSize = 11.sp)
                            }
                        }

                        selectedInventoryItem?.let { item ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Selected product: ${item.name}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            Text("Stock left: ${item.stockQuantity} ${item.unitType}", fontSize = 11.sp, color = Color.Gray)
                                        }
                                        IconButton(onClick = { 
                                            selectedInventoryItem = null
                                            qty = 1
                                        }) {
                                            Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp))
                                        }
                                    }

                                    // Units/Quantity Selector
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Selected Units (${item.unitType}):", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            IconButton(
                                                onClick = { if (qty > 1) { qty--; priceText = (item.sellingPrice * qty).toString() } },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Text("-", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                            }
                                            Text("$qty", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            IconButton(
                                                onClick = { if (qty < item.stockQuantity) { qty++; priceText = (item.sellingPrice * qty).toString() } },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Text("+", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                UrduTextField(
                    value = orderNameText,
                    onValueChange = { orderNameText = it },
                    label = { Text("Name of Work / Item") },
                    placeholder = { Text("e.g. Bridal Dress Stitching") },
                    modifier = Modifier.fillMaxWidth().testTag("order_name_input"),
                    singleLine = true,
                    isUrduMode = isUrduMode,
                    viewModel = viewModel
                )

                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it },
                    label = { Text("Price/Cost of Work") },
                    modifier = Modifier.fillMaxWidth().testTag("order_price_input"),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )

                OutlinedTextField(
                    value = advanceText,
                    onValueChange = { advanceText = it },
                    label = { Text("Advance Paid Today") },
                    placeholder = { Text("Leave empty for unpaid/pending") },
                    modifier = Modifier.fillMaxWidth().testTag("order_advance_input"),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )

                // 📊 LIVE LEDGER MATHEMATICS FORECAST CARD (English & Urdu)
                val priceNum = priceText.toDoubleOrNull() ?: selectedInventoryItem?.let { it.sellingPrice * qty } ?: 0.0
                val advanceNum = advanceText.toDoubleOrNull() ?: 0.0
                val pendingNum = priceNum - advanceNum

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (pendingNum > 0.0) Color(0xFFFFEBEE) else Color(0xFFE8F5E9)
                    ),
                    border = BorderStroke(
                        1.dp, 
                        if (pendingNum > 0.0) Color(0xFFFFCDD2) else Color(0xFFC8E6C9)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "📊 Ledger Account Calculation (کھاتہ کا حساب کتاب):",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = if (pendingNum > 0.0) Color(0xFFC62828) else Color(0xFF2E7D32)
                        )
                        HorizontalDivider(
                            color = (if (pendingNum > 0.0) Color(0xFFC62828) else Color(0xFF2E7D32)).copy(alpha = 0.15f),
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total Price (کل قیمت):", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            Text(String.format(Locale.getDefault(), "%.2f", priceNum), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Advance Received (ایڈوانس وصولی):", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            Text("- ${String.format(Locale.getDefault(), "%.2f", advanceNum)}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                        }
                        HorizontalDivider(color = Color.Gray.copy(alpha = 0.1f))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Added to Customer Debt (بقایا قرض):", 
                                fontSize = 12.sp, 
                                fontWeight = FontWeight.Black,
                                color = if (pendingNum > 0.0) Color(0xFFC62828) else Color(0xFF2E7D32)
                            )
                            Text(
                                String.format(Locale.getDefault(), "%.2f", pendingNum), 
                                fontSize = 12.sp, 
                                fontWeight = FontWeight.Black,
                                color = if (pendingNum > 0.0) Color(0xFFC62828) else Color(0xFF2E7D32)
                            )
                        }
                        Text(
                            text = if (pendingNum > 0.0) {
                                "💡 Outstanding debt of ${String.format(Locale.getDefault(), "%.2f", pendingNum)} will be added to this customer's active ledger profile."
                            } else {
                                "💡 This order is fully paid. No debt will be added to the customer ledger."
                            },
                            fontSize = 10.sp,
                            color = Color.DarkGray,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        Text(
                            text = if (pendingNum > 0.0) {
                                "گاہک کے کھاتہ میں ${String.format(Locale.getDefault(), "%.2f", pendingNum)} کا بقایا قرض جمع کر دیا جائے گا۔"
                            } else {
                                "یہ آرڈر مکمل ادا شدہ ہے۔ مطلع کھاتہ میں کوئی اضافہ نہیں ہو گا۔"
                            },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.DarkGray
                        )
                    }
                }

                UrduTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("Order Note / Details") },
                    placeholder = { Text("e.g. Silk cloth thread decoration details") },
                    modifier = Modifier.fillMaxWidth().testTag("order_note_input"),
                    singleLine = true,
                    isUrduMode = isUrduMode,
                    viewModel = viewModel
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalName = orderNameText.trim().ifEmpty { selectedInventoryItem?.let { "Product: ${it.name}" } ?: "Custom Sew Order" }
                    val finalPrice = priceText.toDoubleOrNull() ?: selectedInventoryItem?.let { it.sellingPrice * qty } ?: 0.0
                    val finalPaid = advanceText.toDoubleOrNull() ?: 0.0
                    onConfirm(finalName, finalPrice, finalPaid, noteText.trim(), selectedInventoryItem?.id, qty)
                },
                modifier = Modifier.testTag("submit_order_button")
            ) {
                Text("Create & Post")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )

    if (showInventorySelect) {
        AlertDialog(
            onDismissRequest = { showInventorySelect = false },
            title = { Text("Select Supply Item") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .fillMaxWidth()
                ) {
                    if (inventory.isEmpty()) {
                        Text("No stock supplies in inventory to choose.", fontSize = 13.sp, color = Color.Gray)
                    } else {
                        inventory.forEach { item ->
                            Card(
                                onClick = {
                                    selectedInventoryItem = item
                                    orderNameText = "Product: ${item.name}"
                                    qty = 1
                                    priceText = item.sellingPrice.toString()
                                    showInventorySelect = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(item.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("Stock: ${item.stockQuantity} | Cost: ${String.format(Locale.getDefault(), "%.2f", item.sellingPrice)}", fontSize = 11.sp, color = Color.Gray)
                                    }
                                    Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showInventorySelect = false }) {
                    Text("Close")
                }
            }
        )
    }
}

fun generatePdfAndShare(context: android.content.Context, contentText: String, customerName: String) {
    try {
        val pdfDocument = android.graphics.pdf.PdfDocument()
        val lines = contentText.split("\n")
        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 10f
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.NORMAL)
        }
        
        val lineHeight = 14f
        val margin = 36f
        val usableHeight = 842f - (margin * 2)
        val linesPerPage = (usableHeight / lineHeight).toInt()
        val chunkedLines = lines.chunked(linesPerPage)

        for ((index, pageLines) in chunkedLines.withIndex()) {
            val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, index + 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas
            
            // Draw neat border
            val borderPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.GRAY
                style = android.graphics.Paint.Style.STROKE
                strokeWidth = 1f
            }
            canvas.drawRect(20f, 20f, 575f, 822f, borderPaint)

            var y = margin + 15f
            for (line in pageLines) {
                canvas.drawText(line, margin, y, paint)
                y += lineHeight
            }
            pdfDocument.finishPage(page)
        }

        val cacheFile = java.io.File(context.cacheDir, "tailor_receipt_${System.currentTimeMillis()}.pdf")
        val outputStream = java.io.FileOutputStream(cacheFile)
        pdfDocument.writeTo(outputStream)
        pdfDocument.close()
        outputStream.close()

        val authority = "${context.packageName}.fileprovider"
        val uri = androidx.core.content.FileProvider.getUriForFile(context, authority, cacheFile)
        
        val shareIntent = android.content.Intent().apply {
            action = android.content.Intent.ACTION_SEND
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            type = "application/pdf"
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(android.content.Intent.createChooser(shareIntent, "Save or Share Statement PDF"))
    } catch(e: Exception) {
        e.printStackTrace()
        android.widget.Toast.makeText(context, "Error generating PDF: ${e.localizedMessage}", android.widget.Toast.LENGTH_LONG).show()
    }
}

fun doPrintReceipt(context: android.content.Context, bitmap: android.graphics.Bitmap, jobName: String) {
    try {
        val printManager = context.getSystemService(android.content.Context.PRINT_SERVICE) as? android.print.PrintManager
        if (printManager == null) {
            android.widget.Toast.makeText(context, "Printing not supported on this device", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        
        val printAdapter = object : android.print.PrintDocumentAdapter() {
            override fun onLayout(
                oldAttributes: android.print.PrintAttributes?,
                newAttributes: android.print.PrintAttributes?,
                cancellationSignal: android.os.CancellationSignal?,
                callback: LayoutResultCallback?,
                extras: android.os.Bundle?
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback?.onLayoutCancelled()
                    return
                }
                
                val info = android.print.PrintDocumentInfo.Builder(jobName)
                    .setContentType(android.print.PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(1)
                    .build()
                
                callback?.onLayoutFinished(info, true)
            }

            override fun onWrite(
                pages: Array<out android.print.PageRange>?,
                destination: android.os.ParcelFileDescriptor?,
                cancellationSignal: android.os.CancellationSignal?,
                callback: WriteResultCallback?
            ) {
                var output: java.io.FileOutputStream? = null
                val pdfDocument = android.graphics.pdf.PdfDocument()
                try {
                    val printWidth = 595
                    val scale = printWidth.toFloat() / bitmap.width.toFloat()
                    val printHeight = (bitmap.height * scale).toInt()
                    
                    val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(printWidth, printHeight, 1).create()
                    val page = pdfDocument.startPage(pageInfo)
                    val canvas = page.canvas
                    
                    val rect = android.graphics.Rect(0, 0, printWidth, printHeight)
                    canvas.drawBitmap(bitmap, null, rect, android.graphics.Paint().apply { isFilterBitmap = true })
                    
                    pdfDocument.finishPage(page)
                    
                    output = java.io.FileOutputStream(destination?.fileDescriptor)
                    pdfDocument.writeTo(output)
                    callback?.onWriteFinished(arrayOf(android.print.PageRange.ALL_PAGES))
                } catch (e: Exception) {
                    callback?.onWriteFailed(e.toString())
                } finally {
                    pdfDocument.close()
                    try {
                        output?.close()
                    } catch (_: Exception) {}
                }
            }
        }
        
        printManager.print(jobName, printAdapter, android.print.PrintAttributes.Builder().build())
    } catch (e: Exception) {
        e.printStackTrace()
        android.widget.Toast.makeText(context, "Print Error: ${e.localizedMessage}", android.widget.Toast.LENGTH_LONG).show()
    }
}

@Composable
fun AppLockScreen(
    correctPin: String,
    isBiometricEnabled: Boolean,
    registeredEmail: String,
    isSubUser: Boolean = false,
    onUnlocked: () -> Unit
) {
    var enteredPin by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    var showRecoveryDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val biometricManager = remember(context) { BiometricManager.from(context) }
    val isBiometricHardwareAvailable = remember(biometricManager) {
        biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) != BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE
    }

    // Function to launch the real system biometric verification
    val launchBiometricAuthentication = {
        if (activity != null && isBiometricHardwareAvailable) {
            val executor = ContextCompat.getMainExecutor(context)
            val biometricPrompt = BiometricPrompt(
                activity,
                executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        super.onAuthenticationError(errorCode, errString)
                        if (errorCode != BiometricPrompt.ERROR_USER_CANCELED && errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                            Toast.makeText(context, "Biometric Security info: $errString", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)
                        Toast.makeText(context, "Identity verified successfully", Toast.LENGTH_SHORT).show()
                        onUnlocked()
                    }

                    override fun onAuthenticationFailed() {
                        super.onAuthenticationFailed()
                        Toast.makeText(context, "Fingerprint scan failed. Try again.", Toast.LENGTH_SHORT).show()
                    }
                }
            )

            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Workspace Biometric Lock")
                .setSubtitle("Confirm enrolled fingerprint to unlock client ledger databases")
                .setNegativeButtonText("Use Passcode PIN")
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                .build()

            try {
                biometricPrompt.authenticate(promptInfo)
            } catch (e: Exception) {
                Toast.makeText(context, "Biometric Launch Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Auto-launch on screen start if biometric scanning is enabled in user settings
    LaunchedEffect(isBiometricEnabled, isBiometricHardwareAvailable) {
        if (isBiometricEnabled && isBiometricHardwareAvailable) {
            launchBiometricAuthentication()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize().testTag("app_lock_overlay"),
        color = Color(0xFF12141C)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header / Title Space
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 48.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Lock Icon",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Tailor Workspace Locked",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Provide security credentials to resume records query",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.LightGray,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }

            // Pin Indicators Space
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val length = if (correctPin.isBlank()) 4 else correctPin.length
                    for (i in 0 until length) {
                        val isFilled = i < enteredPin.length
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .background(
                                    color = if (showError) MaterialTheme.colorScheme.error 
                                            else if (isFilled) MaterialTheme.colorScheme.primary 
                                            else Color.DarkGray,
                                    shape = CircleShape
                                )
                                .border(1.dp, Color.Gray, CircleShape)
                        )
                    }
                }

                if (showError) {
                    Text(
                        text = "Invalid credentials. Try again.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Keypad Grid Space
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val keys = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("BIO", "0", "DEL")
                )

                keys.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(0.85f),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        row.forEach { key ->
                            val isSpecial = key == "BIO" || key == "DEL"

                            if (key == "BIO" && (!isBiometricEnabled || !isBiometricHardwareAvailable)) {
                                Spacer(modifier = Modifier.size(64.dp))
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .background(
                                            color = if (isSpecial) Color.Transparent else Color(0xFF20232E),
                                            shape = CircleShape
                                        )
                                        .clickable {
                                            showError = false
                                            when (key) {
                                                "DEL" -> {
                                                    if (enteredPin.isNotEmpty()) {
                                                        enteredPin = enteredPin.dropLast(1)
                                                    }
                                                }
                                                "BIO" -> {
                                                    if (isBiometricEnabled && isBiometricHardwareAvailable) {
                                                        launchBiometricAuthentication()
                                                    }
                                                }
                                                else -> {
                                                    val requiredLength = if (correctPin.isBlank()) 4 else correctPin.length
                                                    if (enteredPin.length < requiredLength) {
                                                        enteredPin += key
                                                        if (enteredPin == correctPin) {
                                                            onUnlocked()
                                                        } else if (enteredPin.length == requiredLength) {
                                                            showError = true
                                                            enteredPin = ""
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        .testTag("keypad_$key"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    when (key) {
                                        "DEL" -> {
                                            Icon(
                                                imageVector = Icons.Default.Backspace,
                                                contentDescription = "Backspace",
                                                tint = Color.White,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                        "BIO" -> {
                                            Icon(
                                                imageVector = Icons.Default.Fingerprint,
                                                contentDescription = "Scan Fingerprint",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }
                                        else -> {
                                            Text(
                                                text = key,
                                                style = MaterialTheme.typography.titleLarge,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (isSubUser) {
                    Text(
                        text = "Forgot PIN? Ask your administrator physically.",
                        color = Color.Gray,
                        fontWeight = FontWeight.Normal,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(8.dp).testTag("forgot_pin_label_helper")
                    )
                } else {
                    TextButton(
                        onClick = { showRecoveryDialog = true },
                        modifier = Modifier.testTag("forgot_pin_btn")
                    ) {
                        Text(
                            text = "Forgot Passcode? Email Recovery PIN",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    if (showRecoveryDialog) {
        AlertDialog(
            onDismissRequest = { showRecoveryDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Email,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                )
            },
            title = { Text("📬 Pin Recovery Sent Successfully!") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("An automated password dispatch was successfully relayed via electronic cloud services to your registered profile ID:", fontSize = 13.sp)
                    Text(registeredEmail, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
                    HorizontalDivider()
                    Text("💡 SUPPORT INCOMING DETAIL MESSAGE HEADER:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.Gray)
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "To: $registeredEmail\nFrom: info@tailormaster.io\nSubject: Security Recovery Passcode PIN\n\nYour retrieved workspace LOCK CODE is: $correctPin",
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showRecoveryDialog = false },
                    modifier = Modifier.testTag("recovery_dialog_dismiss")
                ) {
                    Text("Return to Locker Keypad")
                }
            },
            modifier = Modifier.testTag("recovery_pin_dialog")
        )
    }
}

@Composable
fun TemplateEditorFormDialog(
    templateId: String,
    templates: List<SizingTemplate>,
    fields: List<MeasurementField>,
    onDismiss: () -> Unit,
    onAddField: (String, String, String, String, Boolean, Boolean, String, String, String, String, Boolean) -> Unit,
    onDeleteField: (String) -> Unit,
    onDeleteTemplate: (String) -> Unit,
    viewModel: TailorViewModel,
    isUrduMode: Boolean
) {
    val template = templates.find { it.id == templateId } ?: return
    val context = LocalContext.current
    var label by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("NUMBER") }
    var options by remember { mutableStateOf("") }
    var isDemand by remember { mutableStateOf(false) }

    var hasSubInput by remember { mutableStateOf(false) }
    var subInputType by remember { mutableStateOf("TEXT") } 
    var subInputLabel by remember { mutableStateOf("") }
    var subInputOptions by remember { mutableStateOf("") }
    var subInputTargetOption by remember { mutableStateOf("") }
    var hasNoteInput by remember { mutableStateOf(false) }

    var editingFieldId by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .padding(4.dp)
                .heightIn(max = 620.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Style, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                        Text(
                            text = "Configure Template",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, null)
                    }
                }

                HorizontalDivider()

                // Template Name Edit & Default Status
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    var templateNameText by remember(template.id) { mutableStateOf(template.name) }
                    val isCurrentDefault = viewModel.getDefaultTemplateId() == template.id

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            UrduTextField(
                                value = templateNameText,
                                onValueChange = {
                                    templateNameText = it
                                    viewModel.updateTemplateName(template.id, it)
                                },
                                label = { Text("Template Name ✏️") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                isUrduMode = isUrduMode,
                                viewModel = viewModel
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isCurrentDefault) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.Star, "Default Sizing Template", tint = Color(0xFFFFB300), modifier = Modifier.size(16.dp))
                                    Text("Default Sizing Template", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                }
                            }
                        } else {
                            OutlinedButton(
                                onClick = {
                                    viewModel.setDefaultTemplateId(template.id)
                                    Toast.makeText(context, "'${template.name}' set as Default Sizing Template", Toast.LENGTH_SHORT).show()
                                },
                                contentPadding = PaddingValues(horizontal = 10.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Icon(Icons.Default.StarOutline, null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Set as Default", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        Button(
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            onClick = {
                                onDeleteTemplate(template.id)
                                if (isCurrentDefault) {
                                    viewModel.setDefaultTemplateId(null)
                                }
                                onDismiss()
                                Toast.makeText(context, "Template deleted", Toast.LENGTH_SHORT).show()
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Delete Template", fontSize = 11.sp)
                        }
                    }
                }

                HorizontalDivider()

                // List of existing parameters
                Text("Existing Parameter Fields:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                val templateFields = fields.filter { it.templateId == template.id }.distinctBy { it.id }
                if (templateFields.isEmpty()) {
                    Text("No parameters registered yet.", fontSize = 12.sp, color = Color.Gray)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        templateFields.forEach { f ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.dp, Color.LightGray.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f))
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = if (f.isDemand) Icons.Default.DesignServices else Icons.Default.SquareFoot,
                                        contentDescription = null,
                                        tint = if (f.isDemand) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = f.label,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                    if (f.options.isNotBlank()) {
                                        Text(
                                            text = "[${f.options}]",
                                            fontSize = 11.sp,
                                            color = Color.Gray,
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = {
                                            // Start editing this field!
                                            editingFieldId = f.id
                                            label = f.label
                                            selectedType = f.type
                                            options = f.options
                                            isDemand = f.isDemand
                                            hasSubInput = f.hasSubInput
                                            subInputType = f.subInputType
                                            subInputLabel = f.subInputLabel
                                            subInputOptions = f.subInputOptions
                                            subInputTargetOption = f.subInputTargetOption
                                            hasNoteInput = f.hasNoteInput
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit Input Parameter",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = { onDeleteField(f.id) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Cancel,
                                            contentDescription = "Delete Input Parameter",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider()

                // Form to add/edit parameter
                val isEditing = editingFieldId != null
                Text(
                    text = if (isEditing) "Edit Sizing Unit Parameter Input:" else "Add Sizing Unit Parameter Input:",
                    fontWeight = FontWeight.Black,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Parameter Type", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (!isDemand) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                                .border(1.dp, if (!isDemand) MaterialTheme.colorScheme.primary else Color.LightGray.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .clickable { isDemand = false; selectedType = "NUMBER" }
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Fitting Size 📏", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isDemand) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                                .border(1.dp, if (isDemand) MaterialTheme.colorScheme.primary else Color.LightGray.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .clickable { isDemand = true; selectedType = "RADIO" }
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Style Demand 🪡", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                UrduTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Label (e.g. Sleeves Style or Chest Size)") },
                    modifier = Modifier.fillMaxWidth(),
                    isUrduMode = isUrduMode,
                    viewModel = viewModel
                )

                // Input option for formats
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf("NUMBER", "TEXT", "RADIO", "CHECKBOX").forEach { t ->
                        val isSel = selectedType == t
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSel) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .clickable { selectedType = t }
                                .border(1.dp, if (isSel) MaterialTheme.colorScheme.secondary else Color.Transparent, RoundedCornerShape(6.dp))
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(t, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (selectedType == "RADIO" || selectedType == "CHECKBOX") {
                    UrduTextField(
                        value = options,
                        onValueChange = { options = it },
                        label = { Text("Choices (comma-separated list)") },
                        placeholder = { Text("e.g. Regular, Half sleeves, Cuffs") },
                        modifier = Modifier.fillMaxWidth(),
                        isUrduMode = isUrduMode,
                        viewModel = viewModel
                    )
                }

                Card(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.15f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.25f))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Checkbox(
                                checked = hasSubInput,
                                onCheckedChange = { hasSubInput = it }
                            )
                            Text("Add Sub-Input / Nested Option", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.tertiary)
                        }

                        if (hasSubInput) {
                            UrduTextField(
                                value = subInputLabel,
                                onValueChange = { subInputLabel = it },
                                label = { Text("Sub-Input Label (e.g. Pocket Depth)") },
                                modifier = Modifier.fillMaxWidth(),
                                isUrduMode = isUrduMode,
                                viewModel = viewModel
                            )

                            Text("Sub-Input Format", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                listOf("TEXT", "NUMBER", "RADIO").forEach { st ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.clickable { subInputType = st }
                                    ) {
                                        RadioButton(
                                            selected = subInputType == st,
                                            onClick = { subInputType = st }
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(st, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                    }
                                }
                            }

                            if (subInputType == "RADIO") {
                                UrduTextField(
                                    value = subInputOptions,
                                    onValueChange = { subInputOptions = it },
                                    label = { Text("Sub Options (comma-separated)") },
                                    placeholder = { Text("e.g. Deep, Shallow, Standard") },
                                    modifier = Modifier.fillMaxWidth(),
                                    isUrduMode = isUrduMode,
                                    viewModel = viewModel
                                )
                            }

                            if (selectedType == "RADIO") {
                                UrduTextField(
                                    value = subInputTargetOption,
                                    onValueChange = { subInputTargetOption = it },
                                    label = { Text("Target Option to trigger sub-input") },
                                    placeholder = { Text("e.g. French Cuff (leave blank for any)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    isUrduMode = isUrduMode,
                                    viewModel = viewModel
                                )
                            }
                        }
                    }
                }

                // Hide field-specific note inputs in favor of the new consolidated/unified Additional Notes section at the bottom.
                // hasNoteInput is defaulted to false.

                if (isEditing) {
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            if (label.isNotBlank()) {
                                viewModel.updateField(
                                    editingFieldId!!,
                                    label,
                                    selectedType,
                                    options,
                                    isDemand,
                                    hasSubInput,
                                    subInputType,
                                    subInputLabel,
                                    subInputOptions,
                                    subInputTargetOption,
                                    hasNoteInput
                                )
                                editingFieldId = null
                                label = ""
                                options = ""
                                hasSubInput = false
                                subInputLabel = ""
                                subInputType = "TEXT"
                                subInputOptions = ""
                                subInputTargetOption = ""
                                hasNoteInput = false
                                Toast.makeText(context, "Updated parameter field!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Please enter a label name first", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Icon(Icons.Default.Save, null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Update Input Parameter")
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            editingFieldId = null
                            label = ""
                            options = ""
                            hasSubInput = false
                            subInputLabel = ""
                            subInputType = "TEXT"
                            subInputOptions = ""
                            subInputTargetOption = ""
                            hasNoteInput = false
                        }
                    ) {
                        Icon(Icons.Default.Cancel, null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Cancel Edit")
                    }
                } else {
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            if (label.isNotBlank()) {
                                onAddField(template.id, label, selectedType, options, isDemand, hasSubInput, subInputType, subInputLabel, subInputOptions, subInputTargetOption, hasNoteInput)
                                label = ""
                                options = ""
                                hasSubInput = false
                                subInputLabel = ""
                                subInputType = "TEXT"
                                subInputOptions = ""
                                subInputTargetOption = ""
                                hasNoteInput = false
                                Toast.makeText(context, "Added template field!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Please enter a label name first", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Icon(Icons.Default.Add, null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Input Parameter")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Finished Editing")
                    }
                }
            }
        }
    }
}










