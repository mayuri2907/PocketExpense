package com.example.pocketexpense.quick

/**
 * Data holder for an in-progress Quick Expense draft.
 */
data class QuickExpenseDraft(
    val amountText: String = "",
    val description: String = "",
    val categoryId: Long? = null,
    val savedAtTimestamp: Long = System.currentTimeMillis()
)

/**
 * Thread-safe draft manager preserving user-entered data across dismissals,
 * countdown expiries, and lifecycle events.
 *
 * Rule 6 & 7 Compliance:
 * - Never silently discards entered user data.
 * - Allows resuming drafts seamlessly.
 * - Automatically cleared once successfully committed to Room database.
 */
object QuickExpenseDraftManager {

    private var currentDraft: QuickExpenseDraft? = null

    @Synchronized
    fun saveDraft(amountText: String, description: String, categoryId: Long?) {
        // Only preserve if the user has entered meaningful text or amount
        if (amountText.isNotBlank() || description.isNotBlank()) {
            currentDraft = QuickExpenseDraft(
                amountText = amountText.trim(),
                description = description.trim(),
                categoryId = categoryId,
                savedAtTimestamp = System.currentTimeMillis()
            )
        }
    }

    @Synchronized
    fun getDraft(): QuickExpenseDraft? = currentDraft

    @Synchronized
    fun clearDraft() {
        currentDraft = null
    }

    @Synchronized
    fun hasDraft(): Boolean = currentDraft != null
}
