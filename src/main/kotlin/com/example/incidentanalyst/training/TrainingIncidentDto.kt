package com.example.incidentanalyst.training

import com.example.incidentanalyst.incident.IncidentId
import com.example.incidentanalyst.incident.Severity
import java.time.Instant

data class TrainingIncidentRequestDto(
    val title: String,
    val description: String,
    val severity: Severity,
    val timestamp: Instant?,
    val stackTrace: String?,
    val source: String = "training"
)

data class TrainingIncidentResponseDto(
    val id: Long,
    val source: String,
    val title: String,
    val description: String,
    val severity: Severity,
    val status: String,
    val createdAt: Instant,
    val updatedAt: Instant
)

fun toTrainingIncidentResponseDto(incident: com.example.incidentanalyst.incident.Incident) =
    TrainingIncidentResponseDto(
        id = incident.id.value,
        source = incident.source,
        title = incident.title,
        description = incident.description,
        severity = incident.severity,
        status = incident.status.toString(),
        createdAt = incident.createdAt,
        updatedAt = incident.updatedAt
    )
