package com.example.pocketexpense

import com.example.pocketexpense.data.model.Category
import com.example.pocketexpense.data.model.Expense
import com.example.pocketexpense.data.model.ExpenseWithCategory
import com.example.pocketexpense.parser.BaseParserUtils
import com.example.pocketexpense.parser.CompositeNotificationParser
import com.example.pocketexpense.parser.NotificationTestFixtures
import com.example.pocketexpense.parser.ParseResult
import com.example.pocketexpense.util.CsvExportHelper
import com.example.pocketexpense.util.CurrencyUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductionHardeningTest {

    @Test
    fun csvExportHelper_formatsRfc4180CompliantCsv() {
        val categoryFood = Category(id = 1, name = "Food & Dining", iconName = "Restaurant", colorHex = "#FF5E57")
        val categoryTravel = Category(id = 2, name = "Travel", iconName = "Flight", colorHex = "#0FB9B1")

        val expenses = listOf(
            ExpenseWithCategory(
                expense = Expense(
                    id = 101,
                    title = "Starbucks, Coffee", // Contains comma
                    amountInPaise = 45050L,
                    categoryId = 1,
                    timestamp = 1771480800000L, // Fixed timestamp
                    note = "Morning \"latte\" run", // Contains quotes
                    transactionReference = "UPI412356789012",
                    source = "AUTO_NOTIFICATION"
                ),
                category = categoryFood
            ),
            ExpenseWithCategory(
                expense = Expense(
                    id = 102,
                    title = "Uber Ride",
                    amountInPaise = 18900L,
                    categoryId = 2,
                    timestamp = 1771484400000L,
                    note = "Office commute\nReturn trip", // Contains newline
                    transactionReference = "T240819123456",
                    source = "MANUAL"
                ),
                category = categoryTravel
            )
        )

        val csv = CsvExportHelper.generateCsv(expenses)
        assertNotNull(csv)

        // Check header
        assertTrue("Must start with standard CSV header", csv.startsWith("ID,Date,Time,Title,Amount (INR),Category,Source,Reference,Notes\n"))

        // Check Row 1: Comma and quotes properly escaped
        assertTrue("Title with comma must be quoted", csv.contains("\"Starbucks, Coffee\""))
        assertTrue("Category name present", csv.contains("Food & Dining"))
        assertTrue("Amount formatted correctly", csv.contains("450.50"))
        assertTrue("Quotes in note escaped with double-quotes", csv.contains("\"Morning \"\"latte\"\" run\""))

        // Check Row 2: Newline in note properly enclosed in quotes per RFC-4180
        assertTrue("Multiline note enclosed in quotes", csv.contains("\"Office commute\nReturn trip\""))
        assertTrue(csv.contains("189.00"))
        assertTrue(csv.contains("Uber Ride"))
        assertTrue(csv.contains("T240819123456"))
    }

    @Test
    fun csvExportHelper_escapeCsv_handlesSpecialCharacters() {
        assertEquals("SimpleText", CsvExportHelper.escapeCsv("SimpleText"))
        assertEquals("\"Hello, World\"", CsvExportHelper.escapeCsv("Hello, World"))
        assertEquals("\"Line1\nLine2\"", CsvExportHelper.escapeCsv("Line1\nLine2"))
        assertEquals("\"He said \"\"Hi\"\"\"", CsvExportHelper.escapeCsv("He said \"Hi\""))
    }

    @Test
    fun baseParserUtils_sensitiveDataRejection_isExhaustive() {
        assertTrue(BaseParserUtils.containsSensitiveData("Your OTP is 492019 for payment"))
        assertTrue(BaseParserUtils.containsSensitiveData("Enter UPI PIN to authenticate"))
        assertTrue(BaseParserUtils.containsSensitiveData("Your verification code is 884120"))
        assertTrue(BaseParserUtils.containsSensitiveData("Do not share this secret code with anyone"))
        assertTrue(BaseParserUtils.containsSensitiveData("CVV 921 must not be shared"))
        assertTrue(BaseParserUtils.containsSensitiveData("Enter MPIN on the next screen"))

        // Legitimate expense notification must NOT be flagged as sensitive
        assertFalse(BaseParserUtils.containsSensitiveData("Paid ₹450 to Starbucks Coffee successfully"))
        assertFalse(BaseParserUtils.containsSensitiveData("Payment to Uber of ₹189 successful"))
    }

    @Test
    fun baseParserUtils_failedAndRefundRejection_isExhaustive() {
        assertTrue(BaseParserUtils.isFailedPayment("Payment of ₹500 to Swiggy failed"))
        assertTrue(BaseParserUtils.isFailedPayment("Transaction was declined by bank"))
        assertTrue(BaseParserUtils.isFailedPayment("Transfer unsuccessful due to network timeout"))
        assertTrue(BaseParserUtils.isFailedPayment("Order was cancelled by merchant"))
        assertTrue(BaseParserUtils.isFailedPayment("Transaction rejected by remitter bank"))

        assertTrue(BaseParserUtils.isRefund("Refund of ₹350 from Swiggy credited"))
        assertTrue(BaseParserUtils.isRefund("Reversal of ₹1,000 completed"))
        assertTrue(BaseParserUtils.isRefund("Amount refunded to your bank account"))
    }

    @Test
    fun currencyUtils_edgeCaseAmounts_calculatedPreciselyInPaise() {
        // Exactly 1 paisa (₹0.01)
        assertEquals(1L, CurrencyUtils.rupeesStringToPaise("0.01"))
        assertEquals("₹0.01", CurrencyUtils.paiseToRupeesString(1L))

        // Large amount: ₹99,999.99
        assertEquals(9999999L, CurrencyUtils.rupeesStringToPaise("99,999.99"))
        assertEquals("₹99,999.99", CurrencyUtils.paiseToRupeesString(9999999L))

        // Single decimal place: ₹100.5
        assertEquals(10050L, CurrencyUtils.rupeesStringToPaise("100.5"))

        // Exact hundreds: ₹5,000
        assertEquals(500000L, CurrencyUtils.rupeesStringToPaise("5000"))
        assertEquals("₹5,000.00", CurrencyUtils.paiseToRupeesString(500000L))
    }

    @Test
    fun compositeParser_dedupHash_isDeterministicAndBucketAware() {
        val parser = CompositeNotificationParser()

        // Same payment parameters within the same 5-minute bucket produce identical hash
        val t0 = 1771480000000L
        val t1 = t0 + (2 * 60 * 1000) // 2 minutes later (same bucket)
        val tNextBucket = t0 + (6 * 60 * 1000) // 6 minutes later (next bucket)

        val hash1 = parser.generateDedupHash("com.phonepe.app", 18900L, "Uber", null, t0)
        val hash2 = parser.generateDedupHash("com.phonepe.app", 18900L, "Uber", null, t1)
        val hash3 = parser.generateDedupHash("com.phonepe.app", 18900L, "Uber", null, tNextBucket)

        assertEquals("Same bucket fallback hashes must match", hash1, hash2)
        assertFalse("Different time buckets must yield different fallback hashes", hash1 == hash3)

        // When transaction reference exists, it is reference-keyed
        val refHash1 = parser.generateDedupHash("com.phonepe.app", 18900L, "Uber", "TXN999", t0)
        val refHash2 = parser.generateDedupHash("com.phonepe.app", 18900L, "Uber", "TXN999", tNextBucket)
        assertEquals("Transaction reference hash must be identical regardless of timestamp", refHash1, refHash2)
        assertEquals("REF:com.phonepe.app:TXN999", refHash1)
    }

    @Test
    fun testSimulator_isExcludedFromProductionCode() {
        // Verify NotificationSimulatorDialog is not present in runtime classpath
        val isSimulatorPresent = try {
            Class.forName("com.example.pocketexpense.ui.components.NotificationSimulatorDialog")
            true
        } catch (_: ClassNotFoundException) {
            false
        }
        assertFalse(
            "NotificationSimulatorDialog must NOT be present in production codebase",
            isSimulatorPresent
        )

        // Verify test fixtures are available only in test classpath
        assertNotNull(NotificationTestFixtures.allFixtures)
        assertEquals(9, NotificationTestFixtures.allFixtures.size)
    }

    @Test
    fun compositeParser_handlesEmptyAndMalformedNotificationsSafely() {
        val parser = CompositeNotificationParser()

        val emptyResult = parser.parse("com.google.android.apps.nbu.paisa.user", "", "")
        assertTrue("Empty notification must be ignored", emptyResult is ParseResult.Ignored)

        val whitespaceResult = parser.parse("com.google.android.apps.nbu.paisa.user", "   ", "   ")
        assertTrue("Whitespace notification must be ignored", whitespaceResult is ParseResult.Ignored)

        val unsupportedResult = parser.parse("com.whatsapp", "Paid ₹100", "to friend")
        assertTrue("Unsupported package must be ignored", unsupportedResult is ParseResult.Ignored)
    }
}
