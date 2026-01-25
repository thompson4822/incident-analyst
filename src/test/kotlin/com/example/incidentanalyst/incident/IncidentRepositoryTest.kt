package com.example.incidentanalyst.incident

import io.quarkus.test.TestTransaction
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

@QuarkusTest
class IncidentRepositoryTest {

    @Inject
    lateinit var incidentRepository: IncidentRepository

    @Test
    @TestTransaction
    fun `toDomain maps OPEN status correctly`() {
        // Arrange
        val entity = IncidentEntity(
            source = "monitoring",
            title = "Test incident",
            description = "Test description",
            severity = "HIGH",
            status = "OPEN",
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        incidentRepository.persist(entity)
        assertNotNull(entity.id)

        // Act
        val foundEntity = incidentRepository.findById(entity.id!!)

        // Assert
        assertNotNull(foundEntity)
        val domain = foundEntity!!.toDomain()
        assertEquals(entity.id!!.toLong(), domain.id.value)
        assertTrue(domain.status is IncidentStatus.Open)
        assertEquals("Test incident", domain.title)
        assertEquals("HIGH", domain.severity.name)
    }

    @Test
    @TestTransaction
    fun `toDomain maps ACK status to Acknowledged`() {
        // Arrange
        val entity = IncidentEntity(
            source = "alerting",
            title = "Acknowledged incident",
            description = "Description",
            severity = "MEDIUM",
            status = "ACK",
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        incidentRepository.persist(entity)

        // Act
        val foundEntity = incidentRepository.findById(entity.id!!)
        val domain = foundEntity!!.toDomain()

        // Assert
        assertTrue(domain.status is IncidentStatus.Acknowledged)
    }

    @Test
    @TestTransaction
    fun `toDomain maps RESOLVED status to Resolved`() {
        // Arrange
        val entity = IncidentEntity(
            source = "support",
            title = "Resolved incident",
            description = "Description",
            severity = "LOW",
            status = "RESOLVED",
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        incidentRepository.persist(entity)

        // Act
        val foundEntity = incidentRepository.findById(entity.id!!)
        val domain = foundEntity!!.toDomain()

        // Assert
        assertTrue(domain.status is IncidentStatus.Resolved)
    }

    @Test
    @TestTransaction
    fun `toDomain parses DIAGNOSED with correct diagnosisId`() {
        // Arrange
        val diagnosisId = 456L
        val entity = IncidentEntity(
            source = "monitoring",
            title = "Diagnosed incident",
            description = "Description",
            severity = "CRITICAL",
            status = "DIAGNOSED:$diagnosisId",
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        incidentRepository.persist(entity)

        // Act
        val foundEntity = incidentRepository.findById(entity.id!!)
        val domain = foundEntity!!.toDomain()

        // Assert
        assertTrue(domain.status is IncidentStatus.Diagnosed)
        val diagnosedStatus = domain.status as IncidentStatus.Diagnosed
        assertEquals(diagnosisId, diagnosedStatus.diagnosisId)
    }

    @Test
    @TestTransaction
    fun `toDomain handles DIAGNOSED with large diagnosisId`() {
        // Arrange
        val diagnosisId = 999999999L
        val entity = IncidentEntity(
            source = "test",
            title = "Large diagnosisId",
            description = "Description",
            severity = "HIGH",
            status = "DIAGNOSED:$diagnosisId",
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        incidentRepository.persist(entity)

        // Act
        val foundEntity = incidentRepository.findById(entity.id!!)
        val domain = foundEntity!!.toDomain()

        // Assert
        assertTrue(domain.status is IncidentStatus.Diagnosed)
        val diagnosedStatus = domain.status as IncidentStatus.Diagnosed
        assertEquals(diagnosisId, diagnosedStatus.diagnosisId)
    }

    @Test
    @TestTransaction
    fun `toDomain handles DIAGNOSED with zero diagnosisId`() {
        // Arrange
        val diagnosisId = 0L
        val entity = IncidentEntity(
            source = "test",
            title = "Zero diagnosisId",
            description = "Description",
            severity = "MEDIUM",
            status = "DIAGNOSED:$diagnosisId",
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        incidentRepository.persist(entity)

        // Act
        val foundEntity = incidentRepository.findById(entity.id!!)
        val domain = foundEntity!!.toDomain()

        // Assert
        assertTrue(domain.status is IncidentStatus.Diagnosed)
        val diagnosedStatus = domain.status as IncidentStatus.Diagnosed
        assertEquals(diagnosisId, diagnosedStatus.diagnosisId)
    }

    @Test
    @TestTransaction
    fun `toDomain handles invalid DIAGNOSED format gracefully`() {
        // Arrange
        val entity = IncidentEntity(
            source = "test",
            title = "Invalid diagnosed format",
            description = "Description",
            severity = "INFO",
            status = "DIAGNOSED:not-a-number",
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        incidentRepository.persist(entity)

        // Act
        val foundEntity = incidentRepository.findById(entity.id!!)
        val domain = foundEntity!!.toDomain()

        // Assert - Invalid format should coerce to Open as per implementation
        assertTrue(domain.status is IncidentStatus.Open)
    }

    @Test
    @TestTransaction
    fun `toDomain handles DIAGNOSED with empty value`() {
        // Arrange
        val entity = IncidentEntity(
            source = "test",
            title = "Empty diagnosed value",
            description = "Description",
            severity = "LOW",
            status = "DIAGNOSED:",
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        incidentRepository.persist(entity)

        // Act
        val foundEntity = incidentRepository.findById(entity.id!!)
        val domain = foundEntity!!.toDomain()

        // Assert - Empty value should coerce to Open as per implementation
        assertTrue(domain.status is IncidentStatus.Open)
    }

    @Test
    @TestTransaction
    fun `toDomain handles unknown status strings gracefully`() {
        // Arrange
        val entity = IncidentEntity(
            source = "test",
            title = "Unknown status",
            description = "Description",
            severity = "HIGH",
            status = "UNKNOWN_STATUS",
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        incidentRepository.persist(entity)

        // Act
        val foundEntity = incidentRepository.findById(entity.id!!)
        val domain = foundEntity!!.toDomain()

        // Assert - Unknown status should coerce to Open as per implementation
        assertTrue(domain.status is IncidentStatus.Open)
    }

    @Test
    @TestTransaction
    fun `toDomain handles empty status string gracefully`() {
        // Arrange
        val entity = IncidentEntity(
            source = "test",
            title = "Empty status",
            description = "Description",
            severity = "MEDIUM",
            status = "",
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        incidentRepository.persist(entity)

        // Act
        val foundEntity = incidentRepository.findById(entity.id!!)
        val domain = foundEntity!!.toDomain()

        // Assert - Empty status should coerce to Open as per implementation
        assertTrue(domain.status is IncidentStatus.Open)
    }

    @Test
    @TestTransaction
    fun `toDomain maps all severity levels correctly`() {
        // Arrange
        val severities = listOf("CRITICAL", "HIGH", "MEDIUM", "LOW", "INFO")
        severities.forEachIndexed { index, severity ->
            val entity = IncidentEntity(
                source = "test",
                title = "Severity test $index",
                description = "Description",
                severity = severity,
                status = "OPEN",
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            )
            incidentRepository.persist(entity)

            // Act
            val foundEntity = incidentRepository.findById(entity.id!!)
            val domain = foundEntity!!.toDomain()

            // Assert
            assertEquals(severity, domain.severity.name)
        }
    }

    @Test
    @TestTransaction
    fun `toDomain preserves timestamp fields`() {
        // Arrange
        val createdTime = Instant.now().minusSeconds(3600)
        val updatedTime = Instant.now()
        val entity = IncidentEntity(
            source = "test",
            title = "Timestamp test",
            description = "Description",
            severity = "HIGH",
            status = "OPEN",
            createdAt = createdTime,
            updatedAt = updatedTime
        )
        incidentRepository.persist(entity)

        // Act
        val foundEntity = incidentRepository.findById(entity.id!!)
        val domain = foundEntity!!.toDomain()

        // Assert
        assertEquals(createdTime, domain.createdAt)
        assertEquals(updatedTime, domain.updatedAt)
    }

    @Test
    @TestTransaction
    fun `findById returns null for non-existent entity`() {
        // Act
        val foundEntity = incidentRepository.findById(999999L)

        // Assert
        assertTrue(foundEntity == null)
    }
}
