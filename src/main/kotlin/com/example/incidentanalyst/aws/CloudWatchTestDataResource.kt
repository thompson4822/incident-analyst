package com.example.incidentanalyst.aws

import jakarta.ws.rs.Consumes
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response

data class CloudWatchTestDataErrorResponseDto(val message: String)

@Path("/test/cloudwatch")
class CloudWatchTestDataResource(
    private val generatorService: CloudWatchTestDataGeneratorService
) {

    @POST
    @Path("/generate-alarms")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun generateAlarms(request: CloudWatchTestDataRequestDto): Response {
        val result = generatorService.generateAlarms(request)

        return when (result) {
            is CloudWatchTestDataGenerationResult.Success -> {
                val responseDto = CloudWatchTestDataResponseDto(
                    generatedCount = result.generatedCount,
                    severityBreakdown = result.severityBreakdown,
                    createdIncidentIds = result.createdIncidentIds,
                    seedUsed = result.seedUsed
                )
                Response.ok(responseDto).build()
            }
            is CloudWatchTestDataGenerationResult.ValidationError -> {
                Response.status(Response.Status.BAD_REQUEST)
                    .entity(CloudWatchTestDataErrorResponseDto(result.message))
                    .build()
            }
        }
    }
}
