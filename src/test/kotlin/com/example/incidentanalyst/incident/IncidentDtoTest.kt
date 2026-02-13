package com.example.incidentanalyst.incident

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant
import com.example.incidentanalyst.incident.IncidentSource

class IncidentDtoTest {

    @Test
    fun `toResponseDto maps Open status to OPEN string`() {
        // Arrange
        val incident = Incident(
            id = IncidentId(123L),
            source = IncidentSource.Webhook("monitoring"),
            title = "Test incident",
            description = "Test description",
            severity = Severity.HIGH,
            status = IncidentStatus.Open,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        // Act
        val dto = incident.toResponseDto()

        // Assert
        assertEquals(123L, dto.id)
        assertEquals("monitoring", dto.source)
        assertEquals("Test incident", dto.title)
        assertEquals("Test description", dto.description)
        assertEquals("HIGH", dto.severity)
        assertEquals("OPEN", dto.status)
    }

    @Test
    fun `toResponseDto maps Acknowledged status to ACK string`() {
        // Arrange
        val incident = Incident(
            id = IncidentId(456L),
            source = IncidentSource.Webhook("alerting"),
            title = "Acknowledged incident",
            description = "Description",
            severity = Severity.MEDIUM,
            status = IncidentStatus.Acknowledged,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        // Act
        val dto = incident.toResponseDto()

        // Assert
        assertEquals("ACK", dto.status)
    }

    @Test
    fun `toResponseDto maps Resolved status to RESOLVED string`() {
        // Arrange
        val incident = Incident(
            id = IncidentId(789L),
            source = IncidentSource.Webhook("support"),
            title = "Resolved incident",
            description = "Description",
            severity = Severity.LOW,
            status = IncidentStatus.Resolved,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        // Act
        val dto = incident.toResponseDto()

        // Assert
        assertEquals("RESOLVED", dto.status)
    }

    @Test
    fun `toResponseDto maps Diagnosed status with diagnosisId`() {
        // Arrange
        val diagnosisId = 999L
        val incident = Incident(
            id = IncidentId(321L),
            source = IncidentSource.Webhook("monitoring"),
            title = "Diagnosed incident",
            description = "Description",
            severity = Severity.CRITICAL,
            status = IncidentStatus.Diagnosed(diagnosisId),
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        // Act
        val dto = incident.toResponseDto()

        // Assert
        assertEquals("DIAGNOSED:$diagnosisId", dto.status)
    }

    @Test
    fun `toResponseDto includes diagnosisId in status for diagnosed incidents`() {
        // Arrange
        val diagnosisId = 456789L
        val incident = Incident(
            id = IncidentId(555L),
            source = IncidentSource.Webhook("test"),
            title = "Test diagnosed",
            description = "Description",
            severity = Severity.HIGH,
            status = IncidentStatus.Diagnosed(diagnosisId),
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        // Act
        val dto = incident.toResponseDto()

        // Assert - Status should contain the diagnosisId
        assertTrue(dto.status.contains("DIAGNOSED:"))
        assertTrue(dto.status.contains(diagnosisId.toString()))
        assertEquals("DIAGNOSED:$diagnosisId", dto.status)
    }

    @Test
    fun `toResponseDto handles zero diagnosisId`() {
        // Arrange
        val diagnosisId = 0L
        val incident = Incident(
            id = IncidentId(666L),
            source = IncidentSource.Webhook("test"),
            title = "Zero diagnosisId",
            description = "Description",
            severity = Severity.MEDIUM,
            status = IncidentStatus.Diagnosed(diagnosisId),
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        // Act
        val dto = incident.toResponseDto()

        // Assert
        assertEquals("DIAGNOSED:$diagnosisId", dto.status)
    }

    @Test
    fun `toResponseDto handles negative diagnosisId`() {
        // Arrange
        val diagnosisId = -1L
        val incident = Incident(
            id = IncidentId(777L),
            source = IncidentSource.Webhook("test"),
            title = "Negative diagnosisId",
            description = "Description",
            severity = Severity.LOW,
            status = IncidentStatus.Diagnosed(diagnosisId),
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        // Act
        val dto = incident.toResponseDto()

        // Assert
        assertEquals("DIAGNOSED:$diagnosisId", dto.status)
    }

    @Test
    fun `toResponseDto handles very large diagnosisId`() {
        // Arrange
        val diagnosisId = 999999999L
        val incident = Incident(
            id = IncidentId(888L),
            source = IncidentSource.Webhook("test"),
            title = "Large diagnosisId",
            description = "Description",
            severity = Severity.CRITICAL,
            status = IncidentStatus.Diagnosed(diagnosisId),
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        // Act
        val dto = incident.toResponseDto()

        // Assert
        assertEquals("DIAGNOSED:$diagnosisId", dto.status)
    }

    @Test
    fun `toResponseDto maps all severity levels correctly`() {
        // Arrange
        val severities = mapOf(
            Severity.CRITICAL to "CRITICAL",
            Severity.HIGH to "HIGH",
            Severity.MEDIUM to "MEDIUM",
            Severity.LOW to "LOW",
            Severity.INFO to "INFO"
        )

        severities.forEach { (severityEnum, severityString) ->
            val incident = Incident(
                id = IncidentId(1000L + severityEnum.ordinal),
                source = IncidentSource.Webhook("test"),
                title = "Severity test",
                description = "Description",
                severity = severityEnum,
                status = IncidentStatus.Open,
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            )

            // Act
            val dto = incident.toResponseDto()

            // Assert
            assertEquals(severityString, dto.severity)
        }
    }

    @Test
    fun `toResponseDto maps all incident fields correctly`() {
        // Arrange
        val incident = Incident(
            id = IncidentId(999L),
            source = IncidentSource.Webhook("custom-source"),
            title = "Custom Title",
            description = "Custom Description",
            severity = Severity.HIGH,
            status = IncidentStatus.Open,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        // Act
        val dto = incident.toResponseDto()

        // Assert
        assertEquals(999L, dto.id)
        assertEquals("custom-source", dto.source)
        assertEquals("Custom Title", dto.title)
        assertEquals("Custom Description", dto.description)
        assertEquals("HIGH", dto.severity)
        assertEquals("OPEN", dto.status)
    }

    @Test
    fun `toResponseDto creates DTO with all fields populated`() {
        // Arrange
        val incident = Incident(
            id = IncidentId(111L),
            source = IncidentSource.Webhook("monitoring-system"),
            title = "High Memory Usage",
            description = "Memory usage exceeded 95% threshold",
            severity = Severity.CRITICAL,
            status = IncidentStatus.Acknowledged,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        // Act
        val dto = incident.toResponseDto()

        // Assert - All fields should be populated
        assertEquals(111L, dto.id)
        assertEquals("monitoring-system", dto.source)
        assertEquals("High Memory Usage", dto.title)
        assertEquals("Memory usage exceeded 95% threshold", dto.description)
        assertEquals("CRITICAL", dto.severity)
        assertEquals("ACK", dto.status)
    }

    @Test
    fun `toResponseDto maps diagnosed incident with complete information`() {
        // Arrange
        val diagnosisId = 12345L
        val incident = Incident(
            id = IncidentId(222L),
            source = IncidentSource.Webhook("alerting-system"),
            title = "Database Connection Failed",
            description = "Application unable to connect to primary database",
            severity = Severity.CRITICAL,
            status = IncidentStatus.Diagnosed(diagnosisId),
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        // Act
        val dto = incident.toResponseDto()

        // Assert
        assertEquals(222L, dto.id)
        assertEquals("alerting-system", dto.source)
        assertEquals("Database Connection Failed", dto.title)
        assertEquals("Application unable to connect to primary database", dto.description)
        assertEquals("CRITICAL", dto.severity)
        assertEquals("DIAGNOSED:$diagnosisId", dto.status)
    }

    @Test
    fun `toResponseDto preserves all incident properties`() {
        // Arrange
        val testTimestamp = Instant.now()
        val incident = Incident(
            id = IncidentId(333L),
            source = IncidentSource.Webhook("test-source"),
            title = "Test Title",
            description = "Test Description",
            severity = Severity.MEDIUM,
            status = IncidentStatus.Resolved,
            createdAt = testTimestamp,
            updatedAt = testTimestamp
        )

        // Act
        val dto = incident.toResponseDto()

        // Assert
        assertEquals(333L, dto.id)
        assertEquals("test-source", dto.source)
        assertEquals("Test Title", dto.title)
        assertEquals("Test Description", dto.description)
        assertEquals("MEDIUM", dto.severity)
        assertEquals("RESOLVED", dto.status)
        // Note: DTO doesn't include timestamps, which is expected
    }
}
