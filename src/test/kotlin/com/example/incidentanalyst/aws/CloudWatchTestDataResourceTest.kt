package com.example.incidentanalyst.aws

import io.quarkus.test.InjectMock
import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.notNullValue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.reset

@QuarkusTest
class CloudWatchTestDataResourceTest {

    @InjectMock
    lateinit var generatorService: CloudWatchTestDataGeneratorService

    @BeforeEach
    fun setup() {
        reset(generatorService)
    }

    @Test
    fun `POST generate-alarms maps Success response`() {
        `when`(generatorService.generateAlarms(anyRequest()))
            .thenReturn(
                CloudWatchTestDataGenerationResult.Success(
                    generatedCount = 2,
                    severityBreakdown = mapOf("HIGH" to 2),
                    createdIncidentIds = listOf(1L, 2L),
                    seedUsed = 123L
                )
            )

        val requestBody = mapOf<String, Any?>(
            "count" to 2,
            "seed" to 123L
        )

        given()
            .contentType(ContentType.JSON)
            .body(requestBody)
            .`when`()
            .post("/test/cloudwatch/generate-alarms")
            .then()
            .statusCode(200)
            .body("generatedCount", equalTo(2))
            .body("severityBreakdown.HIGH", equalTo(2))
            .body("createdIncidentIds.size()", equalTo(2))
            .body("seedUsed", equalTo(123))
    }

    @Test
    fun `POST generate-alarms maps ValidationError to 400`() {
        `when`(generatorService.generateAlarms(anyRequest()))
            .thenReturn(
                CloudWatchTestDataGenerationResult.ValidationError("Count must be non-negative")
            )

        val requestBody = mapOf<String, Any?>(
            "count" to -1
        )

        given()
            .contentType(ContentType.JSON)
            .body(requestBody)
            .`when`()
            .post("/test/cloudwatch/generate-alarms")
            .then()
            .statusCode(400)
            .body("message", notNullValue())
    }

    @Suppress("UNCHECKED_CAST")
    private fun anyRequest(): CloudWatchTestDataRequestDto =
        org.mockito.ArgumentMatchers.any(CloudWatchTestDataRequestDto::class.java)
            ?: CloudWatchTestDataRequestDto()
}
