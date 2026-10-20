package com.example.pocketexpense.parser

import com.example.pocketexpense.util.CurrencyUtils

/**
 * Universal UPI Fallback Parser.
 *
 * Ensures PocketExpense does not rely on a single parser implementation.
 * Handles legitimate standard UPI notification variations across all supported payment applications
 * while strictly adhering to privacy and security guards.
 */
class GenericUpiParser : NotificationParser {

    override val supportedPackageName: String = "generic.upi"
    override val supportedPackageNames: Set<String> = setOf(
        "net.one97.paytm",
        "com.paytm.business",
        "com.google.android.apps.nbu.paisa.user",
        "com.phonepe.app",
        "com.dreamplug.androidapp"
    )
    override val appDisplayName: String = "UPI Payment"

    // 1. Paid ₹100 to Merchant
    private val paidAmountToRegex = Regex(
        """\b(?:Paid|Sent)\s+(?:₹|INR|Rs\.?)\s*([\d,]+(?:\.\d{1,2})?)\s+to\s+([^\n.]+?)(?:\s+using|\s+via|\s+from|\s+successfully|\.|$)""",
        RegexOption.IGNORE_CASE
    )

    // 2. ₹100 paid to Merchant
    private val amountPaidToRegex = Regex(
        """(?:₹|INR|Rs\.?)\s*([\d,]+(?:\.\d{1,2})?)\s+(?:paid|sent)\s+to\s+([^\n.]+?)(?:\s+using|\s+via|\s+from|\s+successfully|\.|$)""",
        RegexOption.IGNORE_CASE
    )

    // 3. Payment of ₹100 to Merchant is successful
    private val paymentOfAmountToSuccessfulRegex = Regex(
        """Payment\s+(?:of\s+)?(?:₹|INR|Rs\.?)\s*([\d,]+(?:\.\d{1,2})?)\s+to\s+([^\n.]+?)\s+(?:is|was)?\s*successful""",
        RegexOption.IGNORE_CASE
    )

    // 4. Payment to Merchant of ₹100 successful
    private val paymentToMerchantOfAmountRegex = Regex(
        """Payment\s+to\s+([^\n.]+?)\s+of\s+(?:₹|INR|Rs\.?)\s*([\d,]+(?:\.\d{1,2})?)\s+(?:is|was)?\s*successful""",
        RegexOption.IGNORE_CASE
    )

    // 5. Money Sent: ₹100 to Merchant
    private val moneySentToRegex = Regex(
        """Money\s+Sent(?::\s*|\s+to\s+)(?:₹|INR|Rs\.?)\s*([\d,]+(?:\.\d{1,2})?)\s+to\s+([^\n.]+?)(?:\s+using|\s+via|\s+from|\s+successfully|\.|$)""",
        RegexOption.IGNORE_CASE
    )

    // 6. Money sent to Merchant: ₹100
    private val moneySentToMerchantColonAmountRegex = Regex(
        """Money\s+sent\s+to\s+([^\n.:]+?):\s*(?:₹|INR|Rs\.?)\s*([\d,]+(?:\.\d{1,2})?)""",
        RegexOption.IGNORE_CASE
    )

    // 7. Debited ₹100 for payment to Merchant
    private val debitedForPaymentRegex = Regex(
        """Debited\s+(?:₹|INR|Rs\.?)\s*([\d,]+(?:\.\d{1,2})?)\s+for\s+payment\s+to\s+([^\n.]+?)(?:\s+using|\s+via|\s+from|\s+successfully|\.|$)""",
        RegexOption.IGNORE_CASE
    )

    // 8. You have paid / You've paid ₹100 to Merchant
    private val youHavePaidRegex = Regex(
        """You(?:'ve|\s+have)?\s+paid\s+(?:₹|INR|Rs\.?)\s*([\d,]+(?:\.\d{1,2})?)\s+to\s+([^\n.]+?)(?:\s+using|\s+via|\s+from|\s+successfully|\.|$)""",
        RegexOption.IGNORE_CASE
    )

    // 9. Transferred ₹100 to Merchant
    private val transferredRegex = Regex(
        """Transferred\s+(?:₹|INR|Rs\.?)\s*([\d,]+(?:\.\d{1,2})?)\s+to\s+([^\n.]+?)(?:\s+using|\s+via|\s+from|\s+successfully|\.|$)""",
        RegexOption.IGNORE_CASE
    )

    override fun parse(title: String, text: String, timestamp: Long): ParseResult {
        return parseForPackage("generic.upi", appDisplayName, title, text, timestamp)
    }

    /**
     * Parses notification with explicit package name and display name attribution.
     */
    fun parseForPackage(
        packageName: String,
        appName: String,
        title: String,
        text: String,
        timestamp: Long
    ): ParseResult {
        val combined = "$title. $text"

        // 1. Mandatory Security & Fraud Guard
        if (BaseParserUtils.containsSensitiveData(combined)) {
            return ParseResult.Ignored("Sensitive verification code / OTP detected")
        }

        // 2. Transaction Status Guards
        if (BaseParserUtils.isFailedPayment(combined)) {
            return ParseResult.Ignored("Transaction failed or declined")
        }

        if (BaseParserUtils.isRefund(combined)) {
            return ParseResult.Ignored("Refund transaction")
        }

        if (BaseParserUtils.isIncomingPayment(combined)) {
            return ParseResult.Ignored("Incoming money transfer (Not an expense)")
        }

        if (BaseParserUtils.isPromotional(combined)) {
            return ParseResult.Ignored("Promotional marketing notification")
        }

        val ref = BaseParserUtils.extractReference(combined)

        // Try Pattern 1: Paid ₹... to <Merchant>
        paidAmountToRegex.find(combined)?.let { match ->
            val amountPaise = CurrencyUtils.rupeesStringToPaise(match.groupValues[1])
            val rawMerchant = match.groupValues[2]
            val merchant = BaseParserUtils.cleanMerchantName(rawMerchant).ifBlank { "$appName Merchant" }
            if (amountPaise > 0) {
                return ParseResult.Success(
                    ParsedPayment(
                        packageName = packageName,
                        appName = appName,
                        amountInPaise = amountPaise,
                        merchantOrRecipient = merchant,
                        direction = PaymentDirection.OUTGOING,
                        status = PaymentStatus.SUCCESS,
                        transactionReference = ref,
                        timestamp = timestamp
                    )
                )
            }
        }

        // Try Pattern 2: ₹... paid to <Merchant>
        amountPaidToRegex.find(combined)?.let { match ->
            val amountPaise = CurrencyUtils.rupeesStringToPaise(match.groupValues[1])
            val rawMerchant = match.groupValues[2]
            val merchant = BaseParserUtils.cleanMerchantName(rawMerchant).ifBlank { "$appName Merchant" }
            if (amountPaise > 0) {
                return ParseResult.Success(
                    ParsedPayment(
                        packageName = packageName,
                        appName = appName,
                        amountInPaise = amountPaise,
                        merchantOrRecipient = merchant,
                        direction = PaymentDirection.OUTGOING,
                        status = PaymentStatus.SUCCESS,
                        transactionReference = ref,
                        timestamp = timestamp
                    )
                )
            }
        }

        // Try Pattern 3: Payment of ₹... to <Merchant> is successful
        paymentOfAmountToSuccessfulRegex.find(combined)?.let { match ->
            val amountPaise = CurrencyUtils.rupeesStringToPaise(match.groupValues[1])
            val rawMerchant = match.groupValues[2]
            val merchant = BaseParserUtils.cleanMerchantName(rawMerchant).ifBlank { "$appName Merchant" }
            if (amountPaise > 0) {
                return ParseResult.Success(
                    ParsedPayment(
                        packageName = packageName,
                        appName = appName,
                        amountInPaise = amountPaise,
                        merchantOrRecipient = merchant,
                        direction = PaymentDirection.OUTGOING,
                        status = PaymentStatus.SUCCESS,
                        transactionReference = ref,
                        timestamp = timestamp
                    )
                )
            }
        }

        // Try Pattern 4: Payment to <Merchant> of ₹... successful
        paymentToMerchantOfAmountRegex.find(combined)?.let { match ->
            val rawMerchant = match.groupValues[1]
            val amountPaise = CurrencyUtils.rupeesStringToPaise(match.groupValues[2])
            val merchant = BaseParserUtils.cleanMerchantName(rawMerchant).ifBlank { "$appName Merchant" }
            if (amountPaise > 0) {
                return ParseResult.Success(
                    ParsedPayment(
                        packageName = packageName,
                        appName = appName,
                        amountInPaise = amountPaise,
                        merchantOrRecipient = merchant,
                        direction = PaymentDirection.OUTGOING,
                        status = PaymentStatus.SUCCESS,
                        transactionReference = ref,
                        timestamp = timestamp
                    )
                )
            }
        }

        // Try Pattern 5: Money Sent: ₹... to <Merchant>
        moneySentToRegex.find(combined)?.let { match ->
            val amountPaise = CurrencyUtils.rupeesStringToPaise(match.groupValues[1])
            val rawMerchant = match.groupValues[2]
            val merchant = BaseParserUtils.cleanMerchantName(rawMerchant).ifBlank { "$appName Recipient" }
            if (amountPaise > 0) {
                return ParseResult.Success(
                    ParsedPayment(
                        packageName = packageName,
                        appName = appName,
                        amountInPaise = amountPaise,
                        merchantOrRecipient = merchant,
                        direction = PaymentDirection.OUTGOING,
                        status = PaymentStatus.SUCCESS,
                        transactionReference = ref,
                        timestamp = timestamp
                    )
                )
            }
        }

        // Try Pattern 6: Money sent to <Merchant>: ₹...
        moneySentToMerchantColonAmountRegex.find(combined)?.let { match ->
            val rawMerchant = match.groupValues[1]
            val amountPaise = CurrencyUtils.rupeesStringToPaise(match.groupValues[2])
            val merchant = BaseParserUtils.cleanMerchantName(rawMerchant).ifBlank { "$appName Recipient" }
            if (amountPaise > 0) {
                return ParseResult.Success(
                    ParsedPayment(
                        packageName = packageName,
                        appName = appName,
                        amountInPaise = amountPaise,
                        merchantOrRecipient = merchant,
                        direction = PaymentDirection.OUTGOING,
                        status = PaymentStatus.SUCCESS,
                        transactionReference = ref,
                        timestamp = timestamp
                    )
                )
            }
        }

        // Try Pattern 7: Debited ₹... for payment to <Merchant>
        debitedForPaymentRegex.find(combined)?.let { match ->
            val amountPaise = CurrencyUtils.rupeesStringToPaise(match.groupValues[1])
            val rawMerchant = match.groupValues[2]
            val merchant = BaseParserUtils.cleanMerchantName(rawMerchant).ifBlank { "$appName Merchant" }
            if (amountPaise > 0) {
                return ParseResult.Success(
                    ParsedPayment(
                        packageName = packageName,
                        appName = appName,
                        amountInPaise = amountPaise,
                        merchantOrRecipient = merchant,
                        direction = PaymentDirection.OUTGOING,
                        status = PaymentStatus.SUCCESS,
                        transactionReference = ref,
                        timestamp = timestamp
                    )
                )
            }
        }

        // Try Pattern 8: You have paid ₹... to <Merchant>
        youHavePaidRegex.find(combined)?.let { match ->
            val amountPaise = CurrencyUtils.rupeesStringToPaise(match.groupValues[1])
            val rawMerchant = match.groupValues[2]
            val merchant = BaseParserUtils.cleanMerchantName(rawMerchant).ifBlank { "$appName Merchant" }
            if (amountPaise > 0) {
                return ParseResult.Success(
                    ParsedPayment(
                        packageName = packageName,
                        appName = appName,
                        amountInPaise = amountPaise,
                        merchantOrRecipient = merchant,
                        direction = PaymentDirection.OUTGOING,
                        status = PaymentStatus.SUCCESS,
                        transactionReference = ref,
                        timestamp = timestamp
                    )
                )
            }
        }

        // Try Pattern 9: Transferred ₹... to <Merchant>
        transferredRegex.find(combined)?.let { match ->
            val amountPaise = CurrencyUtils.rupeesStringToPaise(match.groupValues[1])
            val rawMerchant = match.groupValues[2]
            val merchant = BaseParserUtils.cleanMerchantName(rawMerchant).ifBlank { "$appName Recipient" }
            if (amountPaise > 0) {
                return ParseResult.Success(
                    ParsedPayment(
                        packageName = packageName,
                        appName = appName,
                        amountInPaise = amountPaise,
                        merchantOrRecipient = merchant,
                        direction = PaymentDirection.OUTGOING,
                        status = PaymentStatus.SUCCESS,
                        transactionReference = ref,
                        timestamp = timestamp
                    )
                )
            }
        }

        // Fallback: If strong outgoing markers exist and amount is identified with high confidence
        if (BaseParserUtils.hasOutgoingPaymentMarkers(combined)) {
            val amountPaise = BaseParserUtils.extractAmountInPaise(combined)
            if (amountPaise != null && amountPaise > 0) {
                // Try extracting merchant from title if title is "Paid to <X>" or "Payment to <X>"
                val titleLower = title.lowercase().trim()
                val merchantCandidate = when {
                    titleLower.startsWith("paid to ") -> title.substring(8).trim()
                    titleLower.startsWith("payment to ") -> title.substring(11).trim()
                    titleLower.startsWith("sent to ") -> title.substring(8).trim()
                    else -> null
                }
                val cleanedMerchant = merchantCandidate?.let { BaseParserUtils.cleanMerchantName(it) }
                    ?.ifBlank { null } ?: "$appName Merchant"

                return ParseResult.Success(
                    ParsedPayment(
                        packageName = packageName,
                        appName = appName,
                        amountInPaise = amountPaise,
                        merchantOrRecipient = cleanedMerchant,
                        direction = PaymentDirection.OUTGOING,
                        status = PaymentStatus.SUCCESS,
                        transactionReference = ref,
                        timestamp = timestamp
                    )
                )
            }
        }

        return ParseResult.Ambiguous(title, "Could not extract payment details from $appName notification")
    }
}
