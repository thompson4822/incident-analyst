package com.example.incidentanalyst.training

import com.example.incidentanalyst.incident.IncidentId
import com.example.incidentanalyst.incident.Severity
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant

data class TrainingIncidentRequestDto(
    @field:NotBlank(message = "title is required and must not be blank")
    @field:Size(max = 500, message = "title must not exceed 500 characters")
    val title: String,

    @field:NotBlank(message = "description is required and must not be blank")
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
