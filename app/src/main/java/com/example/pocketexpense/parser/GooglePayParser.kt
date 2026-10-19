package com.example.pocketexpense.parser

class GooglePayParser : NotificationParser {

    override val supportedPackageName: String = "com.google.android.apps.nbu.paisa.user"
    override val appDisplayName: String = "Google Pay"

    // Matches: "Paid ₹450 to Starbucks", "Paid ₹1,200 to Rahul", "Paid to Starbucks ₹450"
    private val paidToRegex1 = Regex("""Paid\s+(?:₹|INR|Rs\.?)\s*[\d,]+(?:\.\d{1,2})?\s+to\s+([^.\n]+)""", RegexOption.IGNORE_CASE)
    private val paidToRegex2 = Regex("""Paid\s+to\s+([^.\n]+?)\s+(?:₹|INR|Rs\.?)\s*[\d,]+(?:\.\d{1,2})?""", RegexOption.IGNORE_CASE)
    private val youPaidRegex = Regex("""You\s+paid\s+(?:₹|INR|Rs\.?)\s*[\d,]+(?:\.\d{1,2})?\s*(?:to\s+([^.\n]+))?""", RegexOption.IGNORE_CASE)

    override fun parse(title: String, text: String, timestamp: Long): ParseResult {
        val combined = "$title\n$text".trim()

        // 1. Guard against OTPs / PINs / passwords
        if (BaseParserUtils.containsSensitiveData(combined)) {
            return ParseResult.Ignored("Sensitive information or OTP detected")
        }

        // 2. Guard against Failed Payments
        if (BaseParserUtils.isFailedPayment(combined)) {
            return ParseResult.Ignored("Payment failed or declined")
        }

        // 3. Guard against Refunds
        if (BaseParserUtils.isRefund(combined)) {
            return ParseResult.Ignored("Refund transaction")
        }

        // 4. Guard against Incoming Transfers (Credits)
        if (BaseParserUtils.isIncomingPayment(combined) || combined.contains("received", ignoreCase = true)) {
            return ParseResult.Ignored("Incoming transfer (credit)")
        }

        // 5. Guard against Promotional / Cashback offers
        if (BaseParserUtils.isPromotional(combined) && !combined.startsWith("Paid", ignoreCase = true) && !title.startsWith("Paid", ignoreCase = true)) {
            return ParseResult.Ignored("Promotional or offer notification")
        }

        // 6. Extract Amount
        val amountInPaise = BaseParserUtils.extractAmountInPaise(combined)
            ?: return ParseResult.Ambiguous(title, "Could not extract valid amount")

        // 7. Extract Merchant / Recipient (prefer title first to avoid appending reference info)
        var merchant: String? = null
        paidToRegex1.find(title)?.let {
            merchant = it.groupValues[1]
        } ?: paidToRegex1.find(combined)?.let {
            merchant = it.groupValues[1]
        } ?: paidToRegex2.find(title)?.let {
            merchant = it.groupValues[1]
        } ?: paidToRegex2.find(combined)?.let {
            merchant = it.groupValues[1]
        } ?: youPaidRegex.find(title)?.let {
            merchant = if (it.groupValues[1].isNotBlank()) it.groupValues[1] else null
        } ?: youPaidRegex.find(combined)?.let {
            merchant = if (it.groupValues[1].isNotBlank()) it.groupValues[1] else null
        }

        val finalMerchant = if (!merchant.isNullOrBlank()) {
            BaseParserUtils.cleanMerchantName(merchant!!)
        } else if (title.startsWith("Paid to", ignoreCase = true)) {
            BaseParserUtils.cleanMerchantName(title.removePrefix("Paid to").removePrefix("paid to"))
        } else {
            "Google Pay Payment"
        }

        // 8. Extract UPI reference if available
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
