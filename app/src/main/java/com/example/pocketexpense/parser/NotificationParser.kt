package com.example.pocketexpense.parser

interface NotificationParser {
    val supportedPackageName: String
    val supportedPackageNames: Set<String> get() = setOf(supportedPackageName)
    val appDisplayName: String

    /**
     * Quick check if the notification comes from this app's package.
     */
    fun matchesPackage(packageName: String): Boolean =
        supportedPackageNames.any { it.equals(packageName.trim(), ignoreCase = true) }

    /**
     * Parses the incoming notification title and text.
     * Returns [ParseResult.Success] if it's a verified payment event,
     * [ParseResult.Ignored] if it is an incoming transfer, failed payment, refund, OTP, or promo,
     * or [ParseResult.Ambiguous] if it cannot be safely categorized.
     */
    fun parse(title: String, text: String, timestamp: Long): ParseResult
}
