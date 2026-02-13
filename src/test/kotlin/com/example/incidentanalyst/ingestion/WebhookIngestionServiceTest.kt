package com.example.incidentanalyst.ingestion

import com.example.incidentanalyst.common.Either
import com.example.incidentanalyst.incident.Incident
import com.example.incidentanalyst.incident.IncidentId
import com.example.incidentanalyst.incident.IncidentService
import com.example.incidentanalyst.incident.IncidentSource
import com.example.incidentanalyst.incident.IncidentStatus
import com.example.incidentanalyst.incident.Severity
import io.quarkus.test.InjectMock
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.mockito.kotlin.any
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import java.time.Instant

@QuarkusTest
class WebhookIngestionServiceTest {

    @InjectMock
    lateinit var incidentService: IncidentService

    @Inject
    lateinit var webhookIngestionService: WebhookIngestionService

    private val validApiKey = "dev-secret-key"

    @BeforeEach
    fun setup() {
        reset(incidentService)
    }

    // ========== Authorization Tests ==========

    @Test
    fun `ingest returns Unauthorized when API key is null`() {
        val request = GenericIncidentRequestDto(
            title = "Test",
            description = "Description",
            severity = "HIGH",
            source = "sentry"
        )

        val result = webhookIngestionService.ingest(request, null)

        assertTrue(result.isLeft())
        val error = (result as Either.Left).value
        assertTrue(error is WebhookIngestionError.Unauthorized)
    }

    @Test
    fun `ingest returns Unauthorized when API key is invalid`() {
        val request = GenericIncidentRequestDto(
            title = "Test",
            description = "Description",
            severity = "HIGH",
            source = "sentry"
        )

        val result = webhookIngestionService.ingest(request, "wrong-key")

        assertTrue(result.isLeft())
        val error = (result as Either.Left).value
        assertTrue(error is WebhookIngestionError.Unauthorized)
    }

    // ========== Validation Tests ==========

    @Test
    fun `ingest returns ValidationError when title is null`() {
        val request = GenericIncidentRequestDto(
            title = null,
            description = "Description",
            severity = "HIGH",
            source = "sentry"
        )

        val result = webhookIngestionService.ingest(request, validApiKey)

        assertTrue(result.isLeft())
        val error = (result as Either.Left).value as WebhookIngestionError.ValidationError
        assertTrue(error.errors.any { it.contains("title", ignoreCase = true) })
    }

    @Test
    fun `ingest returns ValidationError when title is blank`() {
        val request = GenericIncidentRequestDto(
            title = "",
            description = "Description",
            severity = "HIGH",
            source = "sentry"
        )

        val result = webhookIngestionService.ingest(request, validApiKey)

        assertTrue(result.isLeft())
        val error = (result as Either.Left).value as WebhookIngestionError.ValidationError
        assertTrue(error.errors.any { it.contains("title", ignoreCase = true) })
    }

    @Test
    fun `ingest returns ValidationError when description is null`() {
        val request = GenericIncidentRequestDto(
            title = "Test",
            description = null,
            severity = "HIGH",
            source = "sentry"
        )

        val result = webhookIngestionService.ingest(request, validApiKey)

        assertTrue(result.isLeft())
        val error = (result as Either.Left).value as WebhookIngestionError.ValidationError
        assertTrue(error.errors.any { it.contains("description", ignoreCase = true) })
    }

    @Test
    fun `ingest returns ValidationError when source is null`() {
        val request = GenericIncidentRequestDto(
            title = "Test",
            description = "Description",
            severity = "HIGH",
            source = null
        )

        val result = webhookIngestionService.ingest(request, validApiKey)

        assertTrue(result.isLeft())
        val error = (result as Either.Left).value as WebhookIngestionError.ValidationError
        assertTrue(error.errors.any { it.contains("source", ignoreCase = true) })
    }

    @Test
    fun `ingest returns ValidationError with multiple errors when multiple fields invalid`() {
        val request = GenericIncidentRequestDto(
            title = "",
            description = null,
            severity = "HIGH",
            source = ""
        )

        val result = webhookIngestionService.ingest(request, validApiKey)

        assertTrue(result.isLeft())
        val error = (result as Either.Left).value as WebhookIngestionError.ValidationError
        assertTrue(error.errors.size >= 3)
    }

    // ========== Success Tests ==========

    @Test
    fun `ingest returns IncidentCreated on success`() {
        val request = GenericIncidentRequestDto(
            title = "Sentry Error",
            description = "NPE in service",
            severity = "CRITICAL",
            source = "sentry"
        )

        val createdIncident = Incident(
            id = IncidentId(42L),
            source = IncidentSource.Sentry,
            title = "Sentry Error",
            description = "NPE in service",
            severity = Severity.CRITICAL,
            status = IncidentStatus.Open,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        whenever(incidentService.create(any())).thenReturn(createdIncident)

        val result = webhookIngestionService.ingest(request, validApiKey)

        assertTrue(result.isRight())
        val success = (result as Either.Right).value as WebhookIngestionSuccess.IncidentCreated
        assertEquals(42L, success.id)
        assertEquals(IncidentSource.Sentry, success.source)
        assertEquals("Sentry Error", success.title)
    }

    @Test
    fun `ingest defaults severity to MEDIUM when invalid`() {
        val request = GenericIncidentRequestDto(
            title = "Test",
            description = "Description",
            severity = "INVALID",
            source = "sentry"
        )

        val createdIncident = Incident(
            id = IncidentId(1L),
            source = IncidentSource.Sentry,
            title = "Test",
            description = "Description",
            severity = Severity.MEDIUM,
            status = IncidentStatus.Open,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        whenever(incidentService.create(any())).thenReturn(createdIncident)

        val result = webhookIngestionService.ingest(request, validApiKey)

        assertTrue(result.isRight())
    }

    @Test
    fun `ingest handles lowercase severity`() {
        val request = GenericIncidentRequestDto(
            title = "Test",
            description = "Description",
            severity = "critical",
            source = "sentry"
        )

        val createdIncident = Incident(
            id = IncidentId(1L),
            source = IncidentSource.Sentry,
            title = "Test",
            description = "Description",
            severity = Severity.CRITICAL,
            status = IncidentStatus.Open,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        whenever(incidentService.create(any())).thenReturn(createdIncident)

        val result = webhookIngestionService.ingest(request, validApiKey)

        assertTrue(result.isRight())
    }

    // ========== Persistence Error Tests ==========

    @Test
    fun `ingest returns PersistenceError when incidentService throws`() {
        val request = GenericIncidentRequestDto(
            title = "Test",
            description = "Description",
            severity = "HIGH",
            source = "sentry"
        )

        whenever(incidentService.create(any())).thenThrow(RuntimeException("Database connection failed"))

        val result = webhookIngestionService.ingest(request, validApiKey)

        assertTrue(result.isLeft())
        val error = (result as Either.Left).value as WebhookIngestionError.PersistenceError
        assertTrue(error.message.contains("Database connection failed"))
    }

    @Test
    fun `ingest returns PersistenceError with default message when exception has no message`() {
        val request = GenericIncidentRequestDto(
            title = "Test",
            description = "Description",
            severity = "HIGH",
            source = "sentry"
        )

        whenever(incidentService.create(any())).thenThrow(RuntimeException())

        val result = webhookIngestionService.ingest(request, validApiKey)

        assertTrue(result.isLeft())
        val error = (result as Either.Left).value as WebhookIngestionError.PersistenceError
        assertEquals("Unknown error", error.message)
    }
}
