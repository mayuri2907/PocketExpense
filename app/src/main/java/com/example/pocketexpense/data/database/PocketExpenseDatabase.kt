package com.example.pocketexpense.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.pocketexpense.data.dao.CategoryDao
import com.example.pocketexpense.data.dao.ExpenseDao
import com.example.pocketexpense.data.dao.ProcessedNotificationDao
import com.example.pocketexpense.data.model.Category
import com.example.pocketexpense.data.model.Expense
import com.example.pocketexpense.data.model.ProcessedNotificationRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [Category::class, Expense::class, ProcessedNotificationRecord::class],
    version = 2,
    exportSchema = false
)
abstract class PocketExpenseDatabase : RoomDatabase() {

    abstract fun categoryDao(): CategoryDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun processedNotificationDao(): ProcessedNotificationDao

    companion object {
        @Volatile
        private var INSTANCE: PocketExpenseDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add columns to existing expenses table
                db.execSQL("ALTER TABLE expenses ADD COLUMN transactionReference TEXT")
                db.execSQL("ALTER TABLE expenses ADD COLUMN source TEXT NOT NULL DEFAULT 'MANUAL'")

                // Create processed_notifications table for persistent deduplication
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS processed_notifications (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        packageName TEXT NOT NULL,
                        transactionReference TEXT,
                        dedupHash TEXT NOT NULL,
                        amountInPaise INTEGER NOT NULL,
                        processedTimestamp INTEGER NOT NULL,
                        associatedExpenseId INTEGER
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS index_processed_notifications_transactionReference ON processed_notifications (transactionReference)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_processed_notifications_dedupHash ON processed_notifications (dedupHash)")
            }
        }

        fun getDatabase(
            context: Context,
            scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
        ): PocketExpenseDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PocketExpenseDatabase::class.java,
                    "pocket_expense_db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigrationOnDowngrade()
                    .addCallback(DatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateDefaultCategories(database.categoryDao())
                    }
                }
            }
        }

        suspend fun populateDefaultCategories(categoryDao: CategoryDao) {
            if (categoryDao.getCategoryCount() == 0) {
                categoryDao.insertCategories(Category.defaultCategories())
            }
        }
    }
}
