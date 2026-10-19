package com.example.pocketexpense.parser

data class NotificationFixture(
    val id: String,
    val name: String,
    val packageName: String,
    val title: String,
    val text: String,
    val expectedOutcome: String,
    val isExpenseExpected: Boolean
)

object NotificationTestFixtures {

    val allFixtures: List<NotificationFixture> = listOf(
        NotificationFixture(
            id = "gpay_outgoing",
            name = "Google Pay — Outgoing Payment",
            packageName = "com.google.android.apps.nbu.paisa.user",
            title = "Paid ₹450 to Starbucks Coffee",
            text = "UPI transaction ID: 412356789012. Completed using HDFC Bank ••••1234.",
            expectedOutcome = "Outgoing expense: ₹450.00 to Starbucks Coffee (Category: Food)",
            isExpenseExpected = true
        ),
        NotificationFixture(
            id = "gpay_incoming",
            name = "Google Pay — Incoming Transfer (Credit)",
            packageName = "com.google.android.apps.nbu.paisa.user",
            title = "Received ₹1,500 from Rahul Sharma",
            text = "Deposited into your Bank ••••5678. UPI Ref: 498765432109",
            expectedOutcome = "Ignored: Incoming money transfer (Not an expense)",
            isExpenseExpected = false
        ),
        NotificationFixture(
            id = "gpay_failed",
            name = "Google Pay — Failed Payment",
            packageName = "com.google.android.apps.nbu.paisa.user",
            title = "Payment of ₹300 to Zomato failed",
            text = "Money was not deducted. Bank servers timed out.",
            expectedOutcome = "Ignored: Transaction failed or declined",
            isExpenseExpected = false
        ),
        NotificationFixture(
            id = "phonepe_outgoing",
            name = "PhonePe — Outgoing Payment",
            packageName = "com.phonepe.app",
            title = "Payment to Uber of ₹189 successful",
            text = "Paid ₹189 to Uber using HDFC Bank. Txn ID: T240819123456",
            expectedOutcome = "Outgoing expense: ₹189.00 to Uber (Category: Travel)",
            isExpenseExpected = true
        ),
        NotificationFixture(
            id = "phonepe_incoming",
            name = "PhonePe — Incoming Transfer (Credit)",
            packageName = "com.phonepe.app",
            title = "Received ₹500 from Priya",
            text = "₹500 received into your State Bank account from Priya Patel.",
            expectedOutcome = "Ignored: Incoming money transfer (Not an expense)",
            isExpenseExpected = false
        ),
        NotificationFixture(
            id = "paytm_outgoing",
            name = "Paytm — Outgoing Payment",
            packageName = "net.one97.paytm",
            title = "Paid ₹240 to McDonald's",
            text = "Paid ₹240 to McDonald's successfully from Bank. UPI Ref: 987654321098",
            expectedOutcome = "Outgoing expense: ₹240.00 to McDonald's (Category: Food)",
            isExpenseExpected = true
        ),
        NotificationFixture(
            id = "paytm_refund",
            name = "Paytm — Refund Received",
            packageName = "net.one97.paytm",
            title = "Refund Received",
            text = "Refund of ₹350 from Swiggy credited to your bank account.",
            expectedOutcome = "Ignored: Refund transaction",
            isExpenseExpected = false
        ),
        NotificationFixture(
            id = "promo_cashback",
            name = "Promotional / Cashback Offer",
            packageName = "com.google.android.apps.nbu.paisa.user",
            title = "Earn up to ₹100 cashback!",
            text = "Pay your electricity bill now and scratch the card to win rewards.",
            expectedOutcome = "Ignored: Promotional marketing notification",
            isExpenseExpected = false
        ),
        NotificationFixture(
            id = "sensitive_otp",
            name = "Sensitive OTP / PIN Notification (Security Guard)",
            packageName = "com.phonepe.app",
            title = "SBI UPI Verification",
            text = "Your OTP is 739201 for payment of ₹5,000. Do not share with anyone.",
            expectedOutcome = "Ignored: Sensitive verification code / OTP detected",
            isExpenseExpected = false
        )
    )
}
