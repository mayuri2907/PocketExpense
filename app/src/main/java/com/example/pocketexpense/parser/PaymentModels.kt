package com.example.pocketexpense.parser

enum class PaymentDirection {
    OUTGOING, // Money sent / debited (Valid expense)
    INCOMING, // Money received / credited (NOT an expense)
    UNKNOWN   // Ambiguous or unverified
}

enum class PaymentStatus {
    SUCCESS,
    FAILED,   // Transaction failed / declined (NOT an expense)
    REFUND,   // Refund credited (NOT an expense)
    UNKNOWN   // Ambiguous
}

data class ParsedPayment(
    val packageName: String,
    val appName: String,
    val amountInPaise: Long,
    val merchantOrRecipient: String,
    val direction: PaymentDirection,
    val status: PaymentStatus,
    val transactionReference: String? = null,
    val timestamp: Long = System.currentTimeMillis()
) {
    val isExpenseEligible: Boolean
        get() = direction == PaymentDirection.OUTGOING &&
                status == PaymentStatus.SUCCESS &&
                amountInPaise > 0
}

sealed class ParseResult {
    data class Success(val payment: ParsedPayment) : ParseResult()
    data class Ignored(val reason: String) : ParseResult()
    data class Ambiguous(val rawTitle: String, val reason: String) : ParseResult()
}
