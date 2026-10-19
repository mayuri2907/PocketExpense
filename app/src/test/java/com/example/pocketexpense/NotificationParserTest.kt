package com.example.pocketexpense

import com.example.pocketexpense.parser.AutoCategoryMapper
import com.example.pocketexpense.parser.CompositeNotificationParser
import com.example.pocketexpense.parser.GooglePayParser
import com.example.pocketexpense.parser.NotificationTestFixtures
import com.example.pocketexpense.parser.ParseResult
import com.example.pocketexpense.parser.PaytmParser
import com.example.pocketexpense.parser.PaymentDirection
import com.example.pocketexpense.parser.PaymentStatus
import com.example.pocketexpense.parser.PhonePeParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationParserTest {

    private val compositeParser = CompositeNotificationParser()
    private val gpayParser = GooglePayParser()
    private val phonePeParser = PhonePeParser()
    private val paytmParser = PaytmParser()

    @Test
    fun googlePay_parsesOutgoingPaymentCorrectly() {
        val result = gpayParser.parse(
            title = "Paid ₹450 to Starbucks Coffee",
            text = "UPI transaction ID: 412356789012. Completed using HDFC Bank ••••1234.",
            timestamp = 1700000000000L
        )

        assertTrue(result is ParseResult.Success)
        val payment = (result as ParseResult.Success).payment
        assertEquals(45000L, payment.amountInPaise)
        assertEquals("Starbucks Coffee", payment.merchantOrRecipient)
        assertEquals(PaymentDirection.OUTGOING, payment.direction)
        assertEquals(PaymentStatus.SUCCESS, payment.status)
        assertEquals("412356789012", payment.transactionReference)
        assertTrue(payment.isExpenseEligible)
    }

    @Test
    fun googlePay_incomingTransfer_isIgnored() {
        val result = gpayParser.parse(
            title = "Received ₹1,500 from Rahul Sharma",
            text = "Deposited into your Bank ••••5678. UPI Ref: 498765432109",
            timestamp = 1700000000000L
        )

        assertTrue(result is ParseResult.Ignored)
        val reason = (result as ParseResult.Ignored).reason
        assertTrue(reason.contains("credit", ignoreCase = true) || reason.contains("Incoming", ignoreCase = true))
    }

    @Test
    fun googlePay_failedPayment_isIgnored() {
        val result = gpayParser.parse(
            title = "Payment of ₹300 to Zomato failed",
            text = "Money was not deducted. Bank servers timed out.",
            timestamp = 1700000000000L
        )

        assertTrue(result is ParseResult.Ignored)
        val reason = (result as ParseResult.Ignored).reason
        assertTrue(reason.contains("failed", ignoreCase = true) || reason.contains("declined", ignoreCase = true))
    }

    @Test
    fun phonePe_parsesOutgoingPaymentCorrectly() {
        val result = phonePeParser.parse(
            title = "Payment to Uber of ₹189 successful",
            text = "Paid ₹189 to Uber using HDFC Bank. Txn ID: T240819123456",
            timestamp = 1700000000000L
        )

        assertTrue(result is ParseResult.Success)
        val payment = (result as ParseResult.Success).payment
        assertEquals(18900L, payment.amountInPaise)
        assertEquals("Uber", payment.merchantOrRecipient)
        assertEquals(PaymentDirection.OUTGOING, payment.direction)
        assertEquals(PaymentStatus.SUCCESS, payment.status)
        assertEquals("T240819123456", payment.transactionReference)
        assertTrue(payment.isExpenseEligible)
    }

    @Test
    fun phonePe_incomingTransfer_isIgnored() {
        val result = phonePeParser.parse(
            title = "Received ₹500 from Priya",
            text = "₹500 received into your State Bank account from Priya Patel.",
            timestamp = 1700000000000L
        )

        assertTrue(result is ParseResult.Ignored)
    }

    @Test
    fun phonePe_refund_isIgnored() {
        val result = phonePeParser.parse(
            title = "Refund of ₹199 received",
            text = "Refund from Swiggy credited to bank account.",
            timestamp = 1700000000000L
        )

        assertTrue(result is ParseResult.Ignored)
    }

    @Test
    fun paytm_parsesOutgoingPaymentCorrectly() {
        val result = paytmParser.parse(
            title = "Paid ₹240 to McDonald's",
            text = "Paid ₹240 to McDonald's successfully from Bank. UPI Ref: 987654321098",
            timestamp = 1700000000000L
        )

        assertTrue(result is ParseResult.Success)
        val payment = (result as ParseResult.Success).payment
        assertEquals(24000L, payment.amountInPaise)
        assertEquals("McDonald's", payment.merchantOrRecipient)
        assertEquals("987654321098", payment.transactionReference)
        assertTrue(payment.isExpenseEligible)
    }

    @Test
    fun paytm_refund_isIgnored() {
        val result = paytmParser.parse(
            title = "Refund Received",
            text = "Refund of ₹350 from Swiggy credited to your bank account.",
            timestamp = 1700000000000L
        )

        assertTrue(result is ParseResult.Ignored)
    }

    @Test
    fun compositeParser_unsupportedPackage_isIgnored() {
        val result = compositeParser.parse(
            packageName = "com.whatsapp",
            title = "New message",
            text = "Hey how are you?"
        )

        assertTrue(result is ParseResult.Ignored)
    }

    @Test
    fun compositeParser_promotionalNotification_isIgnored() {
        val result = compositeParser.parse(
            packageName = "com.google.android.apps.nbu.paisa.user",
            title = "Earn up to ₹100 cashback!",
            text = "Pay your electricity bill now and scratch the card to win rewards."
        )

        assertTrue(result is ParseResult.Ignored)
    }

    @Test
    fun compositeParser_sensitiveOtp_isBlockedImmediately() {
        val result = compositeParser.parse(
            packageName = "com.phonepe.app",
            title = "SBI UPI Verification",
            text = "Your OTP is 739201 for payment of ₹5,000. Do not share with anyone."
        )

        assertTrue(result is ParseResult.Ignored)
        val reason = (result as ParseResult.Ignored).reason
        assertTrue(reason.contains("Sensitive", ignoreCase = true) || reason.contains("OTP", ignoreCase = true))
    }

    @Test
    fun autoCategoryMapper_mapsAccurately() {
        assertEquals(1L, AutoCategoryMapper.suggestCategoryId("Swiggy"))
        assertEquals(1L, AutoCategoryMapper.suggestCategoryId("Starbucks Coffee"))
        assertEquals(2L, AutoCategoryMapper.suggestCategoryId("Uber India"))
        assertEquals(2L, AutoCategoryMapper.suggestCategoryId("Indian Oil Petrol Pump"))
        assertEquals(3L, AutoCategoryMapper.suggestCategoryId("Blinkit Commerce"))
        assertEquals(3L, AutoCategoryMapper.suggestCategoryId("BigBasket"))
        assertEquals(4L, AutoCategoryMapper.suggestCategoryId("Amazon Retail"))
        assertEquals(4L, AutoCategoryMapper.suggestCategoryId("Zara Clothing"))
        assertEquals(5L, AutoCategoryMapper.suggestCategoryId("Airtel Postpaid Bill"))
        assertEquals(6L, AutoCategoryMapper.suggestCategoryId("Coursera Online Course"))
        assertEquals(7L, AutoCategoryMapper.suggestCategoryId("Apollo Pharmacy"))
        assertEquals(8L, AutoCategoryMapper.suggestCategoryId("Unknown Vendor XYZ"))
    }

    @Test
    fun allFixtures_haveConsistentExpectations() {
        val fixtures = NotificationTestFixtures.allFixtures
        assertTrue(fixtures.isNotEmpty())

        for (fixture in fixtures) {
            val result = compositeParser.parse(
                packageName = fixture.packageName,
                title = fixture.title,
                text = fixture.text
            )

            if (fixture.isExpenseExpected) {
                assertTrue("Fixture ${fixture.id} was expected to parse as expense but result was: $result",
                    result is ParseResult.Success && (result as ParseResult.Success).payment.isExpenseEligible)
            } else {
                assertTrue("Fixture ${fixture.id} was expected to be ignored but parsed as: $result",
                    result is ParseResult.Ignored || result is ParseResult.Ambiguous)
            }
        }
    }
}
