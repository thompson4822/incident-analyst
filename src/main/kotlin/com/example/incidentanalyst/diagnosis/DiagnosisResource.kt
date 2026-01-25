package com.example.incidentanalyst.diagnosis

import jakarta.ws.rs.GET
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
        return when (val result = diagnosisService.getById(diagnosisId)) {
            is DiagnosisResult.Success -> Response.ok(result.diagnosis.toResponseDto()).build()
            is DiagnosisResult.Failure -> Response.status(Response.Status.NOT_FOUND).build()
        }
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
        
        return when (val result = diagnosisService.updateVerification(diagnosisId, verification)) {
            is DiagnosisResult.Success -> Response.ok(result.diagnosis.toResponseDto()).build()
            is DiagnosisResult.Failure -> when (result.error) {
                is DiagnosisError.NotFound -> Response.status(Response.Status.NOT_FOUND).build()
                is DiagnosisError.UpdateFailed -> Response.serverError().build()
                else -> Response.serverError().build()
            }
        }
    }
}
