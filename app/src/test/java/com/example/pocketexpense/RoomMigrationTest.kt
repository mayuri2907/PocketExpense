package com.example.pocketexpense

import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.pocketexpense.data.database.PocketExpenseDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Proxy

class RoomMigrationTest {

    @Test
    fun migration1to2_executesRequiredDdlStatementsInOrder() {
        val executedStatements = mutableListOf<String>()

        // Create a dynamic proxy for SupportSQLiteDatabase to record execSQL statements without framework stubs
        val dbProxy = Proxy.newProxyInstance(
            SupportSQLiteDatabase::class.java.classLoader,
            arrayOf(SupportSQLiteDatabase::class.java)
        ) { _, method, args ->
            if (method.name == "execSQL") {
                val sql = args?.get(0)?.toString() ?: ""
                executedStatements.add(sql.trim())
                null
            } else {
                null
            }
        } as SupportSQLiteDatabase

        val migration = PocketExpenseDatabase.MIGRATION_1_2
        assertEquals(1, migration.startVersion)
        assertEquals(2, migration.endVersion)

        migration.migrate(dbProxy)

        // 1. Must alter expenses table to add transactionReference
        assertTrue(
            "Must add transactionReference column to expenses",
            executedStatements.any { it.contains("ALTER TABLE expenses ADD COLUMN transactionReference TEXT", ignoreCase = true) }
        )

        // 2. Must alter expenses table to add source with default 'MANUAL'
        assertTrue(
            "Must add source column with default 'MANUAL' to expenses",
            executedStatements.any { it.contains("ALTER TABLE expenses ADD COLUMN source TEXT NOT NULL DEFAULT 'MANUAL'", ignoreCase = true) }
        )

        // 3. Must create processed_notifications table for persistent deduplication
        assertTrue(
            "Must create processed_notifications table",
            executedStatements.any {
                it.contains("CREATE TABLE IF NOT EXISTS processed_notifications", ignoreCase = true) &&
                        it.contains("dedupHash TEXT NOT NULL", ignoreCase = true)
            }
        )

        // 4. Must create indexes on transactionReference and dedupHash
        assertTrue(
            "Must create index on transactionReference",
            executedStatements.any { it.contains("index_processed_notifications_transactionReference", ignoreCase = true) }
        )
        assertTrue(
            "Must create unique index on dedupHash",
            executedStatements.any { it.contains("index_processed_notifications_dedupHash", ignoreCase = true) && it.contains("UNIQUE", ignoreCase = true) }
        )
    }
}
