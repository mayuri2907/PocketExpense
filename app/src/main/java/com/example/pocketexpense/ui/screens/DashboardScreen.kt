package com.example.pocketexpense.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pocketexpense.data.model.Expense
import com.example.pocketexpense.ui.components.CategoryIconHelper
import com.example.pocketexpense.ui.components.ExpenseItemCard
import com.example.pocketexpense.ui.components.NotificationAccessCard
import com.example.pocketexpense.ui.components.OverlayAccessCard
import com.example.pocketexpense.ui.theme.DarkBorder
import com.example.pocketexpense.ui.theme.DarkBorderSubtle
import com.example.pocketexpense.ui.theme.DarkSurface
import com.example.pocketexpense.ui.theme.DarkSurfaceElevated
import com.example.pocketexpense.ui.theme.DarkSurfaceHighlight
import com.example.pocketexpense.ui.theme.ElectricPurple
import com.example.pocketexpense.ui.theme.MintGreen
import com.example.pocketexpense.ui.theme.MintGreenContainer
import com.example.pocketexpense.ui.theme.MintGreenGlow
import com.example.pocketexpense.ui.theme.SkyBlue
import com.example.pocketexpense.ui.theme.TextPrimary
import com.example.pocketexpense.ui.theme.TextSecondary
import com.example.pocketexpense.ui.theme.TextTertiary
import com.example.pocketexpense.ui.viewmodel.DashboardUiState
import com.example.pocketexpense.util.CurrencyUtils

@Composable
fun DashboardScreen(
    uiState: DashboardUiState,
    isNotificationListenerEnabled: Boolean,
    onEnableNotificationListener: () -> Unit,
    isOverlayPermissionEnabled: Boolean,
    onEnableOverlayPermission: () -> Unit,
    onAddExpenseClick: () -> Unit,
    onSeeAllTransactionsClick: () -> Unit,
    onEditExpense: (Expense) -> Unit,
    onDeleteExpense: (Expense) -> Unit,
    modifier: Modifier = Modifier,
    isServiceConnected: Boolean = true,
    onRebindService: (() -> Unit)? = null,
    onQuickExpenseClick: () -> Unit = {}
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Top App Bar / Title Header
        item {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "PocketExpense",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = TextPrimary,
                            letterSpacing = (-0.5).sp
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(MintGreen)
                        )
                        Text(
                            text = "Local-First • 100% Private",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MintGreen
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Quick Expense Trigger Button
                    OutlinedButton(
                        onClick = onQuickExpenseClick,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = MintGreenContainer.copy(alpha = 0.4f),
                            contentColor = MintGreen
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MintGreen.copy(alpha = 0.5f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.FlashOn,
                            contentDescription = "Quick Expense",
                            modifier = Modifier.size(18.dp),
                            tint = MintGreen
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Quick", fontWeight = FontWeight.Bold, color = MintGreen)
                    }

                    // Add Expense Button
                    Button(
                        onClick = onAddExpenseClick,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MintGreen,
                            contentColor = Color.Black
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Expense",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Add", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 2. Luxury Hero Card: Total Expenses
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(26.dp))
                    .border(1.dp, DarkBorderSubtle, RoundedCornerShape(26.dp)),
                color = DarkSurface
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    ElectricPurple.copy(alpha = 0.22f),
                                    SkyBlue.copy(alpha = 0.08f),
                                    MintGreen.copy(alpha = 0.12f),
                                    DarkSurface
                                )
                            )
                        )
                        .padding(22.dp)
                ) {
                    Column {
                        // Top row with status pill
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(MintGreen.copy(alpha = 0.18f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.TrendingDown,
                                        contentDescription = null,
                                        tint = MintGreen,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "TOTAL SPENDING",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextSecondary,
                                    letterSpacing = 1.2.sp
                                )
                            }

                            // Offline Security Pill
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = DarkSurfaceElevated.copy(alpha = 0.8f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Shield,
                                        contentDescription = null,
                                        tint = MintGreen,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "On-Device",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Large formatted total
                        Text(
                            text = CurrencyUtils.paiseToRupeesString(uiState.totalExpensePaise),
                            fontSize = 38.sp,
                            fontWeight = FontWeight.Black,
                            color = TextPrimary,
                            letterSpacing = (-0.5).sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Quick summary footer
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Stored in local encrypted SQLite database",
                                fontSize = 12.sp,
                                color = TextTertiary
                            )

                            if (uiState.recentExpenses.isNotEmpty()) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MintGreenContainer.copy(alpha = 0.5f)
                                ) {
                                    Text(
                                        text = "${uiState.recentExpenses.size} recent",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MintGreen,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Notification Access Status Card
        item {
            NotificationAccessCard(
                isPermissionGranted = isNotificationListenerEnabled,
                onEnableClick = onEnableNotificationListener,
                isServiceConnected = isServiceConnected,
                onRebindClick = onRebindService
            )
        }

        // 4. Overlay Permission Status Card
        item {
            OverlayAccessCard(
                isOverlayPermissionGranted = isOverlayPermissionEnabled,
                onEnableOverlayClick = onEnableOverlayPermission
            )
        }

        // 5. Category Breakdown Card
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .border(1.dp, DarkBorder, RoundedCornerShape(22.dp)),
                color = DarkSurface
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Category Breakdown",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Distribution",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    val activeCategories = uiState.categorySpending.filter { it.totalPaise > 0 }
                    if (activeCategories.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(DarkSurfaceElevated.copy(alpha = 0.5f))
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No spending recorded yet. Tap + Add or Quick to start tracking.",
                                fontSize = 13.sp,
                                color = TextSecondary
                            )
                        }
                    } else {
                        val grandTotal = if (uiState.totalExpensePaise > 0) uiState.totalExpensePaise else 1L

                        activeCategories.take(5).forEach { spending ->
                            val catColor = CategoryIconHelper.parseColor(spending.colorHex)
                            val targetProgress = (spending.totalPaise.toFloat() / grandTotal).coerceIn(0f, 1f)
                            val animatedProgress by animateFloatAsState(
                                targetValue = targetProgress,
                                animationSpec = tween(durationMillis = 600),
                                label = "CategoryProgress"
                            )
                            val percentage = (targetProgress * 100).toInt()

                            Column(modifier = Modifier.padding(vertical = 6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(catColor)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = spending.categoryName,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TextPrimary
                                        )
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = CurrencyUtils.paiseToRupeesString(spending.totalPaise),
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = DarkSurfaceHighlight
                                        ) {
                                            Text(
                                                text = "$percentage%",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = TextSecondary,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(7.dp))

                                LinearProgressIndicator(
                                    progress = { animatedProgress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(7.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = catColor,
                                    trackColor = DarkSurfaceElevated
                                )
                            }
                        }
                    }
                }
            }
        }

        // 6. Recent Transactions Section Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Transactions",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onSeeAllTransactionsClick() }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "See All",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MintGreen
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "See All",
                        tint = MintGreen,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        // 7. Recent Transactions List or Delightful Empty State
        if (uiState.recentExpenses.isEmpty()) {
            item {
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
                            .padding(36.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(MintGreen.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                                contentDescription = null,
                                tint = MintGreen,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "No recent transactions",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Expenses added manually or detected from UPI will appear here instantly.",
                            fontSize = 13.sp,
                            color = TextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(uiState.recentExpenses, key = { it.expense.id }) { item ->
                ExpenseItemCard(
                    item = item,
                    onEditClick = { onEditExpense(item.expense) },
                    onDeleteClick = { onDeleteExpense(item.expense) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp)) // Padding for bottom bar
        }
    }
}
