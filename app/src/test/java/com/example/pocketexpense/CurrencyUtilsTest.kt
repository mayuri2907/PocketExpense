package com.example.pocketexpense

import com.example.pocketexpense.util.CurrencyUtils
import com.example.pocketexpense.util.ValidationResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CurrencyUtilsTest {

    @Test
    fun paiseToRupeesString_formatsCorrectly() {
        // Zero paise
        assertEquals("₹0.00", CurrencyUtils.paiseToRupeesString(0L))

        // Fifty paise
        assertEquals("₹0.50", CurrencyUtils.paiseToRupeesString(50L))

        // One rupee
        assertEquals("₹1.00", CurrencyUtils.paiseToRupeesString(100L))

        // Fractional amount (450.75)
        assertEquals("₹450.75", CurrencyUtils.paiseToRupeesString(45075L))

        // Large amount with Indian comma grouping (1,23,456.78)
        assertEquals("₹1,23,456.78", CurrencyUtils.paiseToRupeesString(12345678L))

        // Without symbol
        assertEquals("450.75", CurrencyUtils.paiseToRupeesString(45075L, includeSymbol = false))
    }

    @Test
    fun rupeesStringToPaise_parsesValidInputs() {
        assertEquals(100L, CurrencyUtils.rupeesStringToPaise("1"))
        assertEquals(100L, CurrencyUtils.rupeesStringToPaise("1.0"))
        assertEquals(100L, CurrencyUtils.rupeesStringToPaise("1.00"))
        assertEquals(45050L, CurrencyUtils.rupeesStringToPaise("450.5"))
        assertEquals(45050L, CurrencyUtils.rupeesStringToPaise("450.50"))
        assertEquals(1250000L, CurrencyUtils.rupeesStringToPaise("12,500.00"))
        assertEquals(50000L, CurrencyUtils.rupeesStringToPaise(" ₹500 "))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rupeesStringToPaise_throwsOnEmpty() {
        CurrencyUtils.rupeesStringToPaise("")
    }

    @Test(expected = IllegalArgumentException::class)
    fun rupeesStringToPaise_throwsOnZero() {
        CurrencyUtils.rupeesStringToPaise("0")
    }

    @Test(expected = IllegalArgumentException::class)
    fun rupeesStringToPaise_throwsOnNegative() {
        CurrencyUtils.rupeesStringToPaise("-100")
    }

    @Test(expected = IllegalArgumentException::class)
    fun rupeesStringToPaise_throwsOnInvalidCharacters() {
        CurrencyUtils.rupeesStringToPaise("abc")
    }

    @Test(expected = IllegalArgumentException::class)
    fun rupeesStringToPaise_throwsOnMoreThanTwoDecimals() {
        CurrencyUtils.rupeesStringToPaise("10.999")
    }

    @Test
    fun paiseToEditableString_formatsExpectedly() {
        assertEquals("450.50", CurrencyUtils.paiseToEditableString(45050L))
        assertEquals("100.00", CurrencyUtils.paiseToEditableString(10000L))
        assertEquals("0.05", CurrencyUtils.paiseToEditableString(5L))
    }

    @Test
    fun validateExpenseInput_validatesProperly() {
        // Empty title
        val emptyTitleResult = CurrencyUtils.validateExpenseInput("", "100")
        assertTrue(emptyTitleResult is ValidationResult.Error)
        assertEquals("Title cannot be empty", (emptyTitleResult as ValidationResult.Error).message)

        // Whitespace title
        val blankTitleResult = CurrencyUtils.validateExpenseInput("   ", "100")
        assertTrue(blankTitleResult is ValidationResult.Error)

        // Invalid amount
        val invalidAmountResult = CurrencyUtils.validateExpenseInput("Coffee", "-50")
        assertTrue(invalidAmountResult is ValidationResult.Error)

        // Zero amount
        val zeroAmountResult = CurrencyUtils.validateExpenseInput("Coffee", "0")
        assertTrue(zeroAmountResult is ValidationResult.Error)

        // Valid input
        val validResult = CurrencyUtils.validateExpenseInput("Starbucks Coffee", "350.50")
        assertTrue(validResult is ValidationResult.Success)
        assertEquals(35050L, (validResult as ValidationResult.Success).amountInPaise)
    }
}
