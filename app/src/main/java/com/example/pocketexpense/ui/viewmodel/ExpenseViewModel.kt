package com.example.pocketexpense.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.pocketexpense.data.model.Category
import com.example.pocketexpense.data.model.CategorySpending
import com.example.pocketexpense.data.model.Expense
import com.example.pocketexpense.data.model.ExpenseWithCategory
import com.example.pocketexpense.data.repository.ExpenseRepository
import com.example.pocketexpense.util.CurrencyUtils
import com.example.pocketexpense.util.ValidationResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DashboardUiState(
    val totalExpensePaise: Long = 0L,
    val recentExpenses: List<ExpenseWithCategory> = emptyList(),
    val categorySpending: List<CategorySpending> = emptyList(),
    val isLoading: Boolean = false
)

data class TransactionsUiState(
    val filteredExpenses: List<ExpenseWithCategory> = emptyList(),
    val categories: List<Category> = emptyList(),
    val selectedCategoryId: Long? = null,
    val searchQuery: String = ""
)

data class CategoriesUiState(
    val categorySpending: List<CategorySpending> = emptyList(),
    val totalExpensePaise: Long = 0L
)

data class SettingsUiState(
    val totalExpenseCount: Int = 0,
    val totalCategoryCount: Int = 0,
    val databaseName: String = "pocket_expense_db"
)

class ExpenseViewModel(
    val repository: ExpenseRepository
) : ViewModel() {

    // Filter states
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _selectedCategoryId = MutableStateFlow<Long?>(null)
    val selectedCategoryId: StateFlow<Long?> = _selectedCategoryId

    // UI feedback message
    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage

    // 1. Reactive Dashboard State
    val dashboardUiState: StateFlow<DashboardUiState> = combine(
        repository.totalExpenseInPaise,
        repository.getRecentExpenses(6),
        repository.categorySpending
    ) { totalPaise, recent, categorySpending ->
        DashboardUiState(
            totalExpensePaise = totalPaise,
            recentExpenses = recent,
            categorySpending = categorySpending,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState(isLoading = true)
    )

    // 2. Reactive Transactions State with search and category filtering
    val transactionsUiState: StateFlow<TransactionsUiState> = combine(
        repository.allExpensesWithCategory,
        repository.allCategories,
        _selectedCategoryId,
        _searchQuery
    ) { allExpenses, categories, selectedCatId, query ->
        val filtered = allExpenses.filter { item ->
            val matchesCategory = selectedCatId == null || item.expense.categoryId == selectedCatId
            val matchesQuery = query.isBlank() ||
                    item.expense.title.contains(query, ignoreCase = true) ||
                    item.category.name.contains(query, ignoreCase = true) ||
                    item.expense.note.contains(query, ignoreCase = true)
            matchesCategory && matchesQuery
        }
        TransactionsUiState(
            filteredExpenses = filtered,
            categories = categories,
            selectedCategoryId = selectedCatId,
            searchQuery = query
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TransactionsUiState()
    )

    // 3. Reactive Categories State
    val categoriesUiState: StateFlow<CategoriesUiState> = combine(
        repository.categorySpending,
        repository.totalExpenseInPaise
    ) { spendingList, totalPaise ->
        CategoriesUiState(
            categorySpending = spendingList,
            totalExpensePaise = totalPaise
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CategoriesUiState()
    )

    // 4. Reactive Settings State
    val settingsUiState: StateFlow<SettingsUiState> = combine(
        repository.totalExpenseCount,
        repository.allCategories
    ) { count, categories ->
        SettingsUiState(
            totalExpenseCount = count,
            totalCategoryCount = categories.size
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    // Public Actions
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedCategory(categoryId: Long?) {
        _selectedCategoryId.value = categoryId
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    /**
     * Adds an expense after validating title and amount.
     * Returns a string error message if validation fails, or null on success.
     */
    fun addExpense(
        title: String,
        amountRupees: String,
        categoryId: Long,
        note: String = "",
        timestamp: Long = System.currentTimeMillis()
    ): String? {
        val validation = CurrencyUtils.validateExpenseInput(title, amountRupees)
        return when (validation) {
            is ValidationResult.Error -> validation.message
            is ValidationResult.Success -> {
                viewModelScope.launch {
                    val expense = Expense(
                        title = title.trim(),
                        amountInPaise = validation.amountInPaise,
                        categoryId = categoryId,
                        timestamp = timestamp,
                        note = note.trim()
                    )
                    repository.insertExpense(expense)
                    _userMessage.value = "Expense added successfully"
                }
                null
            }
        }
    }

    /**
     * Updates an existing expense.
     * Returns a string error message if validation fails, or null on success.
     */
    fun updateExpense(
        id: Long,
        title: String,
        amountRupees: String,
        categoryId: Long,
        note: String = "",
        timestamp: Long = System.currentTimeMillis()
    ): String? {
        val validation = CurrencyUtils.validateExpenseInput(title, amountRupees)
        return when (validation) {
            is ValidationResult.Error -> validation.message
            is ValidationResult.Success -> {
                viewModelScope.launch {
                    val expense = Expense(
                        id = id,
                        title = title.trim(),
                        amountInPaise = validation.amountInPaise,
                        categoryId = categoryId,
                        timestamp = timestamp,
                        note = note.trim()
                    )
                    repository.updateExpense(expense)
                    _userMessage.value = "Expense updated successfully"
                }
                null
            }
        }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            repository.deleteExpense(expense)
            _userMessage.value = "Expense deleted"
        }
    }

    fun resetDatabase() {
        viewModelScope.launch {
            repository.resetDatabase()
            _userMessage.value = "Database reset to defaults"
        }
    }

    fun exportCsv(onComplete: (String) -> Unit) {
        viewModelScope.launch {
            val expenses = repository.allExpensesWithCategory.first()
            val csv = com.example.pocketexpense.util.CsvExportHelper.generateCsv(expenses)
            onComplete(csv)
        }
    }

    companion object {
        fun provideFactory(repository: ExpenseRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ExpenseViewModel(repository) as T
                }
            }
    }
}
