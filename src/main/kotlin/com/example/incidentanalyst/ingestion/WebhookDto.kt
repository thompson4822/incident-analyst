package com.example.incidentanalyst.ingestion

import com.example.incidentanalyst.incident.Incident
import com.example.incidentanalyst.incident.IncidentId
import com.example.incidentanalyst.incident.IncidentSource
import com.example.incidentanalyst.incident.IncidentStatus
import com.example.incidentanalyst.incident.Severity
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant

data class GenericIncidentRequestDto(
    @field:NotBlank(message = "title is required and must not be blank")
    @field:Size(max = MAX_TITLE_LENGTH, message = "title must not exceed $MAX_TITLE_LENGTH characters")
    val title: String?,

    @field:NotBlank(message = "description is required and must not be blank")
    val description: String?,

    val severity: String?,  // Optional, defaults to MEDIUM in toDomain()

    @field:NotBlank(message = "source is required and must not be blank")
    @field:Size(max = MAX_SOURCE_LENGTH, message = "source must not exceed $MAX_SOURCE_LENGTH characters")
    val source: String?
) {
    fun toDomain(): Incident {
        val now = Instant.now()
        return Incident(
            id = IncidentId(0),
            source = IncidentSource.parse(source ?: "unknown"),
            title = title ?: "",
            description = description ?: "",
            severity = try {
                Severity.valueOf((severity ?: "MEDIUM").uppercase())
            } catch (e: Exception) {
                Severity.MEDIUM
            },
            status = IncidentStatus.Open,
            createdAt = now,
            updatedAt = now
        )
    }

    companion object {
        const val MAX_TITLE_LENGTH = 500
        const val MAX_SOURCE_LENGTH = 100
    }
}

data class WebhookResponseDto(
    val message: String,
    val incidentId: Long? = null
)

data class ValidationErrorResponseDto(
    val message: String = "Validation failed",
    val errors: List<String>
)
