package com.example.pocketexpense

import com.example.pocketexpense.data.model.Category
import com.example.pocketexpense.data.model.CategorySpending
import com.example.pocketexpense.data.model.Expense
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Test

class ExpenseCalculationTest {

    @Test
    fun defaultCategories_hasEightUniqueCategories() {
        val categories = Category.defaultCategories()
        assertEquals(8, categories.size)

        // Ensure unique IDs
        val ids = categories.map { it.id }.toSet()
        assertEquals(8, ids.size)

        // Ensure unique names
        val names = categories.map { it.name }.toSet()
        assertEquals(8, names.size)

        // Verify required names are present
        val expected = setOf("Food", "Travel", "Groceries", "Shopping", "Bills", "Education", "Health", "Uncategorized")
        assertEquals(expected, names)
    }

    @Test
    fun calculateGrandTotal_sumsPaiseCorrectly() {
        val expenses = listOf(
            Expense(id = 1, title = "Lunch", amountInPaise = 25000L, categoryId = 1), // ₹250.00
            Expense(id = 2, title = "Metro", amountInPaise = 4000L, categoryId = 2),  // ₹40.00
            Expense(id = 3, title = "Milk", amountInPaise = 6550L, categoryId = 3)    // ₹65.50
        )

        val totalPaise = expenses.sumOf { it.amountInPaise }
        assertEquals(35550L, totalPaise) // ₹355.50
    }

    @Test
    fun calculateCategoryTotals_groupsAndCalculatesAccurately() {
        val expenses = listOf(
            Expense(id = 1, title = "Pizza", amountInPaise = 50000L, categoryId = 1),
            Expense(id = 2, title = "Burger", amountInPaise = 30000L, categoryId = 1),
            Expense(id = 3, title = "Taxi", amountInPaise = 20000L, categoryId = 2)
        )

        val grouped = expenses.groupBy { it.categoryId }
        val foodTotal = grouped[1]?.sumOf { it.amountInPaise } ?: 0L
        val travelTotal = grouped[2]?.sumOf { it.amountInPaise } ?: 0L
        val shoppingTotal = grouped[4]?.sumOf { it.amountInPaise } ?: 0L

        assertEquals(80000L, foodTotal)
        assertEquals(20000L, travelTotal)
        assertEquals(0L, shoppingTotal)
    }

    @Test
    fun calculateCategoryPercentage_calculatesProportions() {
        val categorySpendings = listOf(
            CategorySpending(1, "Food", "Restaurant", "#FF5E57", totalPaise = 60000L, transactionCount = 2),
            CategorySpending(2, "Travel", "Flight", "#0FB9B1", totalPaise = 40000L, transactionCount = 1)
        )

        val grandTotal = categorySpendings.sumOf { it.totalPaise }
        assertEquals(100000L, grandTotal)

        val foodRatio = categorySpendings[0].totalPaise.toFloat() / grandTotal
        val travelRatio = categorySpendings[1].totalPaise.toFloat() / grandTotal

        assertEquals(0.60f, foodRatio, 0.001f)
        assertEquals(0.40f, travelRatio, 0.001f)
    }

    @Test
    fun calculateCategoryPercentage_handlesZeroGrandTotal() {
        val totalPaise = 0L
        val categoryPaise = 0L
        val percentage = if (totalPaise > 0) (categoryPaise.toFloat() / totalPaise) else 0f
        assertEquals(0f, percentage, 0.001f)
    }
}
