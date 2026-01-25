package com.example.incidentanalyst.incident

import java.time.Instant

@JvmInline
value class IncidentId(val value: Long)

enum class Severity {
    CRITICAL, HIGH, MEDIUM, LOW, INFO
}

sealed interface IncidentStatus {
    data object Open : IncidentStatus
    data object Acknowledged : IncidentStatus
    data class Diagnosed(val diagnosisId: Long) : IncidentStatus
    data object Resolved : IncidentStatus
}

data class Incident(
    val id: IncidentId,
    val source: String,
    val title: String,
    val description: String,
    val severity: Severity,
    val status: IncidentStatus,
    val createdAt: Instant,
    val updatedAt: Instant
)

fun IncidentEntity.toDomain(): Incident =
    Incident(
        id = IncidentId(requireNotNull(id)),
        source = source,
        title = title,
        description = description,
        severity = Severity.valueOf(severity),
        status = when (status) {
            "OPEN" -> IncidentStatus.Open
            "ACK" -> IncidentStatus.Acknowledged
            "RESOLVED" -> IncidentStatus.Resolved
            else -> IncidentStatus.Open
        },
        createdAt = createdAt,
        updatedAt = updatedAt
    )
