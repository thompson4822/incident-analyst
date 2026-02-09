package com.example.incidentanalyst.incident

import com.example.incidentanalyst.common.Either
import io.quarkus.test.InjectMock
import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import jakarta.inject.Inject
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.CoreMatchers.nullValue
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.anyLong
import org.mockito.Mockito.reset
import org.mockito.Mockito.`when`
import java.time.Instant

@QuarkusTest
class IncidentResourceTest {

    @InjectMock
    lateinit var incidentService: IncidentService

    @BeforeEach
    fun setup() {
        reset(incidentService)
    }

    @Test
    fun `GET by id returns 200 with DTO for valid incident`() {
        // Arrange
        val testId = 123L
        val testTimestamp = Instant.now()
        val incident = Incident(
            id = IncidentId(testId),
            source = "monitoring",
            title = "High CPU usage",
            description = "CPU usage exceeded 90%",
            severity = Severity.HIGH,
            status = IncidentStatus.Open,
            createdAt = testTimestamp,
            updatedAt = testTimestamp
        )
        `when`(incidentService.getById(IncidentId(testId)))
            .thenReturn(Either.Right(incident))

        // Act & Assert
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .`when`()
            .get("/incidents/$testId")
            .then()
            .statusCode(200)
            .body("id", equalTo(testId.toInt()))
            .body("source", equalTo("monitoring"))
            .body("title", equalTo("High CPU usage"))
            .body("description", equalTo("CPU usage exceeded 90%"))
            .body("severity", equalTo("HIGH"))
            .body("status", equalTo("OPEN"))
    }

    @Test
    fun `GET by id returns 404 for non-existent incident`() {
        // Arrange
        val testId = 999L
        `when`(incidentService.getById(IncidentId(testId)))
            .thenReturn(Either.Left(IncidentError.NotFound))

        // Act & Assert
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .`when`()
            .get("/incidents/$testId")
            .then()
            .statusCode(404)
    }

    @Test
    fun `GET by id returns 400 for invalid negative ID`() {
        // Act & Assert
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .`when`()
            .get("/incidents/-1")
            .then()
            .statusCode(400)
            .body("error", notNullValue())
    }

    @Test
    fun `GET by id returns 400 for invalid zero ID`() {
        // Act & Assert
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .`when`()
            .get("/incidents/0")
            .then()
            .statusCode(400)
            .body("error", notNullValue())
    }

    @Test
    fun `GET by id returns 400 with appropriate error message for negative ID`() {
        // Act & Assert
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .`when`()
            .get("/incidents/-5")
            .then()
            .statusCode(400)
            .body("error", equalTo("Incident ID must be a positive number"))
    }

    @Test
    fun `GET by id returns diagnosisId in status for diagnosed incidents`() {
        // Arrange
        val testId = 456L
        val diagnosisId = 789L
        val testTimestamp = Instant.now()
        val incident = Incident(
            id = IncidentId(testId),
            source = "alerting",
            title = "Database connection failed",
            description = "Connection pool exhausted",
            severity = Severity.CRITICAL,
            status = IncidentStatus.Diagnosed(diagnosisId),
            createdAt = testTimestamp,
            updatedAt = testTimestamp
        )
        `when`(incidentService.getById(IncidentId(testId)))
            .thenReturn(Either.Right(incident))

        // Act & Assert
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .`when`()
            .get("/incidents/$testId")
            .then()
            .statusCode(200)
            .body("id", equalTo(testId.toInt()))
            .body("status", equalTo("DIAGNOSED:$diagnosisId"))
            .body("source", equalTo("alerting"))
            .body("title", equalTo("Database connection failed"))
    }

    @Test
    fun `GET by id maps ACK status correctly`() {
        // Arrange
        val testId = 789L
        val testTimestamp = Instant.now()
        val incident = Incident(
            id = IncidentId(testId),
            source = "support",
            title = "User report issue",
            description = "Users reporting slow page loads",
            severity = Severity.MEDIUM,
            status = IncidentStatus.Acknowledged,
            createdAt = testTimestamp,
            updatedAt = testTimestamp
        )
        `when`(incidentService.getById(IncidentId(testId)))
            .thenReturn(Either.Right(incident))

        // Act & Assert
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .`when`()
            .get("/incidents/$testId")
            .then()
            .statusCode(200)
            .body("status", equalTo("ACK"))
    }

    @Test
    fun `GET by id maps RESOLVED status correctly`() {
        // Arrange
        val testId = 321L
        val testTimestamp = Instant.now()
        val incident = Incident(
            id = IncidentId(testId),
            source = "monitoring",
            title = "Disk space warning",
            description = "Disk usage at 85%",
            severity = Severity.LOW,
            status = IncidentStatus.Resolved,
            createdAt = testTimestamp,
            updatedAt = testTimestamp
        )
        `when`(incidentService.getById(IncidentId(testId)))
            .thenReturn(Either.Right(incident))

        // Act & Assert
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .`when`()
            .get("/incidents/$testId")
            .then()
            .statusCode(200)
            .body("status", equalTo("RESOLVED"))
    }

    @Test
    fun `GET by id returns all DTO fields correctly`() {
        // Arrange
        val testId = 555L
        val testTimestamp = Instant.now()
        val incident = Incident(
            id = IncidentId(testId),
            source = "custom-monitor",
            title = "Custom alert",
            description = "Detailed description of the incident",
            severity = Severity.CRITICAL,
            status = IncidentStatus.Open,
            createdAt = testTimestamp,
            updatedAt = testTimestamp
        )
        `when`(incidentService.getById(IncidentId(testId)))
            .thenReturn(Either.Right(incident))

        // Act & Assert
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .`when`()
            .get("/incidents/$testId")
            .then()
            .statusCode(200)
            .body("id", equalTo(testId.toInt()))
            .body("source", equalTo("custom-monitor"))
            .body("title", equalTo("Custom alert"))
            .body("description", equalTo("Detailed description of the incident"))
            .body("severity", equalTo("CRITICAL"))
            .body("status", equalTo("OPEN"))
    }

    @Test
    fun `GET by id handles large diagnosisId in diagnosed status`() {
        // Arrange
        val testId = 999L
        val diagnosisId = 999999999L
        val testTimestamp = Instant.now()
        val incident = Incident(
            id = IncidentId(testId),
            source = "test",
            title = "Large diagnosisId",
            description = "Description",
            severity = Severity.HIGH,
            status = IncidentStatus.Diagnosed(diagnosisId),
            createdAt = testTimestamp,
            updatedAt = testTimestamp
        )
        `when`(incidentService.getById(IncidentId(testId)))
            .thenReturn(Either.Right(incident))

        // Act & Assert
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .`when`()
            .get("/incidents/$testId")
            .then()
            .statusCode(200)
            .body("status", equalTo("DIAGNOSED:$diagnosisId"))
    }

    @Test
    fun `GET by id handles all severity levels`() {
        // Arrange
        val severities = listOf(Severity.CRITICAL, Severity.HIGH, Severity.MEDIUM, Severity.LOW, Severity.INFO)
        severities.forEach { severity ->
            val testId = System.currentTimeMillis() % 10000
            val testTimestamp = Instant.now()
            val incident = Incident(
                id = IncidentId(testId),
                source = "test",
                title = "Severity test",
                description = "Description",
                severity = severity,
                status = IncidentStatus.Open,
                createdAt = testTimestamp,
                updatedAt = testTimestamp
            )
            `when`(incidentService.getById(IncidentId(testId)))
                .thenReturn(Either.Right(incident))

            // Act & Assert
            given()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .`when`()
                .get("/incidents/$testId")
                .then()
                .statusCode(200)
                .body("severity", equalTo(severity.name))
        }
    }

    @Test
    fun `GET by id with zero diagnosisId`() {
        // Arrange
        val testId = 777L
        val diagnosisId = 0L
        val testTimestamp = Instant.now()
        val incident = Incident(
            id = IncidentId(testId),
            source = "test",
            title = "Zero diagnosisId",
            description = "Description",
            severity = Severity.MEDIUM,
            status = IncidentStatus.Diagnosed(diagnosisId),
            createdAt = testTimestamp,
            updatedAt = testTimestamp
        )
        `when`(incidentService.getById(IncidentId(testId)))
            .thenReturn(Either.Right(incident))

        // Act & Assert
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .`when`()
            .get("/incidents/$testId")
            .then()
            .statusCode(200)
            .body("status", equalTo("DIAGNOSED:$diagnosisId"))
    }

    @Test
    fun `GET by id returns 400 for very large negative ID`() {
        // Act & Assert
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .`when`()
            .get("/incidents/-999999999999")
            .then()
            .statusCode(400)
    }

    @Test
    fun `GET by id returns 400 for ID at boundary Long Min`() {
        // Act & Assert - This should validate and reject even at boundary
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .`when`()
            .get("/incidents/-9223372036854775808")
            .then()
            .statusCode(400)
    }
}
