package com.example.pocketexpense.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import android.content.Intent
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.pocketexpense.data.model.Expense
import com.example.pocketexpense.service.ExpenseNotificationListenerService
import com.example.pocketexpense.ui.components.AddEditExpenseDialog
import com.example.pocketexpense.ui.components.QuickExpenseDialog
import com.example.pocketexpense.ui.theme.CoralRed
import com.example.pocketexpense.ui.theme.DarkBackground
import com.example.pocketexpense.ui.theme.DarkBorder
import com.example.pocketexpense.ui.theme.DarkSurface
import com.example.pocketexpense.ui.theme.MintGreen
import com.example.pocketexpense.ui.theme.TextPrimary
import com.example.pocketexpense.ui.theme.TextSecondary
import com.example.pocketexpense.ui.viewmodel.ExpenseViewModel
import com.example.pocketexpense.util.CsvExportHelper
import com.example.pocketexpense.util.NotificationPermissionHelper

import com.example.pocketexpense.PocketExpenseApplication
import com.example.pocketexpense.floating.FloatingExpensePopupContent
import com.example.pocketexpense.floating.OverlayPermissionHelper
import com.example.pocketexpense.util.OnboardingPreferences

enum class NavTab(val label: String, val icon: ImageVector) {
    DASHBOARD("Dashboard", Icons.Default.Home),
    TRANSACTIONS("Transactions", Icons.AutoMirrored.Filled.ReceiptLong),
    CATEGORIES("Categories", Icons.Default.Category),
    SETTINGS("Settings", Icons.Default.Settings)
}

@Composable
fun MainScreen(
    viewModel: ExpenseViewModel,
    isQuickExpenseTriggered: Boolean = false,
    onQuickExpenseHandled: () -> Unit = {},
    currentThemeMode: com.example.pocketexpense.util.ThemeMode = com.example.pocketexpense.util.ThemeMode.SYSTEM,
    onThemeChanged: (com.example.pocketexpense.util.ThemeMode) -> Unit = {}
) {
    val colors = com.example.pocketexpense.ui.theme.AppTheme.colors
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val app = context.applicationContext as PocketExpenseApplication

    var selectedTab by remember { mutableStateOf(NavTab.DASHBOARD) }

    // Dialog & Interaction states
    var showAddDialog by remember { mutableStateOf(false) }
    var showQuickExpenseDialog by remember { mutableStateOf(false) }
    var expenseToEdit by remember { mutableStateOf<Expense?>(null) }
    var expenseToDelete by remember { mutableStateOf<Expense?>(null) }

    LaunchedEffect(isQuickExpenseTriggered) {
        if (isQuickExpenseTriggered) {
            showQuickExpenseDialog = true
            onQuickExpenseHandled()
        }
    }

    var isOnboardingCompleted by remember {
        mutableStateOf(OnboardingPreferences.isOnboardingCompleted(context))
    }
    val isServiceConnected by ExpenseNotificationListenerService.isConnected.collectAsState()

    // Notification permission & Overlay permission tracking with reactive lifecycle refresh
    var isNotificationListenerEnabled by remember {
        mutableStateOf(NotificationPermissionHelper.isNotificationListenerEnabled(context))
    }
    var isOverlayPermissionEnabled by remember {
        mutableStateOf(OverlayPermissionHelper.canDrawOverlays(context))
    }

    // Active floating expense item (observed for in-app fallback presentation)
    val activePendingExpense by app.floatingExpenseManager.activePendingExpense.collectAsState()

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isNotificationListenerEnabled = NotificationPermissionHelper.isNotificationListenerEnabled(context)
                isOverlayPermissionEnabled = OverlayPermissionHelper.canDrawOverlays(context)
                if (isNotificationListenerEnabled) {
                    ExpenseNotificationListenerService.tryRebind(context)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    if (!isOnboardingCompleted) {
        FirstLaunchOnboardingScreen(
            isNotificationListenerEnabled = isNotificationListenerEnabled,
            isOverlayPermissionEnabled = isOverlayPermissionEnabled,
            isServiceConnected = isServiceConnected,
            onEnableNotificationListener = {
                ExpenseNotificationListenerService.tryRebind(context)
                context.startActivity(NotificationPermissionHelper.createSettingsIntent())
            },
            onEnableOverlayPermission = {
                context.startActivity(OverlayPermissionHelper.createOverlaySettingsIntent(context))
            },
            onCompleteOnboarding = {
                isOnboardingCompleted = true
            }
        )
        return
    }

    val snackbarHostState = remember { SnackbarHostState() }

    // Collecting View Model states
    val dashboardState by viewModel.dashboardUiState.collectAsState()
    val transactionsState by viewModel.transactionsUiState.collectAsState()
    val categoriesState by viewModel.categoriesUiState.collectAsState()
    val settingsState by viewModel.settingsUiState.collectAsState()
    val userMessage by viewModel.userMessage.collectAsState()

    // Show transient feedback snackbar
    LaunchedEffect(userMessage) {
        userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearUserMessage()
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background),
        containerColor = colors.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(
                containerColor = colors.surface,
                modifier = Modifier
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .border(1.dp, colors.border, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
                tonalElevation = 10.dp
            ) {
                NavTab.entries.forEach { tab ->
                    val isSelected = selectedTab == tab
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { selectedTab = tab },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.label,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = {
                            Text(
                                text = tab.label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = colors.primary,
                            selectedTextColor = colors.primary,
                            unselectedIconColor = colors.textSecondary,
                            unselectedTextColor = colors.textSecondary,
                            indicatorColor = colors.primary.copy(alpha = 0.18f)
                        )
                    )
                }
            }
        },
        floatingActionButton = {
            if (selectedTab == NavTab.DASHBOARD || selectedTab == NavTab.TRANSACTIONS) {
                FloatingActionButton(
                    onClick = {
                        expenseToEdit = null
                        showAddDialog = true
                    },
                    containerColor = MintGreen,
                    contentColor = Color.Black,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Expense",
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(DarkBackground)
        ) {
            Crossfade(targetState = selectedTab, label = "TabCrossfade") { tab ->
                when (tab) {
                    NavTab.DASHBOARD -> DashboardScreen(
                        uiState = dashboardState,
                        isNotificationListenerEnabled = isNotificationListenerEnabled,
                        onEnableNotificationListener = {
                            ExpenseNotificationListenerService.tryRebind(context)
                            context.startActivity(NotificationPermissionHelper.createSettingsIntent())
                        },
                        isOverlayPermissionEnabled = isOverlayPermissionEnabled,
                        onEnableOverlayPermission = {
                            context.startActivity(OverlayPermissionHelper.createOverlaySettingsIntent(context))
                        },
                        onAddExpenseClick = {
                            expenseToEdit = null
                            showAddDialog = true
                        },
                        onSeeAllTransactionsClick = { selectedTab = NavTab.TRANSACTIONS },
                        onEditExpense = { expense ->
                            expenseToEdit = expense
                            showAddDialog = true
                        },
                        onDeleteExpense = { expense ->
                            expenseToDelete = expense
                        },
                        isServiceConnected = isServiceConnected,
                        onRebindService = {
                            ExpenseNotificationListenerService.tryRebind(context)
                        },
                        onQuickExpenseClick = {
                            showQuickExpenseDialog = true
                        }
                    )

                    NavTab.TRANSACTIONS -> TransactionsScreen(
                        uiState = transactionsState,
                        onSearchQueryChange = { viewModel.setSearchQuery(it) },
                        onCategorySelect = { viewModel.setSelectedCategory(it) },
                        onEditExpense = { expense ->
                            expenseToEdit = expense
                            showAddDialog = true
                        },
                        onDeleteExpense = { expense ->
                            expenseToDelete = expense
                        }
                    )

                    NavTab.CATEGORIES -> CategoriesScreen(
                        uiState = categoriesState
                    )

                    NavTab.SETTINGS -> SettingsScreen(
                        uiState = settingsState,
                        isNotificationListenerEnabled = isNotificationListenerEnabled,
                        onEnableNotificationListener = {
                            ExpenseNotificationListenerService.tryRebind(context)
                            context.startActivity(NotificationPermissionHelper.createSettingsIntent())
                        },
                        isOverlayPermissionEnabled = isOverlayPermissionEnabled,
                        onEnableOverlayPermission = {
                            context.startActivity(OverlayPermissionHelper.createOverlaySettingsIntent(context))
                        },
                        onExportData = {
                            viewModel.exportCsv { csv ->
                                val shareIntent = CsvExportHelper.createShareIntent(csv)
                                context.startActivity(Intent.createChooser(shareIntent, "Export Expenses"))
                            }
                        },
                        onResetDatabase = { viewModel.resetDatabase() },
                        currentThemeMode = currentThemeMode,
                        onThemeChanged = onThemeChanged,
                        isServiceConnected = isServiceConnected
                    )
                }
            }

            // In-App Fallback: Display floating popup when active and system overlay is disabled
            if (activePendingExpense != null && !isOverlayPermissionEnabled) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                        .align(Alignment.TopCenter)
                ) {
                    FloatingExpensePopupContent(
                        item = activePendingExpense!!,
                        totalCountdownMillis = 3000L
                    )
                }
            }
        }
    }

    // Add / Edit Expense Dialog
    if (showAddDialog) {
        AddEditExpenseDialog(
            categories = transactionsState.categories,
            expenseToEdit = expenseToEdit,
            onDismiss = {
                showAddDialog = false
                expenseToEdit = null
            },
            onSave = { title, amount, categoryId, note ->
                if (expenseToEdit != null) {
                    viewModel.updateExpense(
                        id = expenseToEdit!!.id,
                        title = title,
                        amountRupees = amount,
                        categoryId = categoryId,
                        note = note,
                        timestamp = expenseToEdit!!.timestamp
                    )
                } else {
                    viewModel.addExpense(
                        title = title,
                        amountRupees = amount,
                        categoryId = categoryId,
                        note = note
                    )
                }
            }
        )
    }

    // Delete Confirmation Dialog
    if (expenseToDelete != null) {
        val expense = expenseToDelete!!
        AlertDialog(
            onDismissRequest = { expenseToDelete = null },
            title = {
                Text(text = "Delete Expense?", fontWeight = FontWeight.Bold, color = TextPrimary)
            },
            text = {
                Text(
                    text = "Are you sure you want to delete \"${expense.title}\"? This action cannot be undone.",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteExpense(expense)
                        expenseToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CoralRed,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { expenseToDelete = null },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = DarkSurface,
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Quick Expense Dialog
    if (showQuickExpenseDialog) {
        QuickExpenseDialog(
            categories = transactionsState.categories.ifEmpty { com.example.pocketexpense.data.model.Category.defaultCategories() },
            onSaveExpense = { amountPaise, description, categoryId ->
                viewModel.addQuickExpense(amountPaise, description, categoryId)
            },
            onDismiss = {
                showQuickExpenseDialog = false
            }
        )
    }
}
