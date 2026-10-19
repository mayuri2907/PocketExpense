package com.example.pocketexpense.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

object CategoryIconHelper {

    fun getIcon(iconName: String): ImageVector {
        return when (iconName.lowercase()) {
            "restaurant", "food" -> Icons.Default.Restaurant
            "flight", "travel" -> Icons.Default.Flight
            "shoppingcart", "groceries" -> Icons.Default.ShoppingCart
            "shoppingbag", "shopping" -> Icons.Default.ShoppingBag
            "receiptlong", "receipt", "bills" -> Icons.Default.Receipt
            "school", "education" -> Icons.Default.School
            "medicalservices", "health" -> Icons.Default.MedicalServices
            else -> Icons.Default.Category
        }
    }

    fun parseColor(hex: String, default: Color = Color(0xFF747D8C)): Color {
        return try {
            val cleanHex = hex.removePrefix("#")
            val colorLong = if (cleanHex.length == 6) {
                "FF$cleanHex".toLong(16)
            } else {
                cleanHex.toLong(16)
            }
            Color(colorLong)
        } catch (e: Exception) {
            default
        }
    }
}
