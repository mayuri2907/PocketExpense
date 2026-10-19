package com.example.pocketexpense.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Utility functions for handling Indian Rupee currency representation in integer paise.
 * Storing currency as integer paise (1 Rupee = 100 paise) avoids floating-point precision issues.
 */
object CurrencyUtils {

    /**
     * Converts integer paise to a formatted Rupee string with the ₹ symbol.
     * Uses the Indian numbering system (e.g. ₹1,23,456.78).
     * Example: 45050L -> "₹450.50", 100000L -> "₹1,000.00", 0L -> "₹0.00"
     */
    fun paiseToRupeesString(paise: Long, includeSymbol: Boolean = true): String {
        val isNegative = paise < 0
        val absPaise = Math.abs(paise)
        val rupees = absPaise / 100
        val remainingPaise = absPaise % 100

        val rupeesStr = rupees.toString()
        val formattedRupees = if (rupeesStr.length > 3) {
            val lastThree = rupeesStr.substring(rupeesStr.length - 3)
            val remaining = rupeesStr.substring(0, rupeesStr.length - 3)
            val groupedRemaining = remaining.reversed().chunked(2).joinToString(",").reversed()
            "$groupedRemaining,$lastThree"
        } else {
            rupeesStr
        }

        val formattedNumber = "$formattedRupees.%02d".format(remainingPaise)
        val prefix = if (isNegative) "-" else ""
        return if (includeSymbol) "$prefix₹$formattedNumber" else "$prefix$formattedNumber"
    }

    /**
     * Parses a user-input Rupee string into integer paise.
     * Handles inputs like "450", "450.5", "450.50", "1,000.25", etc.
     * Throws [IllegalArgumentException] if the input cannot be parsed or represents a negative amount.
     */
    fun rupeesStringToPaise(input: String): Long {
        val sanitized = input.trim().replace(",", "").replace("₹", "")
        if (sanitized.isEmpty()) {
            throw IllegalArgumentException("Amount cannot be empty")
        }

        // Validate number format with optional decimal up to 2 decimal places
        if (!sanitized.matches(Regex("""^\d+(\.\d{1,2})?$"""))) {
            throw IllegalArgumentException("Invalid amount format. Enter a valid positive number up to 2 decimal places.")
        }

        val parts = sanitized.split(".")
        val rupeesPart = parts[0].toLongOrNull() ?: throw IllegalArgumentException("Amount is too large")
        
        val paisePart = if (parts.size > 1) {
            val decimals = parts[1]
            when (decimals.length) {
                1 -> decimals.toInt() * 10
                2 -> decimals.toInt()
                else -> throw IllegalArgumentException("Maximum 2 decimal places allowed")
            }
        } else {
            0
        }

        val totalPaise = (rupeesPart * 100L) + paisePart
        if (totalPaise <= 0L) {
            throw IllegalArgumentException("Amount must be greater than zero")
        }

        return totalPaise
    }

    /**
     * Converts a Long paise value back to an editable decimal string.
     * Example: 45050L -> "450.50", 10000L -> "100.00"
     */
    fun paiseToEditableString(paise: Long): String {
        val absPaise = Math.abs(paise)
        val rupees = absPaise / 100
        val rem = absPaise % 100
        return "%d.%02d".format(rupees, rem)
    }

    /**
     * Validates expense inputs: title and amount.
     */
    fun validateExpenseInput(title: String, amountString: String): ValidationResult {
        val trimmedTitle = title.trim()
        if (trimmedTitle.isEmpty()) {
            return ValidationResult.Error("Title cannot be empty")
        }
        if (trimmedTitle.length > 100) {
            return ValidationResult.Error("Title must be 100 characters or fewer")
        }

        return try {
            val paise = rupeesStringToPaise(amountString)
            ValidationResult.Success(paise)
        } catch (e: IllegalArgumentException) {
            ValidationResult.Error(e.message ?: "Invalid amount")
        }
    }
}

sealed class ValidationResult {
    data class Success(val amountInPaise: Long) : ValidationResult()
    data class Error(val message: String) : ValidationResult()
}
