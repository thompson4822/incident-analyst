package com.example.incidentanalyst.incident

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import java.time.Instant

class IncidentModelsTest {

    @Test
    fun `toDomain maps OPEN status correctly`() {
        // Arrange
        val entity = IncidentEntity(
            id = 123L,
            source = "monitoring",
            title = "Test incident",
            description = "Test description",
            severity = "HIGH",
            status = "OPEN",
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        // Act
        val domain = entity.toDomain()

        // Assert
        assertTrue(domain.status is IncidentStatus.Open)
        assertEquals(123L, domain.id.value)
        assertEquals("monitoring", domain.source)
        assertEquals("Test incident", domain.title)
        assertEquals(Severity.HIGH, domain.severity)
    }

    @Test
    fun `toDomain maps ACK status to Acknowledged`() {
        // Arrange
        val entity = IncidentEntity(
            id = 456L,
            source = "alerting",
            title = "Acknowledged incident",
            description = "Description",
            severity = "MEDIUM",
            status = "ACK",
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        // Act
        val domain = entity.toDomain()

        // Assert
        assertTrue(domain.status is IncidentStatus.Acknowledged)
    }

    @Test
    fun `toDomain maps RESOLVED status to Resolved`() {
        // Arrange
        val entity = IncidentEntity(
            id = 789L,
            source = "support",
            title = "Resolved incident",
            description = "Description",
            severity = "LOW",
            status = "RESOLVED",
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        // Act
        val domain = entity.toDomain()

        // Assert
        assertTrue(domain.status is IncidentStatus.Resolved)
    }

    @Test
    fun `toDomain parses DIAGNOSED with diagnosisId`() {
        // Arrange
        val diagnosisId = 456L
        val entity = IncidentEntity(
            id = 321L,
            source = "monitoring",
            title = "Diagnosed incident",
            description = "Description",
            severity = "CRITICAL",
            status = "DIAGNOSED:$diagnosisId",
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        // Act
        val domain = entity.toDomain()

        // Assert
        assertTrue(domain.status is IncidentStatus.Diagnosed)
        val diagnosedStatus = domain.status as IncidentStatus.Diagnosed
        assertEquals(diagnosisId, diagnosedStatus.diagnosisId)
    }

    @Test
    fun `toDomain handles DIAGNOSED with very large diagnosisId`() {
        // Arrange
        val diagnosisId = 999999999L
        val entity = IncidentEntity(
            id = 111L,
            source = "test",
            title = "Large diagnosisId",
            description = "Description",
            severity = "HIGH",
            status = "DIAGNOSED:$diagnosisId",
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        // Act
        val domain = entity.toDomain()

        // Assert
        assertTrue(domain.status is IncidentStatus.Diagnosed)
        val diagnosedStatus = domain.status as IncidentStatus.Diagnosed
        assertEquals(diagnosisId, diagnosedStatus.diagnosisId)
    }

    @Test
    fun `toDomain handles DIAGNOSED with zero diagnosisId`() {
        // Arrange
        val diagnosisId = 0L
        val entity = IncidentEntity(
            id = 222L,
            source = "test",
            title = "Zero diagnosisId",
            description = "Description",
            severity = "MEDIUM",
            status = "DIAGNOSED:$diagnosisId",
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        // Act
        val domain = entity.toDomain()

        // Assert
        assertTrue(domain.status is IncidentStatus.Diagnosed)
        val diagnosedStatus = domain.status as IncidentStatus.Diagnosed
        assertEquals(diagnosisId, diagnosedStatus.diagnosisId)
    }

    @Test
    fun `toDomain handles DIAGNOSED with negative diagnosisId`() {
        // Arrange
        val diagnosisId = -1L
        val entity = IncidentEntity(
            id = 333L,
            source = "test",
            title = "Negative diagnosisId",
            description = "Description",
            severity = "LOW",
            status = "DIAGNOSED:$diagnosisId",
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        // Act
        val domain = entity.toDomain()

        // Assert
        assertTrue(domain.status is IncidentStatus.Diagnosed)
        val diagnosedStatus = domain.status as IncidentStatus.Diagnosed
        assertEquals(diagnosisId, diagnosedStatus.diagnosisId)
    }

    @Test
    fun `toDomain handles invalid DIAGNOSED format by coercing to Open`() {
        // Arrange
        val entity = IncidentEntity(
            id = 444L,
            source = "test",
            title = "Invalid diagnosed format",
            description = "Description",
            severity = "INFO",
            status = "DIAGNOSED:not-a-number",
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        // Act
        val domain = entity.toDomain()

        // Assert - Invalid format should coerce to Open
        assertTrue(domain.status is IncidentStatus.Open)
    }

    @Test
    fun `toDomain handles DIAGNOSED with empty value by coercing to Open`() {
        // Arrange
        val entity = IncidentEntity(
            id = 555L,
            source = "test",
            title = "Empty diagnosed value",
            description = "Description",
            severity = "LOW",
            status = "DIAGNOSED:",
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        // Act
        val domain = entity.toDomain()

        // Assert - Empty value should coerce to Open
        assertTrue(domain.status is IncidentStatus.Open)
    }

    @Test
    fun `toDomain handles unknown status strings by coercing to Open`() {
        // Arrange
        val entity = IncidentEntity(
            id = 666L,
            source = "test",
            title = "Unknown status",
            description = "Description",
            severity = "HIGH",
            status = "UNKNOWN_STATUS",
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        // Act
        val domain = entity.toDomain()

        // Assert - Unknown status should coerce to Open
        assertTrue(domain.status is IncidentStatus.Open)
    }

    @Test
    fun `toDomain handles empty status string by coercing to Open`() {
        // Arrange
        val entity = IncidentEntity(
            id = 777L,
            source = "test",
            title = "Empty status",
            description = "Description",
            severity = "MEDIUM",
            status = "",
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        // Act
        val domain = entity.toDomain()

        // Assert - Empty status should coerce to Open
        assertTrue(domain.status is IncidentStatus.Open)
    }

    @Test
    fun `toDomain maps all severity levels correctly`() {
        // Arrange
        val severities = mapOf(
            "CRITICAL" to Severity.CRITICAL,
            "HIGH" to Severity.HIGH,
            "MEDIUM" to Severity.MEDIUM,
            "LOW" to Severity.LOW,
            "INFO" to Severity.INFO
        )

        severities.forEach { (severityString, severityEnum) ->
            val entity = IncidentEntity(
                id = 1000L + severityEnum.ordinal.toLong(),
                source = "test",
                title = "Severity test",
                description = "Description",
                severity = severityString,
                status = "OPEN",
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            )

            // Act
            val domain = entity.toDomain()

            // Assert
            assertEquals(severityEnum, domain.severity)
        }
    }

    @Test
    fun `toDomain preserves timestamp fields`() {
        // Arrange
        val createdTime = Instant.now().minusSeconds(3600)
        val updatedTime = Instant.now()
        val entity = IncidentEntity(
            id = 888L,
            source = "test",
            title = "Timestamp test",
            description = "Description",
            severity = "HIGH",
            status = "OPEN",
            createdAt = createdTime,
            updatedAt = updatedTime
        )

        // Act
        val domain = entity.toDomain()

        // Assert
        assertEquals(createdTime, domain.createdAt)
        assertEquals(updatedTime, domain.updatedAt)
    }

    @Test
    fun `toDomain maps all entity fields correctly`() {
        // Arrange
        val createdTime = Instant.now().minusSeconds(7200)
        val updatedTime = Instant.now()
        val entity = IncidentEntity(
            id = 999L,
            source = "custom-source",
            title = "Custom Title",
            description = "Custom Description",
            severity = "HIGH",
            status = "OPEN",
            createdAt = createdTime,
            updatedAt = updatedTime
        )

        // Act
        val domain = entity.toDomain()

        // Assert
        assertEquals(999L, domain.id.value)
        assertEquals("custom-source", domain.source)
        assertEquals("Custom Title", domain.title)
        assertEquals("Custom Description", domain.description)
        assertEquals(Severity.HIGH, domain.severity)
        assertTrue(domain.status is IncidentStatus.Open)
        assertEquals(createdTime, domain.createdAt)
        assertEquals(updatedTime, domain.updatedAt)
    }

    @Test
    fun `toDomain handles case-insensitive status strings`() {
        // Arrange
        val statuses = listOf("open", "Open", "OPEN", "ack", "Ack", "ACK", "resolved", "Resolved", "RESOLVED")
        statuses.forEach { status ->
            val entity = IncidentEntity(
                id = System.currentTimeMillis() % 10000,
                source = "test",
                title = "Case test",
                description = "Description",
                severity = "MEDIUM",
                status = status,
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            )

            // Act
            val domain = entity.toDomain()

            // Assert - Case-sensitive matching means these might coerce to Open
            // This test documents current behavior
            val expectedStatus = when (status.uppercase()) {
                "OPEN" -> IncidentStatus.Open::class
                "ACK" -> IncidentStatus.Acknowledged::class
                "RESOLVED" -> IncidentStatus.Resolved::class
                else -> IncidentStatus.Open::class
            }
            assertTrue(expectedStatus.isInstance(domain.status),
                "Status '$status' should map to ${expectedStatus.simpleName}, but got ${domain.status::class.simpleName}")
        }
    }

    @Test
    fun `IncidentId value class wraps Long value`() {
        // Arrange & Act
        val id = IncidentId(123L)

        // Assert
        assertEquals(123L, id.value)
    }

    @Test
    fun `IncidentStatus ADT has correct subtypes`() {
        // Arrange & Act
        val openStatus: IncidentStatus = IncidentStatus.Open
        val ackStatus: IncidentStatus = IncidentStatus.Acknowledged
        val diagnosedStatus: IncidentStatus = IncidentStatus.Diagnosed(123)
        val resolvedStatus: IncidentStatus = IncidentStatus.Resolved

        // Assert
        assertTrue(openStatus is IncidentStatus.Open)
        assertTrue(ackStatus is IncidentStatus.Acknowledged)
        assertTrue(diagnosedStatus is IncidentStatus.Diagnosed)
        assertEquals(123L, (diagnosedStatus as IncidentStatus.Diagnosed).diagnosisId)
        assertTrue(resolvedStatus is IncidentStatus.Resolved)
    }

    @Test
    fun `IncidentError ADT has NotFound variant`() {
        // Arrange & Act
        val notFound: IncidentError = IncidentError.NotFound

        // Assert
        assertTrue(notFound is IncidentError.NotFound)
    }

    @Test
    fun `IncidentResult ADT has Success and Failure variants`() {
        // Arrange
        val incident = Incident(
            id = IncidentId(1L),
            source = "test",
            title = "Test",
            description = "Description",
            severity = Severity.HIGH,
            status = IncidentStatus.Open,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        val successResult: IncidentResult = IncidentResult.Success(incident)
        val failureResult: IncidentResult = IncidentResult.Failure(IncidentError.NotFound)

        // Assert
        assertTrue(successResult is IncidentResult.Success)
        assertEquals(incident, (successResult as IncidentResult.Success).incident)
        assertTrue(failureResult is IncidentResult.Failure)
        assertTrue((failureResult as IncidentResult.Failure).error is IncidentError.NotFound)
    }

    @Test
    fun `pattern matching on IncidentResult works correctly`() {
        // Arrange
        val incident = Incident(
            id = IncidentId(1L),
            source = "test",
            title = "Test",
            description = "Description",
            severity = Severity.HIGH,
            status = IncidentStatus.Open,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        // Act & Assert
        val successResult: IncidentResult = IncidentResult.Success(incident)
        var successHandled = false
        var failureHandled = false

        when (successResult) {
            is IncidentResult.Success -> successHandled = true
            is IncidentResult.Failure -> failureHandled = true
        }

        assertTrue(successHandled)
        assertFalse(failureHandled)

        val failureResult: IncidentResult = IncidentResult.Failure(IncidentError.NotFound)
        successHandled = false
        failureHandled = false

        when (failureResult) {
            is IncidentResult.Success -> successHandled = true
            is IncidentResult.Failure -> failureHandled = true
        }

        assertFalse(successHandled)
        assertTrue(failureHandled)
    }

    @Test
    fun `toEntity maps Open status to OPEN`() {
        // Arrange
        val incident = Incident(
            id = IncidentId(123L),
            source = "monitoring",
            title = "Test incident",
            description = "Test description",
            severity = Severity.HIGH,
            status = IncidentStatus.Open,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        // Act
        val entity = incident.toEntity()

        // Assert
        assertEquals(123L, entity.id)
        assertEquals("monitoring", entity.source)
        assertEquals("Test incident", entity.title)
        assertEquals("Test description", entity.description)
        assertEquals("HIGH", entity.severity)
        assertEquals("OPEN", entity.status)
    }

    @Test
    fun `toEntity maps Acknowledged status to ACK`() {
        // Arrange
        val incident = Incident(
            id = IncidentId(456L),
            source = "alerting",
            title = "Acknowledged incident",
            description = "Description",
            severity = Severity.MEDIUM,
            status = IncidentStatus.Acknowledged,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        // Act
        val entity = incident.toEntity()

        // Assert
        assertEquals("ACK", entity.status)
    }

    @Test
    fun `toEntity maps Resolved status to RESOLVED`() {
        // Arrange
        val incident = Incident(
            id = IncidentId(789L),
            source = "support",
            title = "Resolved incident",
            description = "Description",
            severity = Severity.LOW,
            status = IncidentStatus.Resolved,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        // Act
        val entity = incident.toEntity()

        // Assert
        assertEquals("RESOLVED", entity.status)
    }

    @Test
    fun `toEntity maps Diagnosed status to DIAGNOSED with diagnosisId`() {
        // Arrange
        val diagnosisId = 456L
        val incident = Incident(
            id = IncidentId(321L),
            source = "monitoring",
            title = "Diagnosed incident",
            description = "Description",
            severity = Severity.CRITICAL,
            status = IncidentStatus.Diagnosed(diagnosisId),
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        // Act
        val entity = incident.toEntity()

        // Assert
        assertEquals("DIAGNOSED:$diagnosisId", entity.status)
    }

    @Test
    fun `toEntity preserves all severity levels`() {
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
                source = "test",
                title = "Severity test",
                description = "Description",
                severity = severityEnum,
                status = IncidentStatus.Open,
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            )

            // Act
            val entity = incident.toEntity()

            // Assert
            assertEquals(severityString, entity.severity)
        }
    }

    @Test
    fun `toEntity preserves timestamp fields`() {
        // Arrange
        val createdTime = Instant.now().minusSeconds(3600)
        val updatedTime = Instant.now()
        val incident = Incident(
            id = IncidentId(888L),
            source = "test",
            title = "Timestamp test",
            description = "Description",
            severity = Severity.HIGH,
            status = IncidentStatus.Open,
            createdAt = createdTime,
            updatedAt = updatedTime
        )

        // Act
        val entity = incident.toEntity()

        // Assert
        assertEquals(createdTime, entity.createdAt)
        assertEquals(updatedTime, entity.updatedAt)
    }

    @Test
    fun `toEntity maps all incident fields correctly`() {
        // Arrange
        val createdTime = Instant.now().minusSeconds(7200)
        val updatedTime = Instant.now()
        val incident = Incident(
            id = IncidentId(999L),
            source = "custom-source",
            title = "Custom Title",
            description = "Custom Description",
            severity = Severity.HIGH,
            status = IncidentStatus.Open,
            createdAt = createdTime,
            updatedAt = updatedTime
        )

        // Act
        val entity = incident.toEntity()

        // Assert
        assertEquals(999L, entity.id)
        assertEquals("custom-source", entity.source)
        assertEquals("Custom Title", entity.title)
        assertEquals("Custom Description", entity.description)
        assertEquals("HIGH", entity.severity)
        assertEquals("OPEN", entity.status)
        assertEquals(createdTime, entity.createdAt)
        assertEquals(updatedTime, entity.updatedAt)
    }

    private fun assertFalse(condition: Boolean) {
        assertEquals(false, condition)
    }
}
