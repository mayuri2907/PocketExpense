package com.example.pocketexpense.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.pocketexpense.data.model.ProcessedNotificationRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface ProcessedNotificationDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertRecord(record: ProcessedNotificationRecord): Long

    @Query("SELECT * FROM processed_notifications WHERE dedupHash = :hash LIMIT 1")
    suspend fun getByDedupHash(hash: String): ProcessedNotificationRecord?

    @Query("SELECT COUNT(*) FROM processed_notifications WHERE dedupHash = :hash")
    suspend fun countByDedupHash(hash: String): Int

    @Query("SELECT COUNT(*) FROM processed_notifications WHERE transactionReference IS NOT NULL AND transactionReference = :reference")
    suspend fun countByTransactionReference(reference: String): Int

    @Query("SELECT * FROM processed_notifications ORDER BY processedTimestamp DESC LIMIT :limit")
    fun getRecentRecordsFlow(limit: Int = 20): Flow<List<ProcessedNotificationRecord>>

    @Query("SELECT COUNT(*) FROM processed_notifications")
    suspend fun getTotalCount(): Int

    @Query("DELETE FROM processed_notifications")
    suspend fun deleteAll(): Int
}
