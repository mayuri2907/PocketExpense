package com.example.pocketexpense.parser

import com.example.pocketexpense.util.CurrencyUtils

object BaseParserUtils {

    // Matches ₹450, ₹1,200.50, INR 500, Rs. 100, Rs 50.00
    private val amountRegex = Regex("""(?:₹|INR|Rs\.?)\s*([\d,]+(?:\.\d{1,2})?)""", RegexOption.IGNORE_CASE)

    // Matches UPI Ref No: 123456789012, UPI Txn ID: 412356789012, Txn ID: T12345678, Ref: 887766554433, UTR: 123456
    private val referenceRegex = Regex(
        """\b(?:(?:UPI\s+)?Ref(?:erence)?(?:\s*No\.?)?|(?:UPI\s+)?Txn\s*ID|(?:UPI\s+)?Transaction\s*ID|UTR)[:\s]+([A-Za-z0-9]{6,25})\b""",
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
        "money received", "sent you", "deposited to", "received from", "received ₹", "received inr", "received rs"
    )

    private val promoKeywords = listOf(
        "scratch card", "earn up to", "cashback of up to", "win", "discount", "offer", "coupon", "voucher", "deal of the day"
    )

    private val outgoingMarkersRegex = Regex(
        """\b(?:paid|debited|money\s+sent|sent\s+(?:₹|inr|rs\.?)|payment\s+(?:of|to)|you(?:'ve|\s+have)?\s+paid)\b""",
        RegexOption.IGNORE_CASE
    )

    fun hasOutgoingPaymentMarkers(text: String): Boolean {
        return outgoingMarkersRegex.containsMatchIn(text)
    }

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

        // If it explicitly indicates an outgoing payment (e.g. "Paid ₹1 to Sharma. Cashback will be credited..."),
        // it is an expense, NOT an incoming credit.
        if (hasOutgoingPaymentMarkers(text)) {
            val incomingExplicitRegex = Regex(
                """\b(?:money\s+received\s+from|received\s+(?:₹|inr|rs\.?)\s*[\d,]+|sent\s+you\s+(?:₹|inr|rs\.?))\b""",
                RegexOption.IGNORE_CASE
            )
            return incomingExplicitRegex.containsMatchIn(text)
        }

        if (incomingKeywords.any { lower.contains(it) }) return true
        if (lower.contains("credited") || lower.contains("received") || lower.contains("deposited")) {
            return true
        }
        return false
    }

    fun isPromotional(text: String): Boolean {
        // If there are explicit outgoing payment markers (e.g. ₹1 paid to Sharma),
        // the notification is a real transaction even if it mentions scratch cards or cashback rewards.
        if (hasOutgoingPaymentMarkers(text)) {
            return false
        }
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
        val truncated = firstLine.split(Regex("""(?i)\b(?:using|successfully|via|from bank|from|upi|txn|ref|cashback|scratch|tap to|view|check)\b"""))[0]
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
