package com.example.pocketexpense.util

import android.content.Context
import android.content.Intent
import com.example.pocketexpense.data.model.ExpenseWithCategory

object CsvExportHelper {

    /**
     * Generates an RFC-4180 compliant CSV representation of all recorded expenses.
     */
    fun generateCsv(expenses: List<ExpenseWithCategory>): String {
        val sb = StringBuilder()
        sb.append("ID,Date,Time,Title,Amount (INR),Category,Source,Reference,Notes\n")

        for (item in expenses) {
            val expense = item.expense
            val category = item.category

            val date = DateUtils.formatFullDate(expense.timestamp)
            val time = DateUtils.formatTime(expense.timestamp)
            val amount = "%.2f".format(expense.amountInPaise / 100.0)
            val title = escapeCsv(expense.title)
            val catName = escapeCsv(category.name)
            val source = escapeCsv(expense.source)
            val ref = escapeCsv(expense.transactionReference ?: "")
            val note = escapeCsv(expense.note)

            sb.append("${expense.id},$date,$time,$title,$amount,$catName,$source,$ref,$note\n")
        }
        return sb.toString()
    }

    /**
     * Creates an Intent to share or save the generated CSV content locally.
     * Note: 100% user-initiated, local-only export. Never transmits data over the network.
     */
    fun createShareIntent(csvContent: String): Intent {
        return Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_SUBJECT, "PocketExpense_Export_${System.currentTimeMillis()}.csv")
            putExtra(Intent.EXTRA_TEXT, csvContent)
        }
    }

    fun escapeCsv(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
    }
}
