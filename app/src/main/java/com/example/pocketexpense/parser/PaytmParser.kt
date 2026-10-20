package com.example.pocketexpense.parser

class PaytmParser : NotificationParser {

    override val supportedPackageName: String = "net.one97.paytm"
    override val supportedPackageNames: Set<String> = setOf("net.one97.paytm", "com.paytm.business")
    override val appDisplayName: String = "Paytm"

    // Pattern 1: "Paid ₹1 to Sharma Stores", "Paid ₹1.00 successfully to Sharma Stores"
    private val paidAmountToRegex = Regex(
        """\bPaid\s+(?:₹|INR|Rs\.?)\s*[\d,]+(?:\.\d{1,2})?\s+(?:successfully\s+)?to\s+([^.\n]+)""",
        RegexOption.IGNORE_CASE
    )

    // Pattern 2: "₹1 paid to Sharma Stores", "₹1.00 paid successfully to Sharma Stores"
    private val amountPaidToRegex = Regex(
        """(?:₹|INR|Rs\.?)\s*[\d,]+(?:\.\d{1,2})?\s+paid\s+(?:successfully\s+)?to\s+([^.\n]+)""",
        RegexOption.IGNORE_CASE
    )

    // Pattern 3: "Payment of ₹1 to Sharma Stores is/was successful"
    private val paymentOfAmountToRegex = Regex(
        """Payment\s+of\s+(?:₹|INR|Rs\.?)\s*[\d,]+(?:\.\d{1,2})?\s+to\s+([^.\n]+?)(?:\s+(?:is|was)?\s*successful|\.|\n|$)""",
        RegexOption.IGNORE_CASE
    )

    // Pattern 4: "Payment to Sharma Stores of ₹1", "Payment to Sharma Stores ₹1"
    private val paymentToMerchantAmountRegex = Regex(
        """Payment\s+to\s+([^.\n]+?)\s+(?:of\s+)?(?:₹|INR|Rs\.?)\s*[\d,]+(?:\.\d{1,2})?""",
        RegexOption.IGNORE_CASE
    )

    // Pattern 5: "Paid to Sharma Stores ₹1"
    private val paidToMerchantAmountRegex = Regex(
        """Paid\s+to\s+([^.\n]+?)\s+(?:₹|INR|Rs\.?)\s*[\d,]+(?:\.\d{1,2})?""",
        RegexOption.IGNORE_CASE
    )

    // Pattern 6: "Money Sent: ₹1 to Rahul" or "Money Sent ₹1 to Rahul"
    private val moneySentAmountToRegex = Regex(
        """Money\s+Sent[:\s]+(?:₹|INR|Rs\.?)\s*[\d,]+(?:\.\d{1,2})?\s+to\s+([^.\n]+)""",
        RegexOption.IGNORE_CASE
    )

    // Pattern 7: "Money sent to Rahul: ₹1"
    private val moneySentToMerchantRegex = Regex(
        """Money\s+sent\s+to\s+([^.\n:]+)[:\s]+(?:₹|INR|Rs\.?)\s*[\d,]+(?:\.\d{1,2})?""",
        RegexOption.IGNORE_CASE
    )

    // Pattern 8: "Sent ₹1 to Rahul"
    private val sentAmountToRegex = Regex(
        """\bSent\s+(?:₹|INR|Rs\.?)\s*[\d,]+(?:\.\d{1,2})?\s+to\s+([^.\n]+)""",
        RegexOption.IGNORE_CASE
    )

    // Pattern 9: "₹1 sent to Rahul"
    private val amountSentToRegex = Regex(
        """(?:₹|INR|Rs\.?)\s*[\d,]+(?:\.\d{1,2})?\s+sent\s+(?:successfully\s+)?to\s+([^.\n]+)""",
        RegexOption.IGNORE_CASE
    )

    // Pattern 10: "You have paid ₹1 to XYZ", "You've paid ₹1 to XYZ"
    private val youHavePaidRegex = Regex(
        """You(?:'ve|\s+have)?\s+paid\s+(?:₹|INR|Rs\.?)\s*[\d,]+(?:\.\d{1,2})?\s+to\s+([^.\n]+)""",
        RegexOption.IGNORE_CASE
    )

    // Pattern 11: "₹1 debited ... for payment to Sharma Stores"
    private val debitedForPaymentRegex = Regex(
        """(?:Debited\s+(?:₹|INR|Rs\.?)\s*[\d,]+(?:\.\d{1,2})?|(?:₹|INR|Rs\.?)\s*[\d,]+(?:\.\d{1,2})?\s+debited).*?\b(?:to|for\s+payment\s+to)\s+([^.\n]+)""",
        RegexOption.IGNORE_CASE
    )

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

        if (BaseParserUtils.isPromotional(combined)) {
            return ParseResult.Ignored("Promotional or offer notification")
        }

        val amountInPaise = BaseParserUtils.extractAmountInPaise(combined)
            ?: return ParseResult.Ambiguous(title, "Could not extract valid amount")

        // Try extracting merchant from title first, then combined text across all patterns
        val rawMerchant = paidAmountToRegex.find(title)?.groupValues?.get(1)
            ?: paidAmountToRegex.find(combined)?.groupValues?.get(1)
            ?: amountPaidToRegex.find(title)?.groupValues?.get(1)
            ?: amountPaidToRegex.find(combined)?.groupValues?.get(1)
            ?: paymentOfAmountToRegex.find(title)?.groupValues?.get(1)
            ?: paymentOfAmountToRegex.find(combined)?.groupValues?.get(1)
            ?: youHavePaidRegex.find(title)?.groupValues?.get(1)
            ?: youHavePaidRegex.find(combined)?.groupValues?.get(1)
            ?: sentAmountToRegex.find(title)?.groupValues?.get(1)
            ?: sentAmountToRegex.find(combined)?.groupValues?.get(1)
            ?: amountSentToRegex.find(title)?.groupValues?.get(1)
            ?: amountSentToRegex.find(combined)?.groupValues?.get(1)
            ?: moneySentAmountToRegex.find(title)?.groupValues?.get(1)
            ?: moneySentAmountToRegex.find(combined)?.groupValues?.get(1)
            ?: moneySentToMerchantRegex.find(title)?.groupValues?.get(1)
            ?: moneySentToMerchantRegex.find(combined)?.groupValues?.get(1)
            ?: paidToMerchantAmountRegex.find(title)?.groupValues?.get(1)
            ?: paidToMerchantAmountRegex.find(combined)?.groupValues?.get(1)
            ?: paymentToMerchantAmountRegex.find(title)?.groupValues?.get(1)
            ?: paymentToMerchantAmountRegex.find(combined)?.groupValues?.get(1)
            ?: debitedForPaymentRegex.find(title)?.groupValues?.get(1)
            ?: debitedForPaymentRegex.find(combined)?.groupValues?.get(1)

        val finalMerchant = if (!rawMerchant.isNullOrBlank()) {
            BaseParserUtils.cleanMerchantName(rawMerchant)
        } else if (title.startsWith("Paid to", ignoreCase = true)) {
            BaseParserUtils.cleanMerchantName(title.removePrefix("Paid to").removePrefix("paid to"))
        } else if (title.startsWith("Payment to", ignoreCase = true)) {
            BaseParserUtils.cleanMerchantName(title.removePrefix("Payment to").removePrefix("payment to"))
        } else if (title.startsWith("Sent to", ignoreCase = true)) {
            BaseParserUtils.cleanMerchantName(title.removePrefix("Sent to").removePrefix("sent to"))
        } else if (BaseParserUtils.hasOutgoingPaymentMarkers(combined)) {
            "Paytm Merchant"
        } else {
            return ParseResult.Ambiguous(title, "No clear payment recipient or confirmation in notification")
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
