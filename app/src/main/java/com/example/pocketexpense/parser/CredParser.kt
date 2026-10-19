package com.example.pocketexpense.parser

class CredParser : NotificationParser {

    override val supportedPackageName: String = "com.dreamplug.androidapp"
    override val appDisplayName: String = "CRED"

    private val paidRegex = Regex("""(?:Paid\s+(?:₹|INR|Rs\.?)\s*[\d,]+(?:\.\d{1,2})?\s+to\s+([^.\n]+)|(?:₹|INR|Rs\.?)\s*[\d,]+(?:\.\d{1,2})?\s+paid\s+to\s+([^.\n]+))""", RegexOption.IGNORE_CASE)

    override fun parse(title: String, text: String, timestamp: Long): ParseResult {
        val combined = "$title\n$text".trim()

        if (BaseParserUtils.containsSensitiveData(combined)) {
            return ParseResult.Ignored("Sensitive information or OTP detected")
        }

        if (BaseParserUtils.isFailedPayment(combined)) {
            return ParseResult.Ignored("Payment failed or declined")
        }

        if (BaseParserUtils.isRefund(combined)) {
            return ParseResult.Ignored("Refund transaction")
        }

        if (BaseParserUtils.isIncomingPayment(combined)) {
            return ParseResult.Ignored("Incoming transfer (credit)")
        }

        if (BaseParserUtils.isPromotional(combined) && !combined.contains("paid", ignoreCase = true)) {
            return ParseResult.Ignored("Promotional or offer notification")
        }

        val amountInPaise = BaseParserUtils.extractAmountInPaise(combined)
            ?: return ParseResult.Ambiguous(title, "Could not extract valid amount")

        var merchant: String? = null
        paidRegex.find(title)?.let { match ->
            merchant = if (match.groupValues[1].isNotBlank()) match.groupValues[1] else match.groupValues[2]
        } ?: paidRegex.find(combined)?.let { match ->
            merchant = if (match.groupValues[1].isNotBlank()) match.groupValues[1] else match.groupValues[2]
        }

        val finalMerchant = if (!merchant.isNullOrBlank()) {
            BaseParserUtils.cleanMerchantName(merchant!!)
        } else {
            "CRED Payment"
        }

        val transactionRef = BaseParserUtils.extractReference(combined)

        return ParseResult.Success(
            ParsedPayment(
                packageName = supportedPackageName,
                appName = appDisplayName,
                amountInPaise = amountInPaise,
                merchantOrRecipient = finalMerchant,
                direction = PaymentDirection.OUTGOING,
                status = PaymentStatus.SUCCESS,
                transactionReference = transactionRef,
                timestamp = timestamp
            )
        )
    }
}
