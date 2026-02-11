package com.example.incidentanalyst.ingestion

import com.example.incidentanalyst.incident.Incident
import com.example.incidentanalyst.incident.IncidentId
import com.example.incidentanalyst.incident.IncidentStatus
import com.example.incidentanalyst.incident.Severity
import java.time.Instant

data class GenericIncidentRequestDto(
    val title: String,
    val description: String,
    val severity: String,
    val source: String
) {
    fun toDomain(): Incident {
        val now = Instant.now()
        return Incident(
            id = IncidentId(0),
            source = source,
            title = title,
            description = description,
            severity = try {
                Severity.valueOf(severity.uppercase())
            } catch (e: Exception) {
                Severity.MEDIUM
            },
            status = IncidentStatus.Open,
            createdAt = now,
            updatedAt = now
        )
    }
}

data class WebhookResponseDto(
    val message: String,
    val incidentId: Long? = null
)
