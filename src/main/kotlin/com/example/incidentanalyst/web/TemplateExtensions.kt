package com.example.incidentanalyst.web

import com.example.incidentanalyst.incident.IncidentStatus
import com.example.incidentanalyst.incident.Severity
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit

fun Instant.toRelativeTime(): String {
    val now = Instant.now()
    val duration = Duration.between(this, now)

    return when {
        duration.seconds < 60 -> "just now"
        duration.toMinutes() < 60 -> "${duration.toMinutes()}m ago"
        duration.toHours() < 24 -> "${duration.toHours()}h ago"
        duration.toDays() < 7 -> "${duration.toDays()}d ago"
        else -> "${ChronoUnit.WEEKS.between(this, now)}w ago"
    }
}

fun Severity.toDaisyColor(): String {
    return when (this) {
        Severity.CRITICAL -> "error"
        Severity.HIGH -> "warning"
        Severity.MEDIUM -> "info"
        Severity.LOW -> "success"
        Severity.INFO -> "neutral"
    }
}

fun IncidentStatus.toDaisyColor(): String {
    return when (this) {
        IncidentStatus.Open -> "error"
        IncidentStatus.Acknowledged -> "warning"
        is IncidentStatus.Diagnosed -> "info"
        IncidentStatus.Resolved -> "success"
    }
}

fun IncidentStatus.toDisplayString(): String {
    return when (this) {
        IncidentStatus.Open -> "Open"
        IncidentStatus.Acknowledged -> "Acknowledged"
        is IncidentStatus.Diagnosed -> "Diagnosed"
        IncidentStatus.Resolved -> "Resolved"
    }
}
