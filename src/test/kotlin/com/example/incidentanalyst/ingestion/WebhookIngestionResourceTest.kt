package com.example.incidentanalyst.ingestion

import com.example.incidentanalyst.incident.Incident
import com.example.incidentanalyst.incident.IncidentId
import com.example.incidentanalyst.incident.IncidentService
import com.example.incidentanalyst.incident.IncidentStatus
import com.example.incidentanalyst.incident.Severity
import io.quarkus.test.InjectMock
import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import org.hamcrest.CoreMatchers.equalTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import java.time.Instant

@QuarkusTest
class WebhookIngestionResourceTest {

    @InjectMock
    lateinit var incidentService: IncidentService

    private val apiKey = "dev-secret-key"

    @BeforeEach
    fun setup() {
        reset(incidentService)
    }

    @Test
    fun `POST ingest returns 201 and creates incident for valid request`() {
        // Arrange
        val request = GenericIncidentRequestDto(
            title = "Sentry Error: NullPointerException",
            description = "NPE in UserService.kt:42",
            severity = "CRITICAL",
            source = "sentry"
        )
        
        val createdIncident = Incident(
            id = IncidentId(1L),
            source = "sentry",
            title = request.title,
            description = request.description,
            severity = Severity.CRITICAL,
            status = IncidentStatus.Open,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        
        whenever(incidentService.create(any())).thenReturn(createdIncident)

        // Act & Assert
        given()
            .header("X-API-Key", apiKey)
            .contentType(ContentType.JSON)
            .body(request)
            .`when`()
            .post("/ingest/webhook")
            .then()
            .statusCode(201)
            .body("message", equalTo("Incident created"))
            .body("incidentId", equalTo(1))

        verify(incidentService).create(argThat {
            title == request.title &&
            description == request.description &&
            severity == Severity.CRITICAL &&
            source == "sentry"
        })
    }

    @Test
    fun `POST ingest returns 401 for missing API key`() {
        val request = GenericIncidentRequestDto(
            title = "Test",
            description = "Test",
            severity = "MEDIUM",
            source = "test"
        )

        given()
            .contentType(ContentType.JSON)
            .body(request)
            .`when`()
            .post("/ingest/webhook")
            .then()
            .statusCode(401)
            .body("message", equalTo("Unauthorized"))

        verifyNoInteractions(incidentService)
    }

    @Test
    fun `POST ingest returns 401 for invalid API key`() {
        val request = GenericIncidentRequestDto(
            title = "Test",
            description = "Test",
            severity = "MEDIUM",
            source = "test"
        )

        given()
            .header("X-API-Key", "wrong-key")
            .contentType(ContentType.JSON)
            .body(request)
            .`when`()
            .post("/ingest/webhook")
            .then()
            .statusCode(401)
            .body("message", equalTo("Unauthorized"))

        verifyNoInteractions(incidentService)
    }

    @Test
    fun `POST ingest defaults to MEDIUM severity for invalid severity string`() {
        // Arrange
        val request = GenericIncidentRequestDto(
            title = "Test",
            description = "Test",
            severity = "INVALID_SEVERITY",
            source = "test"
        )
        
        val createdIncident = Incident(
            id = IncidentId(1L),
            source = "test",
            title = request.title,
            description = request.description,
            severity = Severity.MEDIUM,
            status = IncidentStatus.Open,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        
        whenever(incidentService.create(any())).thenReturn(createdIncident)

        // Act & Assert
        given()
            .header("X-API-Key", apiKey)
            .contentType(ContentType.JSON)
            .body(request)
            .`when`()
            .post("/ingest/webhook")
            .then()
            .statusCode(201)

        verify(incidentService).create(argThat {
            severity == Severity.MEDIUM
        })
    }
}
