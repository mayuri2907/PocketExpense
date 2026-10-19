package com.example.pocketexpense.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.pocketexpense.data.model.CategorySpending
import com.example.pocketexpense.data.model.Expense
import com.example.pocketexpense.data.model.ExpenseWithCategory
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {

    @Transaction
    @Query("SELECT * FROM expenses ORDER BY timestamp DESC")
    fun getAllExpensesWithCategoryFlow(): Flow<List<ExpenseWithCategory>>

    @Transaction
    @Query("SELECT * FROM expenses ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentExpensesWithCategoryFlow(limit: Int = 10): Flow<List<ExpenseWithCategory>>

    @Transaction
    @Query("SELECT * FROM expenses WHERE categoryId = :categoryId ORDER BY timestamp DESC")
    fun getExpensesByCategoryFlow(categoryId: Long): Flow<List<ExpenseWithCategory>>

    @Query("SELECT COALESCE(SUM(amountInPaise), 0) FROM expenses")
    fun getTotalExpenseInPaiseFlow(): Flow<Long>

    @Query("""
        SELECT 
            c.id AS categoryId,
            c.name AS categoryName,
            c.iconName AS iconName,
            c.colorHex AS colorHex,
            COALESCE(SUM(e.amountInPaise), 0) AS totalPaise,
            COUNT(e.id) AS transactionCount
        FROM categories c
        LEFT JOIN expenses e ON c.id = e.categoryId
        GROUP BY c.id
        ORDER BY totalPaise DESC, c.id ASC
    """)
    fun getCategorySpendingFlow(): Flow<List<CategorySpending>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: Expense): Long

    @Update
    suspend fun updateExpense(expense: Expense): Int

    @Query("UPDATE expenses SET categoryId = :categoryId WHERE id = :id")
    suspend fun updateExpenseCategory(id: Long, categoryId: Long): Int

    @Delete
    suspend fun deleteExpense(expense: Expense): Int

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteExpenseById(id: Long): Int

    @Query("SELECT * FROM expenses WHERE id = :id LIMIT 1")
    suspend fun getExpenseById(id: Long): Expense?

    @Query("SELECT COUNT(*) FROM expenses")
    fun getExpenseCountFlow(): Flow<Int>

    @Query("SELECT * FROM expenses WHERE amountInPaise = :amountInPaise AND timestamp >= :sinceTimestamp")
    suspend fun findRecentExpensesWithAmount(amountInPaise: Long, sinceTimestamp: Long): List<Expense>

    @Query("DELETE FROM expenses")
    suspend fun deleteAllExpenses(): Int
}
