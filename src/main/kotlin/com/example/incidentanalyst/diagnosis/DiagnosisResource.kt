package com.example.incidentanalyst.diagnosis

import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response

@Path("/diagnoses")
class DiagnosisResource(
    private val diagnosisService: DiagnosisService
) {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    fun list(): List<DiagnosisResponseDto> =
        diagnosisService.listRecent().map { it.toResponseDto() }

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    fun getById(@PathParam("id") id: Long): Response {
        // Validate ID - reject negative or zero values
        if (id <= 0) {
            return Response.status(Response.Status.BAD_REQUEST)
                .type(MediaType.APPLICATION_JSON)
                .entity(mapOf("error" to "Diagnosis ID must be a positive number"))
                .build()
        }
        
        val diagnosisId = DiagnosisId(id)
        return diagnosisService.getById(diagnosisId).fold(
            ifLeft = { Response.status(Response.Status.NOT_FOUND).build() },
            ifRight = { diagnosis -> Response.ok(diagnosis.toResponseDto()).build() }
        )
    }

    @PUT
    @Path("/{id}/verification")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun updateVerification(
        @PathParam("id") id: Long,
        request: DiagnosisVerificationUpdateRequestDto
    ): Response {
        // Validate ID - reject negative or zero values
        if (id <= 0) {
            return Response.status(Response.Status.BAD_REQUEST)
                .type(MediaType.APPLICATION_JSON)
                .entity(mapOf("error" to "Diagnosis ID must be a positive number"))
                .build()
        }
        
        val diagnosisId = DiagnosisId(id)
        val verification = if (request.verified) {
            DiagnosisVerification.VerifiedByHuman
        } else {
            DiagnosisVerification.Unverified
        }
        
        return diagnosisService.updateVerification(diagnosisId, verification).fold(
            ifLeft = { error ->
                when (error) {
                    is DiagnosisError.NotFound -> Response.status(Response.Status.NOT_FOUND).build()
                    is DiagnosisError.UpdateFailed -> Response.serverError().build()
                    else -> Response.serverError().build()
                }
            },
            ifRight = { diagnosis -> Response.ok(diagnosis.toResponseDto()).build() }
        )
    }

    @POST
    @Path("/{id}/verify")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun verify(
        @PathParam("id") id: Long,
        request: DiagnosisVerifyRequestDto
    ): Response {
        if (id <= 0) {
            return Response.status(Response.Status.BAD_REQUEST)
                .type(MediaType.APPLICATION_JSON)
                .entity(mapOf("error" to "Diagnosis ID must be a positive number"))
                .build()
        }

        if (request.verifiedBy.isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                .type(MediaType.APPLICATION_JSON)
                .entity(mapOf("error" to "verifiedBy is required"))
                .build()
        }

        val diagnosisId = DiagnosisId(id)
        return diagnosisService.verify(diagnosisId, request.verifiedBy).fold(
            ifLeft = { error ->
                when (error) {
                    is DiagnosisError.NotFound -> Response.status(Response.Status.NOT_FOUND).build()
                    else -> Response.serverError().build()
                }
            },
            ifRight = { diagnosis -> Response.ok(diagnosis.toResponseDto()).build() }
        )
    }
}
