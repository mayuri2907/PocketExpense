package com.example.pocketexpense.parser

class CompositeNotificationParser(
    private val parsers: List<NotificationParser> = listOf(
        GooglePayParser(),
        PhonePeParser(),
        PaytmParser(),
        CredParser()
    )
) {

    private val supportedPackages = parsers.map { it.supportedPackageName }.toSet()

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

        val matchingParser = parsers.firstOrNull { it.matchesPackage(packageName) }
            ?: return ParseResult.Ignored("No parser registered for $packageName")

        return matchingParser.parse(safeTitle, safeText, timestamp)
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
