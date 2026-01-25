package com.example.incidentanalyst.incident

import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType

@Path("/incidents")
class IncidentResource(
    private val incidentService: IncidentService
) {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    fun list(): List<IncidentResponseDto> =
        incidentService.listRecent().map {
            IncidentResponseDto(
                id = it.id.value,
                source = it.source,
                title = it.title,
                description = it.description,
                severity = it.severity.name,
                status = when (it.status) {
                    is IncidentStatus.Open -> "OPEN"
                    is IncidentStatus.Acknowledged -> "ACK"
                    is IncidentStatus.Diagnosed -> "DIAGNOSED"
                    is IncidentStatus.Resolved -> "RESOLVED"
                }
            )
        }
}
