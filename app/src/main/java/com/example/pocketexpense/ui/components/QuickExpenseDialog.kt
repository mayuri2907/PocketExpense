package com.example.pocketexpense.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.pocketexpense.data.model.Category
import com.example.pocketexpense.parser.AutoCategoryMapper
import com.example.pocketexpense.quick.QuickExpenseDraft
import com.example.pocketexpense.quick.QuickExpenseDraftManager
import com.example.pocketexpense.ui.theme.AmberGold
import com.example.pocketexpense.ui.theme.AppTheme
import com.example.pocketexpense.ui.theme.CoralRed
import com.example.pocketexpense.ui.theme.ElectricPurple
import com.example.pocketexpense.util.CurrencyUtils
import kotlinx.coroutines.delay

/**
 * Reusable, accessible Quick Expense Form component.
 *
 * Implements:
 * 1. INR amount input with quick-add chips (+₹50, +₹100, +₹200, +₹500).
 * 2. "What was it for?" description/merchant field with auto-category suggestion.
 * 3. 5-second visible countdown progress bar that pauses automatically upon user edit.
 * 4. Safe expiry behavior: preserves draft state when dismissed with content; never discards data.
 * 5. Rejection of blank, zero, negative, or overflow amounts.
 * 6. Debounced Save button preventing duplicate inserts.
 */
@Composable
fun QuickExpenseDialog(
    categories: List<Category>,
    onSaveExpense: (amountPaise: Long, description: String, categoryId: Long) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Dialog(
        onDismissRequest = {
            onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        QuickExpenseFormCard(
            categories = categories,
            onSaveExpense = onSaveExpense,
            onDismiss = onDismiss,
            modifier = modifier
        )
    }
}

/**
 * Shared Quick Expense card composable used by both the in-app dialog
 * and the system-wide QuickExpenseOverlayWindow.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuickExpenseFormCard(
    categories: List<Category>,
    onSaveExpense: (amountPaise: Long, description: String, categoryId: Long) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val existingDraft = remember { QuickExpenseDraftManager.getDraft() }
    val isResumingDraft = existingDraft != null

    var amountText by remember { mutableStateOf(existingDraft?.amountText ?: "") }
    var descriptionText by remember { mutableStateOf(existingDraft?.description ?: "") }
    var selectedCategoryId by remember {
        mutableStateOf(existingDraft?.categoryId ?: categories.firstOrNull()?.id ?: 1L)
    }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    // User interaction detection: Pause countdown if user interacts or pre-filled draft exists
    var hasUserInteracted by remember { mutableStateOf(isResumingDraft) }

    // 5-second countdown timer state
    val totalCountdownMillis = 5000L
    var remainingMillis by remember { mutableLongStateOf(totalCountdownMillis) }

    val amountFocusRequester = remember { FocusRequester() }

    // Countdown effect: active only while form is untouched
    LaunchedEffect(hasUserInteracted) {
        if (!hasUserInteracted) {
            val interval = 50L
            while (remainingMillis > 0) {
                delay(interval)
                remainingMillis -= interval
            }
            // Timer expired without any user interaction: dismiss cleanly without inventing data
            if (amountText.isBlank() && descriptionText.isBlank()) {
                onDismiss()
            }
        }
    }

    // Auto-save draft on dismiss if user entered partial text
    DisposableEffect(Unit) {
        onDispose {
            if (!isSaving && (amountText.isNotBlank() || descriptionText.isNotBlank())) {
                QuickExpenseDraftManager.saveDraft(
                    amountText = amountText,
                    description = descriptionText,
                    categoryId = selectedCategoryId
                )
            }
        }
    }

    val progress = (remainingMillis.toFloat() / totalCountdownMillis.toFloat()).coerceIn(0f, 1f)
    val progressColor by animateColorAsState(
        targetValue = when {
            hasUserInteracted -> colors.primary
            progress > 0.4f -> ElectricPurple
            progress > 0.2f -> AmberGold
            else -> CoralRed
        },
        label = "TimerColor"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth(0.94f)
            .clip(RoundedCornerShape(24.dp))
            .border(1.dp, colors.border, RoundedCornerShape(24.dp)),
        color = colors.surface
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Top Header Row
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
                            .background(ElectricPurple.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FlashOn,
                            contentDescription = null,
                            tint = ElectricPurple,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Quick Expense",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                        Text(
                            text = if (hasUserInteracted) "Timer paused • Draft saved" else "Auto-closing in ${(remainingMillis + 999) / 1000}s",
                            fontSize = 11.sp,
                            color = if (hasUserInteracted) colors.primary else colors.textSecondary
                        )
                    }
                }

                IconButton(
                    onClick = {
                        if (amountText.isNotBlank() || descriptionText.isNotBlank()) {
                            QuickExpenseDraftManager.saveDraft(amountText, descriptionText, selectedCategoryId)
                        }
                        onDismiss()
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = colors.textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 5-second Animated Countdown Bar
            LinearProgressIndicator(
                progress = { if (hasUserInteracted) 1f else progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(CircleShape),
                color = progressColor,
                trackColor = colors.surfaceElevated
            )

            // Resumed draft banner if draft was restored
            if (isResumingDraft && (amountText.isNotBlank() || descriptionText.isNotBlank())) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(ElectricPurple.copy(alpha = 0.12f))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = ElectricPurple,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Resumed saved draft",
                            fontSize = 11.sp,
                            color = ElectricPurple,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Text(
                        text = "Clear",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CoralRed,
                        modifier = Modifier.clickable {
                            QuickExpenseDraftManager.clearDraft()
                            amountText = ""
                            descriptionText = ""
                            errorMessage = null
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 1. Amount Field in INR
            OutlinedTextField(
                value = amountText,
                onValueChange = { input ->
                    hasUserInteracted = true
                    if (input.isEmpty() || input.matches(Regex("""^\d*\.?\d{0,2}$"""))) {
                        amountText = input
                        errorMessage = null
                    }
                },
                label = { Text("Amount in INR") },
                leadingIcon = {
                    Text(
                        text = "₹",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.primary,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                },
                placeholder = { Text("0.00", fontSize = 20.sp) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(amountFocusRequester),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.primary,
                    unfocusedBorderColor = colors.border,
                    focusedTextColor = colors.textPrimary,
                    unfocusedTextColor = colors.textPrimary,
                    focusedContainerColor = colors.surfaceElevated,
                    unfocusedContainerColor = colors.surfaceElevated
                )
            )

            // Quick Amount Increments (One-tap rapid additions)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(50, 100, 200, 500).forEach { quickVal ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.surfaceElevated)
                            .border(1.dp, colors.border, RoundedCornerShape(8.dp))
                            .clickable {
                                hasUserInteracted = true
                                val currentVal = amountText.toDoubleOrNull() ?: 0.0
                                val newVal = if (amountText.isBlank()) quickVal.toDouble() else currentVal + quickVal
                                amountText = if (newVal % 1.0 == 0.0) newVal.toLong().toString() else String.format(java.util.Locale.US, "%.2f", newVal)
                                errorMessage = null
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "+₹$quickVal",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. "What was it for?" Description Field
            OutlinedTextField(
                value = descriptionText,
                onValueChange = { input ->
                    hasUserInteracted = true
                    descriptionText = input
                    errorMessage = null
                    // Auto-suggest category if confident match exists
                    if (input.isNotBlank()) {
                        val suggestedId = AutoCategoryMapper.suggestCategoryId(input)
                        selectedCategoryId = suggestedId
                    }
                },
                label = { Text("What was it for?") },
                placeholder = { Text("e.g., Chai, Lunch, Metro, Uber") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done
                ),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.primary,
                    unfocusedBorderColor = colors.border,
                    focusedTextColor = colors.textPrimary,
                    unfocusedTextColor = colors.textPrimary,
                    focusedContainerColor = colors.surfaceElevated,
                    unfocusedContainerColor = colors.surfaceElevated
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 3. Category Selector
            Text(
                text = "Category",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textSecondary
            )
            Spacer(modifier = Modifier.height(6.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { category ->
                    val isSelected = category.id == selectedCategoryId
                    val catColor = try {
                        Color(android.graphics.Color.parseColor(category.colorHex))
                    } catch (_: Exception) {
                        colors.primary
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) catColor.copy(alpha = 0.2f) else colors.surfaceElevated)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) catColor else colors.border,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable {
                                hasUserInteracted = true
                                selectedCategoryId = category.id
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = CategoryIconHelper.getIcon(category.iconName),
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = if (isSelected) catColor else colors.textSecondary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = category.name,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) catColor else colors.textSecondary
                            )
                        }
                    }
                }
            }

            // Error message display
            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = errorMessage!!,
                    color = CoralRed,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 4. Save Button
            Button(
                onClick = {
                    if (isSaving) return@Button // Prevent duplicate rapid taps

                    // Validation: amount
                    val trimmedAmount = amountText.trim()
                    if (trimmedAmount.isBlank()) {
                        errorMessage = "Please enter an amount"
                        return@Button
                    }

                    val paise = try {
                        CurrencyUtils.rupeesStringToPaise(trimmedAmount)
                    } catch (e: Exception) {
                        errorMessage = e.message ?: "Invalid amount"
                        return@Button
                    }

                    if (paise <= 0L) {
                        errorMessage = "Amount must be greater than zero"
                        return@Button
                    }

                    val desc = descriptionText.trim().ifBlank { "Quick Expense" }

                    isSaving = true
                    QuickExpenseDraftManager.clearDraft()
                    onSaveExpense(paise, desc, selectedCategoryId)
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = !isSaving,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.primary,
                    contentColor = if (colors.isDark) Color.Black else Color.White
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isSaving) "Saving..." else "Save Expense",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
