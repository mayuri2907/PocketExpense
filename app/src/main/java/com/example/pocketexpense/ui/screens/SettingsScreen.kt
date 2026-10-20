package com.example.pocketexpense.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import com.example.pocketexpense.quick.QuickNotificationHelper
import com.example.pocketexpense.util.DiagnosticStatus
import com.example.pocketexpense.util.NotificationDiagnosticTracker
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pocketexpense.ui.theme.AmberWarning
import com.example.pocketexpense.ui.theme.CoralRed
import com.example.pocketexpense.ui.theme.DarkBorder
import com.example.pocketexpense.ui.theme.DarkSurface
import com.example.pocketexpense.ui.theme.DarkSurfaceElevated
import com.example.pocketexpense.ui.theme.ElectricPurple
import com.example.pocketexpense.ui.theme.MintGreen
import com.example.pocketexpense.ui.theme.MintGreenContainer
import com.example.pocketexpense.ui.theme.TextPrimary
import com.example.pocketexpense.ui.theme.TextSecondary
import com.example.pocketexpense.ui.theme.TextTertiary
import com.example.pocketexpense.ui.viewmodel.SettingsUiState

@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    isNotificationListenerEnabled: Boolean,
    onEnableNotificationListener: () -> Unit,
    isOverlayPermissionEnabled: Boolean,
    onEnableOverlayPermission: () -> Unit,
    onExportData: () -> Unit,
    onResetDatabase: () -> Unit,
    modifier: Modifier = Modifier,
    isServiceConnected: Boolean = true,
    onReopenOnboarding: (() -> Unit)? = null
) {
    var showResetDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Settings",
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            color = TextPrimary
        )

        // 1. Privacy Banner Card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, DarkBorder, RoundedCornerShape(20.dp)),
            color = DarkSurface
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MintGreen.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Private",
                        tint = MintGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = "100% Local & Private",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "All expense records stay securely on your physical device in SQLite. No cloud sync, no tracking, and no third-party analytics.",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        // 2. UPI Auto-Tracking Services Card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, DarkBorder, RoundedCornerShape(20.dp)),
            color = DarkSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(MintGreen.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MintGreen,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "UPI Auto-Detection",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    val (statusLabel, statusColor) = when {
                        !isNotificationListenerEnabled -> "DISABLED" to CoralRed
                        !isServiceConnected -> "CONNECTING" to AmberWarning
                        else -> "LIVE & CONNECTED" to MintGreen
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(statusColor.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = statusLabel,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = when {
                        !isNotificationListenerEnabled -> "Notification access is disabled. PocketExpense cannot read payment notifications until enabled."
                        !isServiceConnected -> "Permission is granted, but the Android system has not connected the listener service yet. Tap below to rebind."
                        else -> "Actively listening for payment notifications from Google Pay, PhonePe, Paytm, and CRED. 100% on-device."
                    },
                    fontSize = 13.sp,
                    color = TextSecondary,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onEnableNotificationListener,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
                    ) {
                        Text(
                            text = if (isNotificationListenerEnabled) {
                                if (isServiceConnected) "Notification Access" else "Reconnect Listener"
                            } else "Enable Notifications",
                            fontSize = 11.sp
                        )
                    }

                    OutlinedButton(
                        onClick = onEnableOverlayPermission,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isOverlayPermissionEnabled) MintGreen.copy(alpha = 0.5f) else DarkBorder
                        )
                    ) {
                        Text(
                            text = if (isOverlayPermissionEnabled) "Overlay: ON" else "Enable Overlay",
                            fontSize = 11.sp,
                            color = if (isOverlayPermissionEnabled) MintGreen else TextPrimary
                        )
                    }
                }

                if (onReopenOnboarding != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = onReopenOnboarding,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
                    ) {
                        Text(
                            text = "Revisit Permission Setup Guide",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // 2B. Live Detection Diagnostics Card
        val diagnosticEvents by NotificationDiagnosticTracker.events.collectAsState()
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, DarkBorder, RoundedCornerShape(20.dp)),
            color = DarkSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Diagnostics",
                            tint = MintGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Detection Diagnostics",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    if (diagnosticEvents.isNotEmpty()) {
                        Text(
                            text = "${diagnosticEvents.size} events",
                            fontSize = 11.sp,
                            color = TextTertiary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (diagnosticEvents.isEmpty()) {
                    Text(
                        text = "No payment events detected in this session yet. When a supported notification arrives from Paytm, PhonePe, Google Pay, or CRED, its processing status will appear here.",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        lineHeight = 17.sp
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        diagnosticEvents.take(5).forEach { event ->
                            val (badgeBg, badgeFg) = when (event.status) {
                                DiagnosticStatus.PARSED_SUCCESS -> MintGreen.copy(alpha = 0.15f) to MintGreen
                                DiagnosticStatus.DUPLICATE_IGNORED -> TextTertiary.copy(alpha = 0.15f) to TextSecondary
                                DiagnosticStatus.NON_EXPENSE_IGNORED -> TextTertiary.copy(alpha = 0.15f) to TextTertiary
                                DiagnosticStatus.AMBIGUOUS_UNPARSED -> AmberWarning.copy(alpha = 0.15f) to AmberWarning
                                DiagnosticStatus.SERVICE_DISCONNECTED -> CoralRed.copy(alpha = 0.15f) to CoralRed
                                DiagnosticStatus.DATABASE_ERROR -> CoralRed.copy(alpha = 0.15f) to CoralRed
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(DarkSurfaceElevated)
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = event.appDisplayName,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "• ${event.timeFormatted}",
                                            fontSize = 10.sp,
                                            color = TextTertiary
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = event.summary,
                                        fontSize = 11.sp,
                                        color = TextSecondary,
                                        maxLines = 2
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(badgeBg)
                                        .padding(horizontal = 6.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = when (event.status) {
                                            DiagnosticStatus.PARSED_SUCCESS -> "SUCCESS"
                                            DiagnosticStatus.DUPLICATE_IGNORED -> "DUPLICATE"
                                            DiagnosticStatus.NON_EXPENSE_IGNORED -> "IGNORED"
                                            DiagnosticStatus.AMBIGUOUS_UNPARSED -> "AMBIGUOUS"
                                            DiagnosticStatus.SERVICE_DISCONNECTED -> "DISCONNECTED"
                                            DiagnosticStatus.DATABASE_ERROR -> "ERROR"
                                        },
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = badgeFg
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Quick Expense Shortcuts & Gestures
        val context = LocalContext.current
        var isQuickNotifEnabled by remember { mutableStateOf(QuickNotificationHelper.isEnabled(context)) }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, DarkBorder, RoundedCornerShape(20.dp)),
            color = DarkSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.FlashOn,
                        contentDescription = "Quick Expense",
                        tint = MintGreen,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Quick Expense Triggers",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Multiple fast entry triggers are supported across Android to record expenses in seconds:",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    lineHeight = 17.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Paytm & Banking App Safe Guarantee Card
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MintGreenContainer.copy(alpha = 0.35f))
                        .border(1.dp, MintGreen.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MintGreen.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Safe",
                            tint = MintGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Paytm & Banking Safe",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MintGreen.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "100% SAFE",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MintGreen,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Zero Accessibility Services or intrusive overlays over Paytm. Your Paytm transactions and UPI payments run smoothly with zero security blocks or warnings.",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            lineHeight = 15.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // In-App Double Volume-Up Shortcut
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkSurfaceElevated)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "In-App Double Volume-Up",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MintGreen.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "READY",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MintGreen,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Double-press Volume-Up while PocketExpense is open to instantly open the Quick Expense form.",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            lineHeight = 15.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OemTipItem(
                    brand = "Android Quick Settings Tile (Recommended)",
                    instruction = "Swipe down twice from top of screen → Edit tiles → Add 'Quick Expense'. Tap it from any screen or app to open the quick form."
                )

                OemTipItem(
                    brand = "OEM Back-Tap / Quick Tap Gesture",
                    instruction = "Supported on Pixel (Quick Tap), Motorola (Quick Launch), & Samsung (Good Lock). In your phone's Gesture settings, map Back-Tap to the PocketExpense 'Quick Expense' shortcut."
                )

                OemTipItem(
                    brand = "Home Screen App Shortcut",
                    instruction = "Long-press the PocketExpense app icon on your home launcher and tap 'Quick Expense'."
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkSurfaceElevated)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Notification Shortcut",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Show ongoing notification with Quick Add button",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                    Switch(
                        checked = isQuickNotifEnabled,
                        onCheckedChange = { enabled ->
                            isQuickNotifEnabled = enabled
                            QuickNotificationHelper.setEnabled(context, enabled)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = MintGreen,
                            uncheckedThumbColor = TextTertiary,
                            uncheckedTrackColor = DarkSurface
                        )
                    )
                }
            }
        }

        // 4. OEM Background Reliability Guide
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, DarkBorder, RoundedCornerShape(20.dp)),
            color = DarkSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                        contentDescription = "Troubleshooting",
                        tint = ElectricPurple,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Phone Reliability Guide",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Some Android phone manufacturers aggressively terminate background listeners. For reliable automatic tracking, please check:",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    lineHeight = 17.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                OemTipItem(
                    brand = "Xiaomi / Redmi / POCO",
                    instruction = "Enable Autostart & set Battery Saver to 'No Restrictions'. Allow 'Display pop-up windows in background'."
                )
                OemTipItem(
                    brand = "Samsung (One UI)",
                    instruction = "Go to Settings → Battery → Background usage limits → Add PocketExpense to 'Never sleeping apps'."
                )
                OemTipItem(
                    brand = "Oppo / Vivo / Realme",
                    instruction = "Enable 'Allow background activity' and grant 'Display over other apps' in App Settings."
                )
                OemTipItem(
                    brand = "Stock Android / Pixel",
                    instruction = "Go to App info → App battery usage → Set to 'Unrestricted'."
                )
            }
        }

        // 4. Local Storage Stats
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, DarkBorder, RoundedCornerShape(20.dp)),
            color = DarkSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Storage,
                        contentDescription = "Database",
                        tint = ElectricPurple,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Local Storage",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                SettingStatRow(label = "Total Expenses", value = "${uiState.totalExpenseCount}")
                SettingStatRow(label = "Default Categories", value = "${uiState.totalCategoryCount}")
                SettingStatRow(label = "Database Engine", value = "Room 2.6.1 (SQLite)")
                SettingStatRow(label = "Storage File", value = uiState.databaseName)
            }
        }

        // 5. Data Management (Export & Reset)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, DarkBorder, RoundedCornerShape(20.dp)),
            color = DarkSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "Data Management",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Export your complete expense history or clear local storage.",
                    fontSize = 13.sp,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(16.dp))

                // User-Initiated Local CSV Export
                Button(
                    onClick = onExportData,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MintGreen.copy(alpha = 0.15f),
                        contentColor = MintGreen
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = "Export CSV",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Export All Expenses to CSV", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Reset Action
                Button(
                    onClick = { showResetDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CoralRed.copy(alpha = 0.15f),
                        contentColor = CoralRed
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.RestartAlt,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Reset All Expense Data", fontWeight = FontWeight.Bold)
                }
            }
        }

        // 6. About App & Security
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, DarkBorder, RoundedCornerShape(20.dp)),
            color = DarkSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "PocketExpense v1.0.1",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Production Release • Local-First Architecture",
                            fontSize = 12.sp,
                            color = TextTertiary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = MintGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Zero internet permission • Zero telemetry • 100% offline",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }

    // Reset Confirmation Dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = {
                Text(
                    text = "Reset All Data?",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Text(
                    text = "This will permanently delete all recorded expenses from your local database. Default categories will be preserved. This action cannot be undone.",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showResetDialog = false
                        onResetDatabase()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CoralRed,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Yes, Delete All", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showResetDialog = false },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = DarkSurface,
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
private fun SettingStatRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 13.sp, color = TextSecondary)
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
    }
}

@Composable
private fun OemTipItem(brand: String, instruction: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurfaceElevated)
            .padding(10.dp)
    ) {
        Text(
            text = brand,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MintGreen
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = instruction,
            fontSize = 11.sp,
            color = TextSecondary,
            lineHeight = 15.sp
        )
    }
}
