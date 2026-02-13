package com.example.incidentanalyst.ingestion

import com.example.incidentanalyst.common.Either
import com.example.incidentanalyst.incident.IncidentService
import jakarta.enterprise.context.ApplicationScoped
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.jboss.logging.Logger

@ApplicationScoped
class WebhookIngestionService(
    private val incidentService: IncidentService,
    @ConfigProperty(name = "app.ingestion.webhook.api-key")
    private val apiKey: String
) {
    private val log = Logger.getLogger(javaClass)

    fun ingest(request: GenericIncidentRequestDto, providedKey: String?): Either<WebhookIngestionError, WebhookIngestionSuccess> {
        // 1. Check API key
        if (providedKey == null || providedKey != apiKey) {
            log.warn("Unauthorized webhook ingestion attempt")
            return Either.Left(WebhookIngestionError.Unauthorized)
        }

        // 2. Validate request
        val validationErrors = request.validate()
        if (validationErrors.isNotEmpty()) {
            log.debugf("Webhook validation failed: %s", validationErrors)
            return Either.Left(WebhookIngestionError.ValidationError(validationErrors))
        }

        // 3. Create incident
        return try {
            val incident = request.toDomain()
            val created = incidentService.create(incident)

            log.infof(
                "Successfully ingested incident via webhook. source=%s, title=%s, id=%d",
                created.source,
                created.title,
                created.id.value
            )

            Either.Right(
                WebhookIngestionSuccess.IncidentCreated(
                    id = created.id.value,
                    source = created.source,
                    title = created.title
                )
            )
        } catch (e: Exception) {
            log.error("Failed to ingest incident via webhook", e)
            Either.Left(WebhookIngestionError.PersistenceError(e.message ?: "Unknown error"))
        }
    }
}
