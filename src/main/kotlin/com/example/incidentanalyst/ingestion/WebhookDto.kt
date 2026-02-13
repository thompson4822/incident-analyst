package com.example.incidentanalyst.ingestion

import com.example.incidentanalyst.incident.Incident
import com.example.incidentanalyst.incident.IncidentId
import com.example.incidentanalyst.incident.IncidentStatus
import com.example.incidentanalyst.incident.Severity
import java.time.Instant

data class GenericIncidentRequestDto(
    val title: String?,
    val description: String?,
    val severity: String?,
    val source: String?
) {
    fun validate(): List<String> {
        val errors = mutableListOf<String>()
        
        if (title.isNullOrBlank()) {
            errors.add("title is required and must not be blank")
        } else if (title.length > MAX_TITLE_LENGTH) {
            errors.add("title must not exceed $MAX_TITLE_LENGTH characters")
        }
        
        if (description.isNullOrBlank()) {
            errors.add("description is required and must not be blank")
        }
        
        if (source.isNullOrBlank()) {
            errors.add("source is required and must not be blank")
        } else if (source.length > MAX_SOURCE_LENGTH) {
            errors.add("source must not exceed $MAX_SOURCE_LENGTH characters")
        }
        
        return errors
    }
    
    fun toDomain(): Incident {
        val now = Instant.now()
        return Incident(
            id = IncidentId(0),
            source = source ?: "",
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
