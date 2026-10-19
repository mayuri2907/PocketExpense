package com.example.pocketexpense.data.model

data class CategorySpending(
    val categoryId: Long,
    val categoryName: String,
    val iconName: String,
    val colorHex: String,
    val totalPaise: Long,
    val transactionCount: Int
)
