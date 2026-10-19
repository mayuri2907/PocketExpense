package com.example.pocketexpense.parser

class PhonePeParser : NotificationParser {

    override val supportedPackageName: String = "com.phonepe.app"
    override val appDisplayName: String = "PhonePe"

    // Matches: "Payment to Uber of ₹189 successful", "Payment to Swiggy of ₹350"
    private val paymentToOfRegex = Regex("""Payment\s+to\s+([^.\n]+?)\s+of\s+(?:₹|INR|Rs\.?)\s*[\d,]+(?:\.\d{1,2})?""", RegexOption.IGNORE_CASE)
    // Matches: "Paid to Swiggy ₹349", "Paid to Uber ₹189"
    private val paidToRegex = Regex("""Paid\s+to\s+([^.\n]+?)\s+(?:₹|INR|Rs\.?)\s*[\d,]+(?:\.\d{1,2})?""", RegexOption.IGNORE_CASE)
    // Matches: "Paid ₹189 to Uber"
    private val paidAmountToRegex = Regex("""Paid\s+(?:₹|INR|Rs\.?)\s*[\d,]+(?:\.\d{1,2})?\s+to\s+([^.\n]+)""", RegexOption.IGNORE_CASE)

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

        if (BaseParserUtils.isPromotional(combined) && !combined.contains("successful", ignoreCase = true) && !combined.contains("Paid", ignoreCase = true)) {
            return ParseResult.Ignored("Promotional or offer notification")
        }

        val amountInPaise = BaseParserUtils.extractAmountInPaise(combined)
            ?: return ParseResult.Ambiguous(title, "Could not extract valid amount")

        var merchant: String? = null
        paymentToOfRegex.find(title)?.let {
            merchant = it.groupValues[1]
        } ?: paymentToOfRegex.find(combined)?.let {
            merchant = it.groupValues[1]
        } ?: paidToRegex.find(title)?.let {
            merchant = it.groupValues[1]
        } ?: paidToRegex.find(combined)?.let {
            merchant = it.groupValues[1]
        } ?: paidAmountToRegex.find(title)?.let {
            merchant = it.groupValues[1]
        } ?: paidAmountToRegex.find(combined)?.let {
            merchant = it.groupValues[1]
        }

        val finalMerchant = if (!merchant.isNullOrBlank()) {
            BaseParserUtils.cleanMerchantName(merchant!!)
        } else if (title.contains("Payment to", ignoreCase = true)) {
            val after = title.substringAfter("Payment to", "").substringBefore("of").trim()
            if (after.isNotBlank()) BaseParserUtils.cleanMerchantName(after) else "PhonePe Merchant"
        } else {
            "PhonePe Payment"
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
