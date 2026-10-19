package com.example.pocketexpense.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateUtils {

    private val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
    private val standardDateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    private val shortDateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())

    fun formatTime(timestamp: Long): String {
        return timeFormat.format(Date(timestamp))
    }

    fun formatDate(timestamp: Long): String {
        val expenseCal = Calendar.getInstance().apply { timeInMillis = timestamp }
        val todayCal = Calendar.getInstance()
        val yesterdayCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }

        return when {
            isSameDay(expenseCal, todayCal) -> "Today"
            isSameDay(expenseCal, yesterdayCal) -> "Yesterday"
            expenseCal.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR) -> shortDateFormat.format(Date(timestamp))
            else -> standardDateFormat.format(Date(timestamp))
        }
    }

    fun formatFullDate(timestamp: Long): String {
        return standardDateFormat.format(Date(timestamp))
    }

    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }
}
