package com.example.pocketexpense.parser

/**
 * Composite parser managing specialized and fallback notification parsers.
 *
 * Implements a multi-layered defense-in-depth architecture:
 * 1. Security & Fraud Gatekeeper: Blocks OTPs, MPINs, failed transactions, refunds, and credits.
 * 2. Primary Specialized Parser: Executes dedicated parser for the incoming package.
 * 3. Multi-Parser Fallback: If primary parser returns ambiguous/ignored (non-sensitive), evaluates
 *    via [GenericUpiParser] and cross-parser candidates to prevent false negatives.
 */
class CompositeNotificationParser(
    private val parsers: List<NotificationParser> = listOf(
        GooglePayParser(),
        PhonePeParser(),
        PaytmParser(),
        CredParser()
    ),
    private val genericParser: GenericUpiParser = GenericUpiParser()
) {

    private val supportedPackages = parsers.flatMap { it.supportedPackageNames }.toSet()

    fun isPackageSupported(packageName: String): Boolean {
        return supportedPackages.contains(packageName.trim())
    }

    fun getSupportedAppNames(): List<String> {
        return parsers.map { it.appDisplayName }
    }

    /**
     * Parses an incoming notification from any supported payment app.
     */
    fun parse(
        packageName: String,
        title: String?,
        text: String?,
        timestamp: Long = System.currentTimeMillis()
    ): ParseResult {
        val safeTitle = title?.trim() ?: ""
        val safeText = text?.trim() ?: ""

        if (!isPackageSupported(packageName)) {
            return ParseResult.Ignored("Unsupported package: $packageName")
        }

        if (safeTitle.isEmpty() && safeText.isEmpty()) {
            return ParseResult.Ignored("Notification content is empty")
        }

        val combined = "$safeTitle. $safeText"

        // Gate 1: Security, fraud prevention, and strict non-expense filtration
        if (BaseParserUtils.containsSensitiveData(combined)) {
            return ParseResult.Ignored("Sensitive verification code / OTP detected")
        }
        if (BaseParserUtils.isFailedPayment(combined)) {
            return ParseResult.Ignored("Transaction failed or declined")
        }
        if (BaseParserUtils.isRefund(combined)) {
            return ParseResult.Ignored("Refund transaction")
        }
        if (BaseParserUtils.isIncomingPayment(combined)) {
            return ParseResult.Ignored("Incoming money transfer (Not an expense)")
        }

        val matchingParser = parsers.firstOrNull { it.matchesPackage(packageName) }
            ?: return ParseResult.Ignored("No parser registered for $packageName")

        // Gate 2: Primary dedicated parser execution
        val primaryResult = matchingParser.parse(safeTitle, safeText, timestamp)
        if (primaryResult is ParseResult.Success) {
            return primaryResult
        }

        // If primary parser rejected for an explicit non-expense reason, respect it
        if (primaryResult is ParseResult.Ignored) {
            val reason = primaryResult.reason
            if (reason.contains("Sensitive", ignoreCase = true) ||
                reason.contains("OTP", ignoreCase = true) ||
                reason.contains("failed", ignoreCase = true) ||
                reason.contains("declined", ignoreCase = true) ||
                reason.contains("Refund", ignoreCase = true) ||
                reason.contains("Incoming", ignoreCase = true)
            ) {
                return primaryResult
            }
        }

        // Gate 3: Universal UPI Fallback (Multi-parser resilience)
        val genericResult = genericParser.parseForPackage(
            packageName = packageName,
            appName = matchingParser.appDisplayName,
            title = safeTitle,
            text = safeText,
            timestamp = timestamp
        )
        if (genericResult is ParseResult.Success) {
            return genericResult
        }

        // Gate 4: Cross-parser candidate evaluation across ensemble
        for (candidateParser in parsers) {
            if (candidateParser != matchingParser) {
                val candidateResult = candidateParser.parse(safeTitle, safeText, timestamp)
                if (candidateResult is ParseResult.Success) {
                    val adaptedPayment = candidateResult.payment.copy(
                        packageName = packageName,
                        appName = matchingParser.appDisplayName
                    )
                    return ParseResult.Success(adaptedPayment)
                }
            }
        }

        return primaryResult
    }

    /**
     * Generates a deterministic deduplication hash for an incoming payment.
     * 1. If a unique transaction reference (UPI ref / UTR / Txn ID) exists, use it.
     * 2. If absent, fallback to a conservative bucket hash based on (package + amount + merchant + 5-minute time bucket).
     */
    fun generateDedupHash(
        packageName: String,
        amountInPaise: Long,
        merchant: String,
        transactionReference: String?,
        timestamp: Long
    ): String {
        return if (!transactionReference.isNullOrBlank()) {
            "REF:${packageName.trim()}:${transactionReference.trim()}"
        } else {
            val fiveMinuteBucket = timestamp / (5 * 60 * 1000)
            val normalizedMerchant = merchant.lowercase().replace(Regex("[^a-z0-9]"), "")
            "FALLBACK:${packageName.trim()}:$amountInPaise:$normalizedMerchant:$fiveMinuteBucket"
        }
    }
}
