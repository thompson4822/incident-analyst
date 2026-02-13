package com.example.incidentanalyst.ingestion

import com.example.incidentanalyst.incident.IncidentSource

sealed interface WebhookIngestionError {
    data object Unauthorized : WebhookIngestionError
    data class ValidationError(val errors: List<String>) : WebhookIngestionError
    data class PersistenceError(val message: String) : WebhookIngestionError
}

sealed interface WebhookIngestionSuccess {
    data class IncidentCreated(val id: Long, val source: IncidentSource, val title: String) : WebhookIngestionSuccess
}
