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
            title = request.title!!,
            description = request.description!!,
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
            title = request.title!!,
            description = request.description!!,
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

    @Test
    fun `POST ingest returns 400 for missing title`() {
        val request = GenericIncidentRequestDto(
            title = null,
            description = "Test description",
            severity = "MEDIUM",
            source = "test"
        )

        given()
            .header("X-API-Key", apiKey)
            .contentType(ContentType.JSON)
            .body(request)
            .`when`()
            .post("/ingest/webhook")
            .then()
            .statusCode(400)
            .body("message", equalTo("Validation failed"))

        verifyNoInteractions(incidentService)
    }

    @Test
    fun `POST ingest returns 400 for blank title`() {
        val request = GenericIncidentRequestDto(
            title = "",
            description = "Test description",
            severity = "MEDIUM",
            source = "test"
        )

        given()
            .header("X-API-Key", apiKey)
            .contentType(ContentType.JSON)
            .body(request)
            .`when`()
            .post("/ingest/webhook")
            .then()
            .statusCode(400)
            .body("message", equalTo("Validation failed"))

        verifyNoInteractions(incidentService)
    }

    @Test
    fun `POST ingest returns 400 for missing description`() {
        val request = GenericIncidentRequestDto(
            title = "Test title",
            description = null,
            severity = "MEDIUM",
            source = "test"
        )

        given()
            .header("X-API-Key", apiKey)
            .contentType(ContentType.JSON)
            .body(request)
            .`when`()
            .post("/ingest/webhook")
            .then()
            .statusCode(400)
            .body("message", equalTo("Validation failed"))

        verifyNoInteractions(incidentService)
    }

    @Test
    fun `POST ingest returns 400 for missing source`() {
        val request = GenericIncidentRequestDto(
            title = "Test title",
            description = "Test description",
            severity = "MEDIUM",
            source = null
        )

        given()
            .header("X-API-Key", apiKey)
            .contentType(ContentType.JSON)
            .body(request)
            .`when`()
            .post("/ingest/webhook")
            .then()
            .statusCode(400)
            .body("message", equalTo("Validation failed"))

        verifyNoInteractions(incidentService)
    }

    @Test
    fun `POST ingest returns 400 for title exceeding max length`() {
        val longTitle = "x".repeat(GenericIncidentRequestDto.MAX_TITLE_LENGTH + 1)
        val request = GenericIncidentRequestDto(
            title = longTitle,
            description = "Test description",
            severity = "MEDIUM",
            source = "test"
        )

        given()
            .header("X-API-Key", apiKey)
            .contentType(ContentType.JSON)
            .body(request)
            .`when`()
            .post("/ingest/webhook")
            .then()
            .statusCode(400)
            .body("message", equalTo("Validation failed"))

        verifyNoInteractions(incidentService)
    }

    @Test
    fun `POST ingest returns 400 for source exceeding max length`() {
        val longSource = "x".repeat(GenericIncidentRequestDto.MAX_SOURCE_LENGTH + 1)
        val request = GenericIncidentRequestDto(
            title = "Test title",
            description = "Test description",
            severity = "MEDIUM",
            source = longSource
        )

        given()
            .header("X-API-Key", apiKey)
            .contentType(ContentType.JSON)
            .body(request)
            .`when`()
            .post("/ingest/webhook")
            .then()
            .statusCode(400)
            .body("message", equalTo("Validation failed"))

        verifyNoInteractions(incidentService)
    }

    @Test
    fun `POST ingest returns 400 with multiple validation errors`() {
        val request = GenericIncidentRequestDto(
            title = "",
            description = null,
            severity = "MEDIUM",
            source = ""
        )

        given()
            .header("X-API-Key", apiKey)
            .contentType(ContentType.JSON)
            .body(request)
            .`when`()
            .post("/ingest/webhook")
            .then()
            .statusCode(400)
            .body("message", equalTo("Validation failed"))
            .body("errors.size()", equalTo(3))

        verifyNoInteractions(incidentService)
    }

    // ========== Severity Level Tests ==========

    @Test
    fun `POST ingest creates incident with HIGH severity`() {
        testSeverityLevel("HIGH", Severity.HIGH)
    }

    @Test
    fun `POST ingest creates incident with MEDIUM severity`() {
        testSeverityLevel("MEDIUM", Severity.MEDIUM)
    }

    @Test
    fun `POST ingest creates incident with LOW severity`() {
        testSeverityLevel("LOW", Severity.LOW)
    }

    @Test
    fun `POST ingest creates incident with INFO severity`() {
        testSeverityLevel("INFO", Severity.INFO)
    }

    private fun testSeverityLevel(severityString: String, expectedSeverity: Severity) {
        val request = GenericIncidentRequestDto(
            title = "Test incident",
            description = "Test description",
            severity = severityString,
            source = "test"
        )

        val createdIncident = Incident(
            id = IncidentId(1L),
            source = "test",
            title = request.title!!,
            description = request.description!!,
            severity = expectedSeverity,
            status = IncidentStatus.Open,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        whenever(incidentService.create(any())).thenReturn(createdIncident)

        given()
            .header("X-API-Key", apiKey)
            .contentType(ContentType.JSON)
            .body(request)
            .`when`()
            .post("/ingest/webhook")
            .then()
            .statusCode(201)
            .body("message", equalTo("Incident created"))

        verify(incidentService).create(argThat {
            severity == expectedSeverity
        })
    }

    // ========== Case-Insensitive Severity Tests ==========

    @Test
    fun `POST ingest handles lowercase severity`() {
        testCaseInsensitiveSeverity("critical", Severity.CRITICAL)
    }

    @Test
    fun `POST ingest handles mixed case severity`() {
        testCaseInsensitiveSeverity("HiGh", Severity.HIGH)
    }

    private fun testCaseInsensitiveSeverity(severityString: String, expectedSeverity: Severity) {
        val request = GenericIncidentRequestDto(
            title = "Test incident",
            description = "Test description",
            severity = severityString,
            source = "test"
        )

        val createdIncident = Incident(
            id = IncidentId(1L),
            source = "test",
            title = request.title!!,
            description = request.description!!,
            severity = expectedSeverity,
            status = IncidentStatus.Open,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        whenever(incidentService.create(any())).thenReturn(createdIncident)

        given()
            .header("X-API-Key", apiKey)
            .contentType(ContentType.JSON)
            .body(request)
            .`when`()
            .post("/ingest/webhook")
            .then()
            .statusCode(201)

        verify(incidentService).create(argThat {
            severity == expectedSeverity
        })
    }

    // ========== Blank/Whitespace Field Tests ==========

    @Test
    fun `POST ingest returns 400 for whitespace-only description`() {
        val request = GenericIncidentRequestDto(
            title = "Test title",
            description = "   \t\n  ",
            severity = "MEDIUM",
            source = "test"
        )

        given()
            .header("X-API-Key", apiKey)
            .contentType(ContentType.JSON)
            .body(request)
            .`when`()
            .post("/ingest/webhook")
            .then()
            .statusCode(400)
            .body("message", equalTo("Validation failed"))

        verifyNoInteractions(incidentService)
    }

    @Test
    fun `POST ingest returns 400 for whitespace-only source`() {
        val request = GenericIncidentRequestDto(
            title = "Test title",
            description = "Test description",
            severity = "MEDIUM",
            source = "   "
        )

        given()
            .header("X-API-Key", apiKey)
            .contentType(ContentType.JSON)
            .body(request)
            .`when`()
            .post("/ingest/webhook")
            .then()
            .statusCode(400)
            .body("message", equalTo("Validation failed"))

        verifyNoInteractions(incidentService)
    }

    @Test
    fun `POST ingest returns 400 for whitespace-only title`() {
        val request = GenericIncidentRequestDto(
            title = "   \t   ",
            description = "Test description",
            severity = "MEDIUM",
            source = "test"
        )

        given()
            .header("X-API-Key", apiKey)
            .contentType(ContentType.JSON)
            .body(request)
            .`when`()
            .post("/ingest/webhook")
            .then()
            .statusCode(400)
            .body("message", equalTo("Validation failed"))

        verifyNoInteractions(incidentService)
    }

    // ========== Unicode Character Tests ==========

    @Test
    fun `POST ingest handles unicode characters in fields`() {
        val request = GenericIncidentRequestDto(
            title = "错误: 数据库连接失败 🚨",
            description = "描述: 服务不可用\n详细信息: 连接超时 错误代码: 500",
            severity = "CRITICAL",
            source = "监控服务-中文"
        )

        val createdIncident = Incident(
            id = IncidentId(1L),
            source = request.source!!,
            title = request.title!!,
            description = request.description!!,
            severity = Severity.CRITICAL,
            status = IncidentStatus.Open,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        whenever(incidentService.create(any())).thenReturn(createdIncident)

        given()
            .header("X-API-Key", apiKey)
            .contentType(ContentType.JSON)
            .body(request)
            .`when`()
            .post("/ingest/webhook")
            .then()
            .statusCode(201)
            .body("message", equalTo("Incident created"))

        verify(incidentService).create(argThat {
            title == request.title &&
            description == request.description &&
            source == request.source
        })
    }

    // ========== Long Description Test (should be allowed) ==========

    @Test
    fun `POST ingest accepts very long description`() {
        val longDescription = "x".repeat(10000) // 10KB description
        val request = GenericIncidentRequestDto(
            title = "Test title",
            description = longDescription,
            severity = "MEDIUM",
            source = "test"
        )

        val createdIncident = Incident(
            id = IncidentId(1L),
            source = "test",
            title = request.title!!,
            description = longDescription,
            severity = Severity.MEDIUM,
            status = IncidentStatus.Open,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        whenever(incidentService.create(any())).thenReturn(createdIncident)

        given()
            .header("X-API-Key", apiKey)
            .contentType(ContentType.JSON)
            .body(request)
            .`when`()
            .post("/ingest/webhook")
            .then()
            .statusCode(201)
            .body("message", equalTo("Incident created"))

        verify(incidentService).create(argThat {
            description == longDescription
        })
    }

    // ========== Boundary Tests for Max Length ==========

    @Test
    fun `POST ingest accepts title at exactly max length`() {
        val maxTitle = "x".repeat(GenericIncidentRequestDto.MAX_TITLE_LENGTH)
        val request = GenericIncidentRequestDto(
            title = maxTitle,
            description = "Test description",
            severity = "MEDIUM",
            source = "test"
        )

        val createdIncident = Incident(
            id = IncidentId(1L),
            source = "test",
            title = maxTitle,
            description = request.description!!,
            severity = Severity.MEDIUM,
            status = IncidentStatus.Open,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        whenever(incidentService.create(any())).thenReturn(createdIncident)

        given()
            .header("X-API-Key", apiKey)
            .contentType(ContentType.JSON)
            .body(request)
            .`when`()
            .post("/ingest/webhook")
            .then()
            .statusCode(201)
            .body("message", equalTo("Incident created"))

        verify(incidentService).create(argThat {
            title == maxTitle
        })
    }

    @Test
    fun `POST ingest accepts source at exactly max length`() {
        val maxSource = "x".repeat(GenericIncidentRequestDto.MAX_SOURCE_LENGTH)
        val request = GenericIncidentRequestDto(
            title = "Test title",
            description = "Test description",
            severity = "MEDIUM",
            source = maxSource
        )

        val createdIncident = Incident(
            id = IncidentId(1L),
            source = maxSource,
            title = request.title!!,
            description = request.description!!,
            severity = Severity.MEDIUM,
            status = IncidentStatus.Open,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        whenever(incidentService.create(any())).thenReturn(createdIncident)

        given()
            .header("X-API-Key", apiKey)
            .contentType(ContentType.JSON)
            .body(request)
            .`when`()
            .post("/ingest/webhook")
            .then()
            .statusCode(201)
            .body("message", equalTo("Incident created"))

        verify(incidentService).create(argThat {
            source == maxSource
        })
    }

    // ========== Error Handling Test ==========

    @Test
    fun `POST ingest returns 500 when incidentService throws exception`() {
        val request = GenericIncidentRequestDto(
            title = "Test title",
            description = "Test description",
            severity = "MEDIUM",
            source = "test"
        )

        whenever(incidentService.create(any())).thenThrow(RuntimeException("Database error"))

        given()
            .header("X-API-Key", apiKey)
            .contentType(ContentType.JSON)
            .body(request)
            .`when`()
            .post("/ingest/webhook")
            .then()
            .statusCode(500)
            .body("message", equalTo("Internal server error"))

        verify(incidentService).create(any())
    }
}
