package com.example.pocketexpense.data.repository

import com.example.pocketexpense.data.dao.CategoryDao
import com.example.pocketexpense.data.dao.ExpenseDao
import com.example.pocketexpense.data.dao.ProcessedNotificationDao
import com.example.pocketexpense.data.database.PocketExpenseDatabase
import com.example.pocketexpense.data.model.Category
import com.example.pocketexpense.data.model.CategorySpending
import com.example.pocketexpense.data.model.Expense
import com.example.pocketexpense.data.model.ExpenseWithCategory
import com.example.pocketexpense.data.model.ProcessedNotificationRecord
import com.example.pocketexpense.parser.AutoCategoryMapper
import com.example.pocketexpense.parser.CompositeNotificationParser
import com.example.pocketexpense.parser.ParseResult
import com.example.pocketexpense.parser.PaymentDirection
import com.example.pocketexpense.parser.PaymentStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

sealed class ProcessNotificationResult {
    data class Created(
        val expense: Expense,
        val appName: String,
        val suggestedCategoryId: Long = expense.categoryId,
        val isConfidentCategory: Boolean = true
    ) : ProcessNotificationResult()
    data class Duplicate(val reason: String) : ProcessNotificationResult()
    data class Ignored(val reason: String) : ProcessNotificationResult()
    data class Ambiguous(val reason: String) : ProcessNotificationResult()
}

class ExpenseRepository(
    private val expenseDao: ExpenseDao,
    private val categoryDao: CategoryDao,
    private val processedNotificationDao: ProcessedNotificationDao,
    val compositeParser: CompositeNotificationParser = CompositeNotificationParser()
) {

    val allExpensesWithCategory: Flow<List<ExpenseWithCategory>> =
        expenseDao.getAllExpensesWithCategoryFlow()

    val totalExpenseInPaise: Flow<Long> =
        expenseDao.getTotalExpenseInPaiseFlow()

    val categorySpending: Flow<List<CategorySpending>> =
        expenseDao.getCategorySpendingFlow()

    val allCategories: Flow<List<Category>> =
        categoryDao.getAllCategoriesFlow()

    val totalExpenseCount: Flow<Int> =
        expenseDao.getExpenseCountFlow()

    fun getRecentExpenses(limit: Int = 5): Flow<List<ExpenseWithCategory>> =
        expenseDao.getRecentExpensesWithCategoryFlow(limit)

    fun getExpensesByCategory(categoryId: Long): Flow<List<ExpenseWithCategory>> =
        expenseDao.getExpensesByCategoryFlow(categoryId)

    suspend fun ensureDefaultCategories() = withContext(Dispatchers.IO) {
        PocketExpenseDatabase.populateDefaultCategories(categoryDao)
    }

    suspend fun insertExpense(expense: Expense): Long = withContext(Dispatchers.IO) {
        expenseDao.insertExpense(expense)
    }

    suspend fun updateExpenseCategory(id: Long, categoryId: Long): Int = withContext(Dispatchers.IO) {
        expenseDao.updateExpenseCategory(id, categoryId)
    }

    suspend fun updateExpense(expense: Expense): Int = withContext(Dispatchers.IO) {
        expenseDao.updateExpense(expense)
    }

    suspend fun deleteExpense(expense: Expense): Int = withContext(Dispatchers.IO) {
        expenseDao.deleteExpense(expense)
    }

    suspend fun deleteExpenseById(id: Long): Int = withContext(Dispatchers.IO) {
        expenseDao.deleteExpenseById(id)
    }

    suspend fun getExpenseById(id: Long): Expense? = withContext(Dispatchers.IO) {
        expenseDao.getExpenseById(id)
    }

    suspend fun getAllCategoriesList(): List<Category> = withContext(Dispatchers.IO) {
        categoryDao.getAllCategories()
    }

    suspend fun resetDatabase() = withContext(Dispatchers.IO) {
        expenseDao.deleteAllExpenses()
        processedNotificationDao.deleteAll()
        PocketExpenseDatabase.populateDefaultCategories(categoryDao)
    }

    /**
     * Processes an incoming payment notification from a supported UPI app.
     * Guaranteed to:
     * 1. Reject unsupported apps, non-expenses, incoming credits, refunds, and failed transactions.
     * 2. Detect and reject duplicates via reference or fallback matching.
     * 3. Insert a clean Expense and record deduplication metadata without storing raw notification text.
     */
    suspend fun processIncomingNotification(
        packageName: String,
        title: String?,
        text: String?,
        timestamp: Long = System.currentTimeMillis()
    ): ProcessNotificationResult = withContext(Dispatchers.IO) {
        // 1. Parse via composite parser
        val parseResult = compositeParser.parse(packageName, title, text, timestamp)

        when (parseResult) {
            is ParseResult.Ignored -> ProcessNotificationResult.Ignored(parseResult.reason)
            is ParseResult.Ambiguous -> ProcessNotificationResult.Ambiguous(parseResult.reason)
            is ParseResult.Success -> {
                val payment = parseResult.payment

                // 2. Verify eligibility (must be outgoing and success)
                if (payment.direction != PaymentDirection.OUTGOING || payment.status != PaymentStatus.SUCCESS) {
                    return@withContext ProcessNotificationResult.Ignored("Payment is not a successful outgoing expense")
                }
                if (payment.amountInPaise <= 0L) {
                    return@withContext ProcessNotificationResult.Ignored("Amount is zero or negative")
                }

                // 3. Duplicate Detection
                // Check 3A: By transaction reference if available
                if (!payment.transactionReference.isNullOrBlank()) {
                    val refCount = processedNotificationDao.countByTransactionReference(payment.transactionReference)
                    if (refCount > 0) {
                        return@withContext ProcessNotificationResult.Duplicate(
                            "Transaction reference '${payment.transactionReference}' has already been processed"
                        )
                    }
                }

                // Check 3B: By deterministic deduplication hash
                val dedupHash = compositeParser.generateDedupHash(
                    packageName = payment.packageName,
                    amountInPaise = payment.amountInPaise,
                    merchant = payment.merchantOrRecipient,
                    transactionReference = payment.transactionReference,
                    timestamp = payment.timestamp
                )
                if (processedNotificationDao.countByDedupHash(dedupHash) > 0) {
                    return@withContext ProcessNotificationResult.Duplicate(
                        "Notification with matching signature has already been processed"
                    )
                }

                // Check 3C: Conservative fallback window check (within 5 minutes, same amount and similar title)
                val fiveMinutesAgo = timestamp - (5 * 60 * 1000)
                val recentWithAmount = expenseDao.findRecentExpensesWithAmount(payment.amountInPaise, fiveMinutesAgo)
                val hasDuplicateRecent = recentWithAmount.any {
                    it.title.equals(payment.merchantOrRecipient, ignoreCase = true)
                }
                if (hasDuplicateRecent) {
                    return@withContext ProcessNotificationResult.Duplicate(
                        "Identical expense of ₹${payment.amountInPaise / 100} to '${payment.merchantOrRecipient}' was recorded within the last 5 minutes"
                    )
                }

                // 4. Auto-assign Category
                val isConfident = AutoCategoryMapper.hasConfidentMatch(payment.merchantOrRecipient)
                val categoryId = AutoCategoryMapper.suggestCategoryId(payment.merchantOrRecipient)

                // 5. Create and persist Expense
                val expense = Expense(
                    title = payment.merchantOrRecipient,
                    amountInPaise = payment.amountInPaise,
                    categoryId = categoryId,
                    timestamp = payment.timestamp,
                    note = "Auto-detected from ${payment.appName}",
                    transactionReference = payment.transactionReference,
                    source = "AUTO_NOTIFICATION"
                )
                val expenseId = expenseDao.insertExpense(expense)

                // 6. Record in deduplication table
                processedNotificationDao.insertRecord(
                    ProcessedNotificationRecord(
                        packageName = payment.packageName,
                        transactionReference = payment.transactionReference,
                        dedupHash = dedupHash,
                        amountInPaise = payment.amountInPaise,
                        processedTimestamp = System.currentTimeMillis(),
                        associatedExpenseId = expenseId
                    )
                )

                ProcessNotificationResult.Created(
                    expense = expense.copy(id = expenseId),
                    appName = payment.appName,
                    suggestedCategoryId = categoryId,
                    isConfidentCategory = isConfident
                )
            }
        }
    }

    suspend fun getProcessedNotificationCount(): Int = withContext(Dispatchers.IO) {
        processedNotificationDao.getTotalCount()
    }
}
