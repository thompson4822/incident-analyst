package com.example.incidentanalyst.runbook

import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response

@Path("/runbooks")
class RunbookResource(
    private val runbookService: RunbookService
) {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    fun list(): List<RunbookFragmentResponseDto> =
        runbookService.listRecent().map { it.toResponseDto() }

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    fun getById(@PathParam("id") id: Long): Response {
        val fragmentId = RunbookFragmentId(id)
        return when (val result = runbookService.getById(fragmentId)) {
            is RunbookFragmentResult.Success -> Response.ok(result.fragment.toResponseDto()).build()
            is RunbookFragmentResult.Failure -> when (result.error) {
                is RunbookFragmentError.NotFound -> Response.status(Response.Status.NOT_FOUND).build()
                is RunbookFragmentError.ValidationFailed -> Response.status(Response.Status.BAD_REQUEST)
                    .type(MediaType.APPLICATION_JSON)
                    .entity(mapOf("error" to "Invalid request data"))
                    .build()
            }
        }
    }

    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun updateFragment(
        @PathParam("id") id: Long,
        request: RunbookFragmentUpdateRequestDto
    ): Response {
        val fragmentId = RunbookFragmentId(id)
        return when (val result = runbookService.updateFragment(fragmentId, request.title, request.content, request.tags)) {
            is RunbookFragmentResult.Success -> Response.ok(result.fragment.toResponseDto()).build()
            is RunbookFragmentResult.Failure -> when (result.error) {
                is RunbookFragmentError.NotFound -> Response.status(Response.Status.NOT_FOUND).build()
                is RunbookFragmentError.ValidationFailed -> Response.status(Response.Status.BAD_REQUEST)
                    .type(MediaType.APPLICATION_JSON)
                    .entity(mapOf("error" to "Invalid request data"))
                    .build()
            }
        }
    }
}
