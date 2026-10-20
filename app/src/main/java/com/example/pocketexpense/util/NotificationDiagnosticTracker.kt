package com.example.pocketexpense.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DiagnosticEvent(
    val id: Long,
    val timeFormatted: String,
    val packageName: String,
    val appDisplayName: String,
    val status: DiagnosticStatus,
    val summary: String
)

enum class DiagnosticStatus {
    PARSED_SUCCESS,
    DUPLICATE_IGNORED,
    NON_EXPENSE_IGNORED,
    AMBIGUOUS_UNPARSED,
    SERVICE_DISCONNECTED,
    DATABASE_ERROR
}

/**
 * Thread-safe, in-memory diagnostic tracker for recent notification detection events.
 *
 * Privacy Guarantees:
 * - Only stores sanitized metadata (app name, timestamp, and safe outcome summary).
 * - NEVER stores or persists raw notification text, passwords, OTPs, or authentication credentials.
 * - Stored exclusively in volatile memory (bounded to last 10 events).
 */
object NotificationDiagnosticTracker {

    private val _events = MutableStateFlow<List<DiagnosticEvent>>(emptyList())
    val events: StateFlow<List<DiagnosticEvent>> = _events.asStateFlow()

    private val timeFormat = SimpleDateFormat("hh:mm:ss a", Locale.getDefault())
    private var nextId = 1L

    @Synchronized
    fun logEvent(
        packageName: String,
        appDisplayName: String,
        status: DiagnosticStatus,
        summary: String
    ) {
        val event = DiagnosticEvent(
            id = nextId++,
            timeFormatted = timeFormat.format(Date()),
            packageName = packageName,
            appDisplayName = appDisplayName,
            status = status,
            summary = summary
        )
        val current = _events.value.toMutableList()
        current.add(0, event)
        _events.value = current.take(10)
    }

    @Synchronized
    fun clearEvents() {
        _events.value = emptyList()
    }
}
