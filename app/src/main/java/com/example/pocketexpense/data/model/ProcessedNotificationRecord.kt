package com.example.pocketexpense.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Persists deduplication records for parsed notifications.
 * Notice: We NEVER store raw notification body, OTPs, or banking credentials here.
 * Only sanitized metadata (package, reference, amount, hash, timestamp) is kept locally.
 */
@Entity(
    tableName = "processed_notifications",
    indices = [
        Index(value = ["transactionReference"]),
        Index(value = ["dedupHash"], unique = true)
    ]
)
data class ProcessedNotificationRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val packageName: String,
    val transactionReference: String?,
    val dedupHash: String,
    val amountInPaise: Long,
    val processedTimestamp: Long = System.currentTimeMillis(),
    val associatedExpenseId: Long? = null
)
