package com.example.pocketexpense.floating

import android.content.Context
import com.example.pocketexpense.data.model.Category
import com.example.pocketexpense.data.model.Expense
import com.example.pocketexpense.data.repository.ExpenseRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FloatingExpenseManager(
    private val context: Context,
    val repository: ExpenseRepository,
    private val queueCapacity: Int = 5
) {
    private val managerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val queue = FloatingExpenseQueue(maxCapacity = queueCapacity)
    private val overlayWindow = FloatingOverlayWindow(context)

    private val _activePendingExpense = MutableStateFlow<PendingExpenseItem?>(null)
    val activePendingExpense: StateFlow<PendingExpenseItem?> = _activePendingExpense.asStateFlow()

    private var cachedCategories: List<Category> = Category.defaultCategories()

    init {
        managerScope.launch {
            val dbCategories = repository.getAllCategoriesList()
            if (dbCategories.isNotEmpty()) {
                cachedCategories = dbCategories
            }
        }
    }

    /**
     * Enqueues an auto-detected expense for 3-second floating popup categorization.
     * Note: The expense has ALREADY been durably written to Room with its default suggestion,
     * ensuring zero data loss even if overlay is missing, cancelled, or the process dies.
     */
    fun enqueueAndShow(
        expense: Expense,
        appName: String,
        suggestedCategoryId: Long = expense.categoryId,
        isConfidentCategory: Boolean = true,
        onCommitted: ((Long) -> Unit)? = null
    ) {
        val categories = cachedCategories.ifEmpty { Category.defaultCategories() }

        lateinit var pendingItem: PendingExpenseItem
        pendingItem = PendingExpenseItem(
            expenseId = expense.id,
            amountInPaise = expense.amountInPaise,
            merchant = expense.title,
            suggestedCategoryId = suggestedCategoryId,
            isConfidentCategory = isConfidentCategory,
            appName = appName,
            categories = categories,
            onCategoryCommitted = { chosenCategoryId ->
                managerScope.launch {
                    repository.updateExpenseCategory(expense.id, chosenCategoryId)
                    onCommitted?.invoke(chosenCategoryId)
                }
                overlayWindow.dismiss()
                showNextQueuedItem()
            },
            onDismissed = {
                // Already durably recorded with default category; just advance queue
                overlayWindow.dismiss()
                showNextQueuedItem()
            }
        )

        queue.enqueue(pendingItem)

        // If no popup is currently active, display this item immediately
        if (_activePendingExpense.value == null) {
            showNextQueuedItem()
        }
    }

    private fun showNextQueuedItem() {
        val nextItem = queue.poll()
        _activePendingExpense.value = nextItem

        if (nextItem != null) {
            if (OverlayPermissionHelper.canDrawOverlays(context)) {
                overlayWindow.show(nextItem)
            }
        } else {
            overlayWindow.dismiss()
        }
    }

    fun dismissCurrent() {
        _activePendingExpense.value?.dismiss()
    }

    fun clearAll() {
        queue.clear()
        _activePendingExpense.value = null
        overlayWindow.dismiss()
    }
}
