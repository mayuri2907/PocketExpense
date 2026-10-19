package com.example.pocketexpense.floating

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pocketexpense.data.model.Category
import com.example.pocketexpense.ui.theme.DarkBorder
import com.example.pocketexpense.ui.theme.DarkSurface
import com.example.pocketexpense.ui.theme.DarkSurfaceElevated
import com.example.pocketexpense.ui.theme.ElectricPurple
import com.example.pocketexpense.ui.theme.MintGreen
import com.example.pocketexpense.ui.theme.TextPrimary
import com.example.pocketexpense.ui.theme.TextSecondary
import com.example.pocketexpense.ui.theme.TextTertiary
import com.example.pocketexpense.util.CurrencyUtils
import kotlinx.coroutines.delay
import kotlin.math.ceil

@Composable
fun FloatingExpensePopupContent(
    item: PendingExpenseItem,
    modifier: Modifier = Modifier,
    totalCountdownMillis: Long = 3000L
) {
    var remainingMillis by remember(item.expenseId) { mutableLongStateOf(totalCountdownMillis) }
    var targetProgress by remember(item.expenseId) { mutableFloatStateOf(1f) }

    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = totalCountdownMillis.toInt(), easing = LinearEasing),
        label = "PopupCountdown"
    )

    // Active 3-second countdown timer
    LaunchedEffect(item.expenseId) {
        targetProgress = 0f
        val step = 50L
        while (remainingMillis > 0L) {
            delay(step)
            remainingMillis = (remainingMillis - step).coerceAtLeast(0L)
        }
        // Countdown expired -> auto-commit with default
        item.commitDefault()
    }

    val suggestedCategory = item.categories.find { it.id == item.suggestedCategoryId }
    val remainingSeconds = ceil(remainingMillis / 1000.0).toInt().coerceAtLeast(1)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .clip(RoundedCornerShape(20.dp))
            .border(1.5.dp, MintGreen.copy(alpha = 0.5f), RoundedCornerShape(20.dp)),
        color = DarkSurface,
        shadowElevation = 12.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Row 1: Header (App Badge, Merchant, Amount, Dismiss X)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(ElectricPurple.copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FlashOn,
                            contentDescription = null,
                            tint = ElectricPurple,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column {
                        Text(
                            text = item.merchant,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${item.appName} • Auto-Detected",
                            fontSize = 11.sp,
                            color = TextTertiary
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = CurrencyUtils.paiseToRupeesString(item.amountInPaise),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MintGreen
                    )

                    IconButton(
                        onClick = { item.dismiss() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss popup",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Row 2: 3-Second Countdown Progress Bar & Label
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (item.isConfidentCategory && suggestedCategory != null) {
                            "Saving as ${suggestedCategory.name} in ${remainingSeconds}s..."
                        } else {
                            "Auto-saving in ${remainingSeconds}s..."
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary
                    )

                    Text(
                        text = "${remainingSeconds}s",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MintGreen
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = MintGreen,
                    trackColor = DarkSurfaceElevated,
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Row 3: Quick Category Choices (Horizontal Scrolling Chips)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Ensure suggested category is placed first if available
                val sortedCategories = item.categories.sortedByDescending { it.id == item.suggestedCategoryId }

                sortedCategories.forEach { category ->
                    val isSuggested = category.id == item.suggestedCategoryId

                    FloatingCategoryChip(
                        category = category,
                        isSuggested = isSuggested,
                        onClick = {
                            item.commitCategory(category.id)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun FloatingCategoryChip(
    category: Category,
    isSuggested: Boolean,
    onClick: () -> Unit
) {
    val chipBorderColor = if (isSuggested) MintGreen else DarkBorder
    val chipBgColor = if (isSuggested) MintGreen.copy(alpha = 0.15f) else DarkSurfaceElevated

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(chipBgColor)
            .border(1.dp, chipBorderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val categoryColor = try {
            Color(android.graphics.Color.parseColor(category.colorHex))
        } catch (_: Exception) {
            MintGreen
        }

        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(categoryColor)
        )

        Spacer(modifier = Modifier.width(6.dp))

        Text(
            text = category.name,
            fontSize = 12.sp,
            fontWeight = if (isSuggested) FontWeight.Bold else FontWeight.Medium,
            color = if (isSuggested) TextPrimary else TextSecondary
        )

        if (isSuggested) {
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "★",
                fontSize = 10.sp,
                color = MintGreen
            )
        }
    }
}
