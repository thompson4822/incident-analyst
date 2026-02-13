package com.example.incidentanalyst.ingestion

import jakarta.ws.rs.Consumes
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response

@Path("/ingest/webhook")
class WebhookIngestionResource(
    private val webhookIngestionService: WebhookIngestionService
) {
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun ingest(
        @HeaderParam("X-API-Key") providedKey: String?,
        request: GenericIncidentRequestDto
    ): Response {
        return webhookIngestionService.ingest(request, providedKey).fold(
            ifLeft = { error ->
                when (error) {
                    is WebhookIngestionError.Unauthorized ->
                        Response.status(Response.Status.UNAUTHORIZED)
                            .entity(WebhookResponseDto("Unauthorized"))
                            .build()
                    is WebhookIngestionError.ValidationError ->
                        Response.status(Response.Status.BAD_REQUEST)
                            .entity(ValidationErrorResponseDto(errors = error.errors))
                            .build()
                    is WebhookIngestionError.PersistenceError ->
                        Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                            .entity(WebhookResponseDto("Internal server error"))
                            .build()
                }
            },
            ifRight = { success ->
                when (success) {
                    is WebhookIngestionSuccess.IncidentCreated ->
                        Response.status(Response.Status.CREATED)
                            .entity(WebhookResponseDto("Incident created", success.id))
                            .build()
                }
            }
        )
    }
}
