package com.example.incidentanalyst.diagnosis

import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType

@Path("/diagnoses")
class DiagnosisResource(
    private val diagnosisRepository: DiagnosisRepository
) {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    fun list(): List<DiagnosisResponseDto> =
        diagnosisRepository.listAll().map { entity ->
            val d = entity.toDomain()
            DiagnosisResponseDto(
                id = d.id.value,
                incidentId = d.incidentId.value,
                rootCause = d.rootCause,
                steps = d.steps,
                confidence = d.confidence.name,
                verified = d.verification is DiagnosisVerification.VerifiedByHuman
            )
        }
}
