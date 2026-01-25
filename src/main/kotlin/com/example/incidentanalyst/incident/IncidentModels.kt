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

sealed interface IncidentError {
    data object NotFound : IncidentError
}

sealed interface IncidentResult {
    data class Success(val incident: Incident) : IncidentResult
    data class Failure(val error: IncidentError) : IncidentResult
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
        severity = Severity.valueOf(severity.uppercase()),
        status = when (status.uppercase()) {
            "OPEN" -> IncidentStatus.Open
            "ACK" -> IncidentStatus.Acknowledged
            "RESOLVED" -> IncidentStatus.Resolved
            else -> {
                // Check for DIAGNOSED: format (case-insensitive)
                val upperStatus = status.uppercase()
                if (upperStatus.startsWith("DIAGNOSED:")) {
                    val diagnosisId = upperStatus.substringAfter("DIAGNOSED:").toLongOrNull()
                    if (diagnosisId != null) IncidentStatus.Diagnosed(diagnosisId) else IncidentStatus.Open
                } else {
                    // Log warning for unknown status strings - silently coerced to Open
                    // TODO: Add proper logging when logger is available
                    IncidentStatus.Open
                }
            }
        },
        createdAt = createdAt,
        updatedAt = updatedAt
    )
