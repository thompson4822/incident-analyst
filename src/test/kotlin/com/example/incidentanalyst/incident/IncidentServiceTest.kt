package com.example.incidentanalyst.incident

import io.quarkus.test.InjectMock
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.anyLong
import org.mockito.Mockito.reset
import org.mockito.Mockito.`when`
import java.time.Instant

@QuarkusTest
class IncidentServiceTest {

    @InjectMock
    lateinit var incidentRepository: IncidentRepository

    @Inject
    lateinit var incidentService: IncidentService

    @BeforeEach
    fun setup() {
        reset(incidentRepository)
    }

    @Test
    fun `getById returns Success for valid incident`() {
        // Arrange
        val testId = 123L
        val testTimestamp = Instant.now()
        val entity = IncidentEntity(
            id = testId,
            source = "monitoring",
            title = "High CPU usage",
            description = "CPU usage exceeded 90%",
            severity = "HIGH",
            status = "OPEN",
            createdAt = testTimestamp,
            updatedAt = testTimestamp
        )
        `when`(incidentRepository.findById(testId)).thenReturn(entity)

        // Act
        val result = incidentService.getById(IncidentId(testId))

        // Assert
        assertTrue(result is IncidentResult.Success)
        val incident = (result as IncidentResult.Success).incident
        assertEquals(testId, incident.id.value)
        assertEquals("monitoring", incident.source)
        assertEquals("High CPU usage", incident.title)
        assertEquals("HIGH", incident.severity.name)
        assertTrue(incident.status is IncidentStatus.Open)
    }

    @Test
    fun `getById returns Failure NotFound for non-existent incident`() {
        // Arrange
        val testId = 999L
        `when`(incidentRepository.findById(testId)).thenReturn(null)

        // Act
        val result = incidentService.getById(IncidentId(testId))

        // Assert
        assertTrue(result is IncidentResult.Failure)
        val error = (result as IncidentResult.Failure).error
        assertTrue(error is IncidentError.NotFound)
    }

    @Test
    fun `getById Success contains diagnosed incident with correct diagnosisId`() {
        // Arrange
        val testId = 456L
        val diagnosisId = 789L
        val testTimestamp = Instant.now()
        val entity = IncidentEntity(
            id = testId,
            source = "alerting",
            title = "Database connection failed",
            description = "Connection pool exhausted",
            severity = "CRITICAL",
            status = "DIAGNOSED:$diagnosisId",
            createdAt = testTimestamp,
            updatedAt = testTimestamp
        )
        `when`(incidentRepository.findById(testId)).thenReturn(entity)

        // Act
        val result = incidentService.getById(IncidentId(testId))

        // Assert
        assertTrue(result is IncidentResult.Success)
        val incident = (result as IncidentResult.Success).incident
        assertTrue(incident.status is IncidentStatus.Diagnosed)
        val diagnosedStatus = incident.status as IncidentStatus.Diagnosed
        assertEquals(diagnosisId, diagnosedStatus.diagnosisId)
    }

    @Test
    fun `getById Success contains acknowledged incident`() {
        // Arrange
        val testId = 789L
        val testTimestamp = Instant.now()
        val entity = IncidentEntity(
            id = testId,
            source = "support",
            title = "User report issue",
            description = "Users reporting slow page loads",
            severity = "MEDIUM",
            status = "ACK",
            createdAt = testTimestamp,
            updatedAt = testTimestamp
        )
        `when`(incidentRepository.findById(testId)).thenReturn(entity)

        // Act
        val result = incidentService.getById(IncidentId(testId))

        // Assert
        assertTrue(result is IncidentResult.Success)
        val incident = (result as IncidentResult.Success).incident
        assertTrue(incident.status is IncidentStatus.Acknowledged)
    }

    @Test
    fun `getById Success contains resolved incident`() {
        // Arrange
        val testId = 321L
        val testTimestamp = Instant.now()
        val entity = IncidentEntity(
            id = testId,
            source = "monitoring",
            title = "Disk space warning",
            description = "Disk usage at 85%",
            severity = "LOW",
            status = "RESOLVED",
            createdAt = testTimestamp,
            updatedAt = testTimestamp
        )
        `when`(incidentRepository.findById(testId)).thenReturn(entity)

        // Act
        val result = incidentService.getById(IncidentId(testId))

        // Assert
        assertTrue(result is IncidentResult.Success)
        val incident = (result as IncidentResult.Success).incident
        assertTrue(incident.status is IncidentStatus.Resolved)
    }

    @Test
    fun `IncidentResult Success pattern matching works correctly`() {
        // Arrange
        val testId = 111L
        val testTimestamp = Instant.now()
        val entity = IncidentEntity(
            id = testId,
            source = "test",
            title = "Test incident",
            description = "Test description",
            severity = "INFO",
            status = "OPEN",
            createdAt = testTimestamp,
            updatedAt = testTimestamp
        )
        `when`(incidentRepository.findById(testId)).thenReturn(entity)

        // Act
        val result = incidentService.getById(IncidentId(testId))

        // Assert
        when (result) {
            is IncidentResult.Success -> {
                val incident = result.incident
                assertEquals("Test incident", incident.title)
                assertNotNull(incident.id)
            }
            is IncidentResult.Failure -> {
                // Should not reach here
                org.junit.jupiter.api.Assertions.fail<Any>("Expected Success but got Failure")
            }
        }
    }

    @Test
    fun `IncidentResult Failure pattern matching works correctly`() {
        // Arrange
        val testId = 222L
        `when`(incidentRepository.findById(testId)).thenReturn(null)

        // Act
        val result = incidentService.getById(IncidentId(testId))

        // Assert
        when (result) {
            is IncidentResult.Success -> {
                // Should not reach here
                org.junit.jupiter.api.Assertions.fail<Any>("Expected Failure but got Success")
            }
            is IncidentResult.Failure -> {
                assertTrue(result.error is IncidentError.NotFound)
            }
        }
    }
}
