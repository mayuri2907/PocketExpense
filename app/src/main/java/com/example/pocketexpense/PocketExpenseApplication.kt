package com.example.pocketexpense

import android.app.Application
import com.example.pocketexpense.data.database.PocketExpenseDatabase
import com.example.pocketexpense.data.repository.ExpenseRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class PocketExpenseApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database: PocketExpenseDatabase by lazy {
        PocketExpenseDatabase.getDatabase(this, applicationScope)
    }

    val repository: ExpenseRepository by lazy {
        ExpenseRepository(
            expenseDao = database.expenseDao(),
            categoryDao = database.categoryDao(),
            processedNotificationDao = database.processedNotificationDao()
        )
    }

    val floatingExpenseManager: com.example.pocketexpense.floating.FloatingExpenseManager by lazy {
        com.example.pocketexpense.floating.FloatingExpenseManager(this, repository)
    }

    override fun onCreate() {
        super.onCreate()
        // Ensure default categories exist on app startup
        applicationScope.launch {
            repository.ensureDefaultCategories()
        }
    }
}
