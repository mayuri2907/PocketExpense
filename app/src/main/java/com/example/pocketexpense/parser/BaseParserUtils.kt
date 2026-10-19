package com.example.pocketexpense.parser

import com.example.pocketexpense.util.CurrencyUtils

object BaseParserUtils {

    // Matches ₹450, ₹1,200.50, INR 500, Rs. 100, Rs 50.00
    private val amountRegex = Regex("""(?:₹|INR|Rs\.?)\s*([\d,]+(?:\.\d{1,2})?)""", RegexOption.IGNORE_CASE)

    // Matches UPI Ref No: 123456789012, UPI Txn ID: 412356789012, Txn ID: T12345678, UTR: 123456
    private val referenceRegex = Regex(
        """\b(?:UPI\s*(?:Ref(?:erence)?(?:\s*No)?|Txn\s*ID|Transaction\s*ID)|Txn\s*ID|UTR)[:\s]+([A-Za-z0-9]{6,25})\b""",
        RegexOption.IGNORE_CASE
    )

    private val sensitiveKeywords = listOf(
        "otp", "one time password", "verification code", "secret code",
        "upi pin", "mpin", "atm pin", "cvv", "do not share"
    )

    private val failedKeywords = listOf(
        "failed", "declined", "unsuccessful", "timed out", "could not", "cancelled", "rejected"
    )

    private val refundKeywords = listOf(
        "refund", "refunded", "reversal", "reversed"
    )

    private val incomingKeywords = listOf(
        "received", "credited", "sent you", "deposited", "money received"
    )

    private val promoKeywords = listOf(
        "scratch card", "earn up to", "cashback of up to", "win", "discount", "offer", "coupon", "voucher", "deal of the day"
    )

    fun containsSensitiveData(text: String): Boolean {
        val lower = text.lowercase()
        return sensitiveKeywords.any { lower.contains(it) }
    }

    fun isFailedPayment(text: String): Boolean {
        val lower = text.lowercase()
        return failedKeywords.any { lower.contains(it) }
    }

    fun isRefund(text: String): Boolean {
        val lower = text.lowercase()
        return refundKeywords.any { lower.contains(it) }
    }

    fun isIncomingPayment(text: String): Boolean {
        val lower = text.lowercase()
        return incomingKeywords.any { lower.contains(it) }
    }

    fun isPromotional(text: String): Boolean {
        val lower = text.lowercase()
        return promoKeywords.any { lower.contains(it) }
    }

    fun extractAmountInPaise(text: String): Long? {
        val match = amountRegex.find(text) ?: return null
        val amountStr = match.groupValues[1]
        return try {
            CurrencyUtils.rupeesStringToPaise(amountStr)
        } catch (e: Exception) {
            null
        }
    }

    fun extractReference(text: String): String? {
        val match = referenceRegex.find(text) ?: return null
        return match.groupValues[1].trim()
    }

    fun cleanMerchantName(raw: String): String {
        val firstLine = raw.lines().firstOrNull()?.trim() ?: raw.trim()
        val truncated = firstLine.split(Regex("""(?i)\b(?:using|successfully|via|from bank|from|upi|txn|ref)\b"""))[0]
        return truncated.trim()
            .removePrefix("to ")
            .removePrefix("To ")
            .removePrefix("at ")
            .removePrefix("At ")
            .removePrefix("for ")
            .removePrefix("For ")
            .trimEnd('.', ',', ' ', '-', ':')
            .trim()
    }
}
