package com.example.incidentanalyst.incident

import java.time.Instant

@JvmInline
value class IncidentId(val value: Long)

/**
 * Represents the source of an incident.
 * Using a sealed interface allows for exhaustive pattern matching
 * and type-safe handling of different incident origins.
 */
sealed interface IncidentSource {
    val displayName: String
    
    data object CloudWatch : IncidentSource {
        override val displayName: String = "CloudWatch"
    }
    
    data object Sentry : IncidentSource {
        override val displayName: String = "Sentry"
    }
    
    data object GitHub : IncidentSource {
        override val displayName: String = "GitHub"
    }
    
    data object PagerDuty : IncidentSource {
        override val displayName: String = "PagerDuty"
    }
    
    data object Manual : IncidentSource {
        override val displayName: String = "Manual"
    }
    
    data object Training : IncidentSource {
        override val displayName: String = "Training"
    }
    
    /**
     * Custom webhook source with a specific name.
     * Used for external integrations that don't have a predefined source type.
     */
    data class Webhook(val name: String) : IncidentSource {
        override val displayName: String = name
    }
    
    companion object {
        /**
         * Parse a source string into an IncidentSource.
         * Known sources are mapped to their specific types.
         * Unknown sources are wrapped in Webhook.
         */
        fun parse(value: String): IncidentSource = when (value.lowercase()) {
            "cloudwatch", "aws/cloudwatch" -> CloudWatch
            "sentry" -> Sentry
            "github" -> GitHub
            "pagerduty" -> PagerDuty
            "manual" -> Manual
            "training" -> Training
            else -> Webhook(value)
        }
    }
}

/**
 * Extension to convert IncidentSource to its string representation for persistence.
 */
fun IncidentSource.toPersistenceString(): String = when (this) {
    is IncidentSource.CloudWatch -> "cloudwatch"
    is IncidentSource.Sentry -> "sentry"
    is IncidentSource.GitHub -> "github"
    is IncidentSource.PagerDuty -> "pagerduty"
    is IncidentSource.Manual -> "manual"
    is IncidentSource.Training -> "training"
    is IncidentSource.Webhook -> name
}

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

data class Incident(
    val id: IncidentId,
    val source: IncidentSource,
    val title: String,
    val description: String,
    val severity: Severity,
    val status: IncidentStatus,
    val createdAt: Instant,
    val updatedAt: Instant,
    val resolutionText: String? = null
)

fun IncidentEntity.toDomain(): Incident =
    Incident(
        id = IncidentId(requireNotNull(id)),
        source = IncidentSource.parse(source),
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
        updatedAt = updatedAt,
        resolutionText = resolutionText
    )

fun Incident.toEntity(): IncidentEntity =
    IncidentEntity(
        id = if (id.value == 0L) null else id.value,
        source = source.toPersistenceString(),
        title = title,
        description = description,
        severity = severity.name,
        status = when (status) {
            IncidentStatus.Open -> "OPEN"
            IncidentStatus.Acknowledged -> "ACK"
            IncidentStatus.Resolved -> "RESOLVED"
            is IncidentStatus.Diagnosed -> "DIAGNOSED:${status.diagnosisId}"
        },
        createdAt = createdAt,
        updatedAt = updatedAt,
        resolutionText = resolutionText
    )
