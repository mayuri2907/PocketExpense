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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pocketexpense.ui.components.CategoryIconHelper
import com.example.pocketexpense.ui.theme.DarkBorder
import com.example.pocketexpense.ui.theme.DarkSurface
import com.example.pocketexpense.ui.theme.DarkSurfaceElevated
import com.example.pocketexpense.ui.theme.MintGreen
import com.example.pocketexpense.ui.theme.TextPrimary
import com.example.pocketexpense.ui.theme.TextSecondary
import com.example.pocketexpense.ui.theme.TextTertiary
import com.example.pocketexpense.ui.viewmodel.CategoriesUiState
import com.example.pocketexpense.util.CurrencyUtils

@Composable
fun CategoriesScreen(
    uiState: CategoriesUiState,
    modifier: Modifier = Modifier
) {
    val grandTotal = if (uiState.totalExpensePaise > 0) uiState.totalExpensePaise else 1L

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Categories",
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Track your budget distribution across 8 essential categories",
                fontSize = 13.sp,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        items(uiState.categorySpending, key = { it.categoryId }) { spending ->
            val catColor = CategoryIconHelper.parseColor(spending.colorHex)
            val progress = (spending.totalPaise.toFloat() / grandTotal).coerceIn(0f, 1f)
            val percentage = (progress * 100).toInt()

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .border(1.dp, DarkBorder, RoundedCornerShape(18.dp)),
                color = DarkSurface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(catColor.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = CategoryIconHelper.getIcon(spending.iconName),
                                    contentDescription = spending.categoryName,
                                    tint = catColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = spending.categoryName,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "${spending.transactionCount} ${if (spending.transactionCount == 1) "entry" else "entries"}",
                                    fontSize = 12.sp,
                                    color = TextTertiary
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = CurrencyUtils.paiseToRupeesString(spending.totalPaise),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (spending.totalPaise > 0) MintGreen else TextSecondary
                            )
                            Text(
                                text = "$percentage% of total",
                                fontSize = 12.sp,
                                color = TextTertiary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

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

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}
