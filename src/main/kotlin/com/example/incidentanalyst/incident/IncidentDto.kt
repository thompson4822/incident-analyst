package com.example.incidentanalyst.incident

data class IncidentResponseDto(
    val id: Long,
    val source: String,
    val title: String,
    val description: String,
    val severity: String,
    val status: String
)

// Extension function to convert Incident domain model to DTO
fun Incident.toResponseDto(): IncidentResponseDto =
    IncidentResponseDto(
        id = id.value,
        source = source.displayName,
        title = title,
        description = description,
        severity = severity.name,
        status = when (status) {
            is IncidentStatus.Open -> "OPEN"
            is IncidentStatus.Acknowledged -> "ACK"
            is IncidentStatus.Diagnosed -> "DIAGNOSED:${status.diagnosisId}"
            is IncidentStatus.Resolved -> "RESOLVED"
        }
    )
