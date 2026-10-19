package com.example.pocketexpense.parser

object AutoCategoryMapper {

    private val categoryRules = mapOf(
        // Category 1: Food
        1L to listOf(
            "swiggy", "zomato", "starbucks", "mcdonald", "kfc", "burger", "cafe",
            "restaurant", "pizza", "chai", "tea", "dining", "domino", "subway",
            "haldiram", "barista", "bakery", "sweet", "bistro", "eats", "coffee", "dhaba"
        ),

        // Category 2: Travel
        2L to listOf(
            "uber", "ola", "rapido", "metro", "irctc", "rail", "train", "flight",
            "indigo", "air india", "fuel", "petrol", "indian oil", "bharat petroleum",
            "hpcl", "shell", "parking", "fastag", "toll", "cab", "auto", "airline", "bus"
        ),

        // Category 3: Groceries
        3L to listOf(
            "blinkit", "zepto", "instamart", "bigbasket", "supermarket", "dmart",
            "provision", "vegetable", "fruit", "milk", "dairy", "grocery", "groceries",
            "more retail", "nature basket", "spencer", "reliance fresh"
        ),

        // Category 4: Shopping
        4L to listOf(
            "amazon", "flipkart", "myntra", "zara", "h&m", "ajio", "nykaa", "retail",
            "store", "shop", "mall", "decathlon", "uniqlo", "westside", "croma",
            "reliance digital", "tata cliq", "footwear", "clothing", "fashion"
        ),

        // Category 5: Bills
        5L to listOf(
            "bescom", "electricity", "airtel", "jio", "vi", "vodafone", "gas",
            "water", "broadband", "wifi", "rent", "maintenance", "tata power",
            "dth", "recharge", "bill", "utility", "broadband", "postpaid", "dish tv"
        ),

        // Category 6: Education
        6L to listOf(
            "college", "school", "university", "coursera", "udemy", "books",
            "tuition", "fee", "exam", "library", "course", "coaching", "academy"
        ),

        // Category 7: Health
        7L to listOf(
            "pharmacy", "apollo", "medplus", "hospital", "clinic", "doctor",
            "1mg", "pharmeasy", "lab", "diagnostics", "medical", "dentist", "gym",
            "fitness", "healthcare", "chemist", "opticals"
        )
    )

    fun hasConfidentMatch(merchantName: String): Boolean {
        val lowerMerchant = merchantName.lowercase()
        for ((_, keywords) in categoryRules) {
            for (keyword in keywords) {
                if (lowerMerchant.contains(keyword)) {
                    return true
                }
            }
        }
        return false
    }

    /**
     * Determines the most appropriate default category ID based on the merchant name.
     * Falls back to Category 8 (Uncategorized) if no keywords match.
     */
    fun suggestCategoryId(merchantName: String): Long {
        val lowerMerchant = merchantName.lowercase()

        for ((categoryId, keywords) in categoryRules) {
            for (keyword in keywords) {
                if (lowerMerchant.contains(keyword)) {
                    return categoryId
                }
            }
        }

        return 8L // Uncategorized
    }
}
