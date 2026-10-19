package com.example.pocketexpense.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val iconName: String,
    val colorHex: String,
    val isDefault: Boolean = false
) {
    companion object {
        fun defaultCategories(): List<Category> = listOf(
            Category(id = 1, name = "Food", iconName = "Restaurant", colorHex = "#FF5E57", isDefault = true),
            Category(id = 2, name = "Travel", iconName = "Flight", colorHex = "#0FB9B1", isDefault = true),
            Category(id = 3, name = "Groceries", iconName = "ShoppingCart", colorHex = "#2ED573", isDefault = true),
            Category(id = 4, name = "Shopping", iconName = "ShoppingBag", colorHex = "#FFA502", isDefault = true),
            Category(id = 5, name = "Bills", iconName = "ReceiptLong", colorHex = "#3742FA", isDefault = true),
            Category(id = 6, name = "Education", iconName = "School", colorHex = "#A55EEA", isDefault = true),
            Category(id = 7, name = "Health", iconName = "MedicalServices", colorHex = "#FF4757", isDefault = true),
            Category(id = 8, name = "Uncategorized", iconName = "Category", colorHex = "#747D8C", isDefault = true)
        )
    }
}
