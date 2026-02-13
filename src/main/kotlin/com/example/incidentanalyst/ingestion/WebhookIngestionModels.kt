package com.example.incidentanalyst.ingestion

sealed interface WebhookIngestionError {
    data object Unauthorized : WebhookIngestionError
    data class ValidationError(val errors: List<String>) : WebhookIngestionError
    data class PersistenceError(val message: String) : WebhookIngestionError
}

sealed interface WebhookIngestionSuccess {
    data class IncidentCreated(val id: Long, val source: String, val title: String) : WebhookIngestionSuccess
}
