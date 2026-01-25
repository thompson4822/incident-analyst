package com.example.incidentanalyst.incident

import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response

@Path("/incidents")
class IncidentResource(
    private val incidentService: IncidentService
) {



    @GET
    @Produces(MediaType.APPLICATION_JSON)
    fun list(): List<IncidentResponseDto> =
        incidentService.listRecent().map { it.toResponseDto() }

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    fun getById(@PathParam("id") id: Long): Response {
        // Validate ID - reject negative or zero values
        if (id <= 0) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(mapOf("error" to "Incident ID must be a positive number"))
                .build()
        }
        
        val incidentId = IncidentId(id)
        return when (val result = incidentService.getById(incidentId)) {
            is IncidentResult.Success -> Response.ok(result.incident.toResponseDto()).build()
            is IncidentResult.Failure -> Response.status(Response.Status.NOT_FOUND).build()
        }
    }
}
