package com.example.incidentanalyst.runbook

import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType

@Path("/runbooks")
class RunbookResource(
    private val runbookFragmentRepository: RunbookFragmentRepository
) {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    fun list(): List<RunbookFragment> =
        runbookFragmentRepository.listAll().map { it.toDomain() }
}
