package com.example.incidentanalyst.ingestion

import com.example.incidentanalyst.incident.IncidentService
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.jboss.logging.Logger

@Path("/ingest/webhook")
class WebhookIngestionResource(
    private val incidentService: IncidentService,
    @ConfigProperty(name = "app.ingestion.webhook.api-key")
    private val apiKey: String
) {
    private val log = Logger.getLogger(javaClass)

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun ingest(
        @HeaderParam("X-API-Key") providedKey: String?,
        request: GenericIncidentRequestDto
    ): Response {
        if (providedKey == null || providedKey != apiKey) {
            log.warnf("Unauthorized webhook ingestion attempt. providedKey=%s", providedKey)
            return Response.status(Response.Status.UNAUTHORIZED)
                .entity(WebhookResponseDto("Unauthorized"))
                .build()
        }

        return try {
            val incident = request.toDomain()
            val created = incidentService.create(incident)
            
            log.infof(
                "Successfully ingested incident via webhook. source=%s, title=%s, id=%d",
                created.source,
                created.title,
                created.id.value
            )

            Response.status(Response.Status.CREATED)
                .entity(WebhookResponseDto("Incident created", created.id.value))
                .build()
        } catch (e: Exception) {
            log.error("Failed to ingest incident via webhook", e)
            Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(WebhookResponseDto("Internal server error"))
                .build()
        }
    }
}
