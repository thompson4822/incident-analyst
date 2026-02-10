package com.example.incidentanalyst.incident

import com.example.incidentanalyst.common.Either
import io.quarkus.test.InjectMock
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
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
    fun `getById returns Failure NotFound for non-existent incident`() {
        // Arrange
        val testId = 999L
        whenever(incidentRepository.findById(testId)).thenReturn(null)

        // Act
        val result = incidentService.getById(IncidentId(testId))

        // Assert
        assertTrue(result is Either.Left)
        val error = (result as Either.Left).value
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
        whenever(incidentRepository.findById(testId)).thenReturn(entity)

        // Act
        val result = incidentService.getById(IncidentId(testId))

        // Assert
        assertTrue(result is Either.Right)
        val incident = (result as Either.Right).value
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
        whenever(incidentRepository.findById(testId)).thenReturn(entity)

        // Act
        val result = incidentService.getById(IncidentId(testId))

        // Assert
        assertTrue(result is Either.Right)
        val incident = (result as Either.Right).value
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
        whenever(incidentRepository.findById(testId)).thenReturn(entity)

        // Act
        val result = incidentService.getById(IncidentId(testId))

        // Assert
        assertTrue(result is Either.Right)
        val incident = (result as Either.Right).value
        assertTrue(incident.status is IncidentStatus.Resolved)
    }

    @Test
    fun `Either Success pattern matching works correctly`() {
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
        whenever(incidentRepository.findById(testId)).thenReturn(entity)

        // Act
        val result = incidentService.getById(IncidentId(testId))

        // Assert
        when (result) {
            is Either.Right -> {
                val incident = result.value
                assertEquals("Test incident", incident.title)
                assertNotNull(incident.id)
            }
            is Either.Left -> {
                // Should not reach here
                org.junit.jupiter.api.Assertions.fail<Any>("Expected Right but got Left")
            }
        }
    }

    @Test
    fun `Either Left pattern matching works correctly`() {
        // Arrange
        val testId = 222L
        whenever(incidentRepository.findById(testId)).thenReturn(null)

        // Act
        val result = incidentService.getById(IncidentId(testId))

        // Assert
        when (result) {
            is Either.Right -> {
                // Should not reach here
                org.junit.jupiter.api.Assertions.fail<Any>("Expected Left but got Right")
            }
            is Either.Left -> {
                assertTrue(result.value is IncidentError.NotFound)
            }
        }
    }

    @Test
    fun `resolve updates status and resolutionText successfully`() {
        // Arrange
        val testId = 123L
        val testTimestamp = Instant.now()
        val entity = IncidentEntity(
            id = testId,
            source = "monitoring",
            title = "Test incident",
            description = "Description",
            severity = "HIGH",
            status = "OPEN",
            createdAt = testTimestamp,
            updatedAt = testTimestamp
        )
        whenever(incidentRepository.findById(testId)).thenReturn(entity)

        // Act
        val result = incidentService.resolve(IncidentId(testId), "Fixed it")

        // Assert
        assertTrue(result is Either.Right)
        val incident = (result as Either.Right).value
        assertTrue(incident.status is IncidentStatus.Resolved)
        assertEquals("Fixed it", incident.resolutionText)
        assertEquals("RESOLVED", entity.status)
        assertEquals("Fixed it", entity.resolutionText)
    }
}
