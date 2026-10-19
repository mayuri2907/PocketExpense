package com.example.pocketexpense.floating

import com.example.pocketexpense.data.model.Category
import java.util.concurrent.atomic.AtomicBoolean

data class PendingExpenseItem(
    val expenseId: Long,
    val amountInPaise: Long,
    val merchant: String,
    val suggestedCategoryId: Long,
    val isConfidentCategory: Boolean,
    val appName: String,
    val categories: List<Category>,
    val onCategoryCommitted: (categoryId: Long) -> Unit,
    val onDismissed: () -> Unit
) {
    private val isDecisionCommitted = AtomicBoolean(false)

    /**
     * Commits the selected category atomically.
     * Guarantees only one final decision is ever committed per transaction.
     */
    fun commitCategory(categoryId: Long): Boolean {
        if (isDecisionCommitted.compareAndSet(false, true)) {
            onCategoryCommitted(categoryId)
            return true
        }
        return false
    }

    /**
     * Commits the default category (confident suggestion or 8L Uncategorized) on timeout.
     */
    fun commitDefault(): Boolean {
        if (isDecisionCommitted.compareAndSet(false, true)) {
            onCategoryCommitted(suggestedCategoryId)
            return true
        }
        return false
    }

    /**
     * Dismisses the popup, finalizing with default category if not already committed.
     */
    fun dismiss(): Boolean {
        if (isDecisionCommitted.compareAndSet(false, true)) {
            onDismissed()
            return true
        }
        return false
    }

    val isFinalized: Boolean get() = isDecisionCommitted.get()
}
