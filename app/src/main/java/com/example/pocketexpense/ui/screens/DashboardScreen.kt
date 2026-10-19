package com.example.pocketexpense.ui.screens

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
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.filled.Add

import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pocketexpense.data.model.Expense
import com.example.pocketexpense.data.model.ExpenseWithCategory
import com.example.pocketexpense.ui.components.CategoryIconHelper
import com.example.pocketexpense.ui.components.ExpenseItemCard
import com.example.pocketexpense.ui.theme.DarkBorder
import com.example.pocketexpense.ui.theme.DarkSurface
import com.example.pocketexpense.ui.theme.DarkSurfaceElevated
import com.example.pocketexpense.ui.theme.ElectricPurple
import com.example.pocketexpense.ui.theme.MintGreen
import com.example.pocketexpense.ui.theme.TextPrimary
import com.example.pocketexpense.ui.theme.TextSecondary
import com.example.pocketexpense.ui.theme.TextTertiary
import com.example.pocketexpense.ui.viewmodel.DashboardUiState
import com.example.pocketexpense.util.CurrencyUtils

import com.example.pocketexpense.ui.components.NotificationAccessCard
import com.example.pocketexpense.ui.components.OverlayAccessCard

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
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top App Bar / Title
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "PocketExpense",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Local-First Tracker",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MintGreen
                    )
                }

                Button(
                    onClick = onAddExpenseClick,
                    shape = RoundedCornerShape(14.dp),
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

        // Hero Card: Total Expenses
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .border(1.dp, DarkBorder, RoundedCornerShape(24.dp)),
                color = DarkSurface
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    ElectricPurple.copy(alpha = 0.2f),
                                    MintGreen.copy(alpha = 0.1f),
                                    DarkSurface
                                )
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(MintGreen.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.TrendingDown,
                                    contentDescription = null,
                                    tint = MintGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "TOTAL SPENDING",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextSecondary,
                                letterSpacing = 1.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = CurrencyUtils.paiseToRupeesString(uiState.totalExpensePaise),
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Black,
                            color = TextPrimary
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Stored entirely offline in local SQLite database",
                            fontSize = 12.sp,
                            color = TextTertiary
                        )
                    }
                }
            }
        }

        // Notification Access Onboarding / Status Card
        item {
            NotificationAccessCard(
                isPermissionGranted = isNotificationListenerEnabled,
                onEnableClick = onEnableNotificationListener
            )
        }

        // Overlay Permission Onboarding / Status Card
        item {
            OverlayAccessCard(
                isOverlayPermissionGranted = isOverlayPermissionEnabled,
                onEnableOverlayClick = onEnableOverlayPermission
            )
        }

        // Category Breakdown Card
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.dp, DarkBorder, RoundedCornerShape(20.dp)),
                color = DarkSurface
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Category Breakdown",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    val activeCategories = uiState.categorySpending.filter { it.totalPaise > 0 }
                    if (activeCategories.isEmpty()) {
                        Text(
                            text = "No spending recorded yet. Tap + Add to record an expense.",
                            fontSize = 13.sp,
                            color = TextSecondary,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        val grandTotal = if (uiState.totalExpensePaise > 0) uiState.totalExpensePaise else 1L

                        activeCategories.take(4).forEach { spending ->
                            val catColor = CategoryIconHelper.parseColor(spending.colorHex)
                            val progress = (spending.totalPaise.toFloat() / grandTotal).coerceIn(0f, 1f)
                            val percentage = (progress * 100).toInt()

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
                                            fontWeight = FontWeight.Medium,
                                            color = TextPrimary
                                        )
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = CurrencyUtils.paiseToRupeesString(spending.totalPaise),
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TextPrimary
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "($percentage%)",
                                            fontSize = 12.sp,
                                            color = TextTertiary
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                LinearProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = catColor,
                                    trackColor = DarkSurfaceElevated
                                )
                            }
                        }
                    }
                }
            }
        }

        // Recent Transactions Section Header
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
                    modifier = Modifier.clickable { onSeeAllTransactionsClick() },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "See All",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
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

        // Recent Transactions List
        if (uiState.recentExpenses.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, DarkBorder, RoundedCornerShape(16.dp)),
                    color = DarkSurface
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No recent transactions",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Expenses you add will appear here immediately",
                            fontSize = 12.sp,
                            color = TextTertiary
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
