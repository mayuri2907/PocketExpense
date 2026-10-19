package com.example.pocketexpense

import com.example.pocketexpense.parser.CompositeNotificationParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DuplicateDetectionTest {

    private val parser = CompositeNotificationParser()

    @Test
    fun referenceHash_isDeterministicAndUniqueToReference() {
        val hash1 = parser.generateDedupHash(
            packageName = "com.google.android.apps.nbu.paisa.user",
            amountInPaise = 45000L,
            merchant = "Starbucks",
            transactionReference = "412356789012",
            timestamp = 1700000000000L
        )

        val hash2 = parser.generateDedupHash(
            packageName = "com.google.android.apps.nbu.paisa.user",
            amountInPaise = 45000L,
            merchant = "Starbucks",
            transactionReference = "412356789012",
            timestamp = 1700000010000L // 10 seconds later
        )

        // Must produce identical hash because transaction reference is identical
        assertEquals(hash1, hash2)
        assertEquals("REF:com.google.android.apps.nbu.paisa.user:412356789012", hash1)
    }

    @Test
    fun referenceHash_differsWhenReferenceDiffers() {
        val hash1 = parser.generateDedupHash(
            packageName = "com.google.android.apps.nbu.paisa.user",
            amountInPaise = 45000L,
            merchant = "Starbucks",
            transactionReference = "REF001",
            timestamp = 1700000000000L
        )

        val hash2 = parser.generateDedupHash(
            packageName = "com.google.android.apps.nbu.paisa.user",
            amountInPaise = 45000L,
            merchant = "Starbucks",
            transactionReference = "REF002",
            timestamp = 1700000000000L
        )

        assertNotEquals(hash1, hash2)
    }

    @Test
    fun fallbackHash_matchesWithinSameFiveMinuteBucket() {
        val baseTimestamp = 1700000000000L // arbitrary fixed time
        val oneMinuteLater = baseTimestamp + (60 * 1000L) // +1 min (same 5-min bucket)

        // Ensure both timestamps fall into the same 5-minute bucket
        val bucket1 = baseTimestamp / (5 * 60 * 1000)
        val bucket2 = oneMinuteLater / (5 * 60 * 1000)

        if (bucket1 == bucket2) {
            val hash1 = parser.generateDedupHash(
                packageName = "com.phonepe.app",
                amountInPaise = 18900L,
                merchant = "Uber",
                transactionReference = null,
                timestamp = baseTimestamp
            )

            val hash2 = parser.generateDedupHash(
                packageName = "com.phonepe.app",
                amountInPaise = 18900L,
                merchant = "Uber",
                transactionReference = null,
                timestamp = oneMinuteLater
            )

            assertEquals("Hashes within the same 5-minute bucket must match to prevent duplicate notifications", hash1, hash2)
            assertTrue(hash1.startsWith("FALLBACK:com.phonepe.app:18900:uber:"))
        }
    }

    @Test
    fun fallbackHash_differsWhenAmountOrMerchantDiffers() {
        val timestamp = 1700000000000L

        val hash1 = parser.generateDedupHash(
            packageName = "net.one97.paytm",
            amountInPaise = 24000L,
            merchant = "McDonald's",
            transactionReference = null,
            timestamp = timestamp
        )

        val hashDifferentAmount = parser.generateDedupHash(
            packageName = "net.one97.paytm",
            amountInPaise = 35000L,
            merchant = "McDonald's",
            transactionReference = null,
            timestamp = timestamp
        )

        val hashDifferentMerchant = parser.generateDedupHash(
            packageName = "net.one97.paytm",
            amountInPaise = 24000L,
            merchant = "KFC",
            transactionReference = null,
            timestamp = timestamp
        )

        assertNotEquals(hash1, hashDifferentAmount)
        assertNotEquals(hash1, hashDifferentMerchant)
    }

    @Test
    fun fallbackHash_differsWhenHoursApart() {
        val timestamp1 = 1700000000000L
        val timestampTwoHoursLater = timestamp1 + (2 * 60 * 60 * 1000L) // 2 hours later

        val hash1 = parser.generateDedupHash(
            packageName = "com.phonepe.app",
            amountInPaise = 10000L,
            merchant = "Cafe",
            transactionReference = null,
            timestamp = timestamp1
        )

        val hash2 = parser.generateDedupHash(
            packageName = "com.phonepe.app",
            amountInPaise = 10000L,
            merchant = "Cafe",
            transactionReference = null,
            timestamp = timestampTwoHoursLater
        )

        assertNotEquals(hash1, hash2)
    }
}
