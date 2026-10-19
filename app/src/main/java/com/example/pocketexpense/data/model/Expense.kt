package com.example.pocketexpense.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "expenses",
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["categoryId"]),
        Index(value = ["timestamp"])
    ]
)
data class Expense(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val amountInPaise: Long, // 1 Rupee = 100 paise
    val categoryId: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val note: String = "",
    val transactionReference: String? = null,
    val source: String = "MANUAL" // "MANUAL" or "AUTO_NOTIFICATION"
)

