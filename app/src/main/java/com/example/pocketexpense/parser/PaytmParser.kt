package com.example.pocketexpense.parser

class PaytmParser : NotificationParser {

    override val supportedPackageName: String = "net.one97.paytm"
    override val appDisplayName: String = "Paytm"

    // Matches: "Paid ₹240 to McDonald's", "Paid ₹150 to Chai Point"
    private val paidAmountToRegex = Regex("""Paid\s+(?:₹|INR|Rs\.?)\s*[\d,]+(?:\.\d{1,2})?\s+to\s+([^.\n]+)""", RegexOption.IGNORE_CASE)
    // Matches: "Money Sent: ₹500 to Rahul"
    private val moneySentRegex = Regex("""Money\s+Sent[:\s]+(?:₹|INR|Rs\.?)\s*[\d,]+(?:\.\d{1,2})?\s+to\s+([^.\n]+)""", RegexOption.IGNORE_CASE)
    // Matches: "Paid to McDonald's ₹240"
    private val paidToRegex = Regex("""Paid\s+to\s+([^.\n]+?)\s+(?:₹|INR|Rs\.?)\s*[\d,]+(?:\.\d{1,2})?""", RegexOption.IGNORE_CASE)

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

        if (BaseParserUtils.isPromotional(combined) && !combined.startsWith("Paid", ignoreCase = true) && !title.startsWith("Paid", ignoreCase = true)) {
            return ParseResult.Ignored("Promotional or offer notification")
        }

        val amountInPaise = BaseParserUtils.extractAmountInPaise(combined)
            ?: return ParseResult.Ambiguous(title, "Could not extract valid amount")

        var merchant: String? = null
        paidAmountToRegex.find(title)?.let {
            merchant = it.groupValues[1]
        } ?: paidAmountToRegex.find(combined)?.let {
            merchant = it.groupValues[1]
        } ?: moneySentRegex.find(title)?.let {
            merchant = it.groupValues[1]
        } ?: moneySentRegex.find(combined)?.let {
            merchant = it.groupValues[1]
        } ?: paidToRegex.find(title)?.let {
            merchant = it.groupValues[1]
        } ?: paidToRegex.find(combined)?.let {
            merchant = it.groupValues[1]
        }

        val finalMerchant = if (!merchant.isNullOrBlank()) {
            BaseParserUtils.cleanMerchantName(merchant!!)
        } else if (title.startsWith("Paid to", ignoreCase = true)) {
            BaseParserUtils.cleanMerchantName(title.removePrefix("Paid to").removePrefix("paid to"))
        } else {
            "Paytm Merchant"
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
