package com.example.incidentanalyst.diagnosis

import io.quarkus.test.TestTransaction
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import com.example.incidentanalyst.incident.IncidentEntity
import com.example.incidentanalyst.incident.IncidentId
import java.time.Instant

@QuarkusTest
class DiagnosisRepositoryTest {

    @Inject
    lateinit var diagnosisRepository: DiagnosisRepository

    @Test
    @TestTransaction
    fun `findRecent returns diagnoses ordered by createdAt desc`() {
        // Arrange
        val baseTime = Instant.now()
        val incidentEntity = IncidentEntity(
            source = "monitoring",
            title = "Test incident",
            description = "Description",
            severity = "HIGH",
            status = "OPEN",
            createdAt = baseTime,
            updatedAt = baseTime
        )
        incidentEntity.persist()
        assertNotNull(incidentEntity.id)

        val oldDiagnosis = DiagnosisEntity(
            incident = incidentEntity,
            suggestedRootCause = "Old root cause",
            remediationSteps = "Old step 1",
            confidence = "HIGH",
            verification = "UNVERIFIED",
            createdAt = baseTime.minusSeconds(3600)
        )
        diagnosisRepository.persist(oldDiagnosis)
        assertNotNull(oldDiagnosis.id)

        val newDiagnosis = DiagnosisEntity(
            incident = incidentEntity,
            suggestedRootCause = "New root cause",
            remediationSteps = "New step 1",
            confidence = "MEDIUM",
            verification = "VERIFIED",
            createdAt = baseTime
        )
        diagnosisRepository.persist(newDiagnosis)
        assertNotNull(newDiagnosis.id)

        // Act
        val diagnoses = diagnosisRepository.findRecent(50)

        // Assert
        assertEquals(2, diagnoses.size)
        assertEquals(newDiagnosis.id, diagnoses[0].id)
        assertEquals(oldDiagnosis.id, diagnoses[1].id)
    }

    @Test
    @TestTransaction
    fun `findRecent respects limit parameter`() {
        // Arrange
        val baseTime = Instant.now()
        val incidentEntity = IncidentEntity(
            source = "monitoring",
            title = "Test incident",
            description = "Description",
            severity = "HIGH",
            status = "OPEN",
            createdAt = baseTime,
            updatedAt = baseTime
        )
        incidentEntity.persist()

        val diagnoses = (1..10).map { i ->
            DiagnosisEntity(
                incident = incidentEntity,
                suggestedRootCause = "Root cause $i",
                remediationSteps = "Step $i",
                confidence = "HIGH",
                verification = "UNVERIFIED",
                createdAt = baseTime.minusSeconds((10 - i).toLong())
            )
        }

        diagnoses.forEach { diagnosisRepository.persist(it) }

        // Act
        val result = diagnosisRepository.findRecent(5)

        // Assert
        assertEquals(5, result.size)
    }

    @Test
    @TestTransaction
    fun `findRecent returns empty list when no diagnoses exist`() {
        // Act
        val result = diagnosisRepository.findRecent(50)

        // Assert
        assertEquals(0, result.size)
    }

    @Test
    @TestTransaction
    fun `findById returns entity for existing ID`() {
        // Arrange
        val baseTime = Instant.now()
        val incidentEntity = IncidentEntity(
            source = "monitoring",
            title = "Test incident",
            description = "Description",
            severity = "HIGH",
            status = "OPEN",
            createdAt = baseTime,
            updatedAt = baseTime
        )
        incidentEntity.persist()

        val diagnosisEntity = DiagnosisEntity(
            incident = incidentEntity,
            suggestedRootCause = "Root cause",
            remediationSteps = "Step 1\nStep 2",
            confidence = "HIGH",
            verification = "VERIFIED",
            createdAt = baseTime
        )
        diagnosisRepository.persist(diagnosisEntity)
        assertNotNull(diagnosisEntity.id)

        // Act
        val found = diagnosisRepository.findById(diagnosisEntity.id!!)

        // Assert
        assertNotNull(found)
        assertEquals(diagnosisEntity.id, found!!.id)
        assertEquals("Root cause", found.suggestedRootCause)
    }

    @Test
    @TestTransaction
    fun `findById returns null for non-existent ID`() {
        // Act
        val found = diagnosisRepository.findById(999999L)

        // Assert
        assertTrue(found == null)
    }

    @Test
    @TestTransaction
    fun `toDomain maps all entity fields correctly`() {
        // Arrange
        val baseTime = Instant.now()
        val createdTime = baseTime.minusSeconds(7200)
        val incidentEntity = IncidentEntity(
            id = 123L,
            source = "monitoring",
            title = "Test incident",
            description = "Description",
            severity = "HIGH",
            status = "OPEN",
            createdAt = baseTime,
            updatedAt = baseTime
        )

        val diagnosisEntity = DiagnosisEntity(
            id = 456L,
            incident = incidentEntity,
            suggestedRootCause = "Custom root cause",
            remediationSteps = "Custom step 1\nCustom step 2\nCustom step 3",
            confidence = "MEDIUM",
            verification = "VERIFIED",
            createdAt = createdTime
        )

        // Act
        val domain = diagnosisEntity.toDomain()

        // Assert
        assertEquals(456L, domain.id.value)
        assertEquals(123L, domain.incidentId.value)
        assertEquals("Custom root cause", domain.rootCause)
        assertEquals(3, domain.steps.size)
        assertEquals(Confidence.MEDIUM, domain.confidence)
        assertTrue(domain.verification is DiagnosisVerification.VerifiedByHuman)
        assertEquals(createdTime, domain.createdAt)
    }

    @Test
    @TestTransaction
    fun `toDomain maps VERIFIED to VerifiedByHuman`() {
        // Arrange
        val baseTime = Instant.now()
        val incidentEntity = IncidentEntity(
            id = 1L,
            source = "monitoring",
            title = "Test incident",
            description = "Description",
            severity = "HIGH",
            status = "OPEN",
            createdAt = baseTime,
            updatedAt = baseTime
        )

        val diagnosisEntity = DiagnosisEntity(
            id = 1L,
            incident = incidentEntity,
            suggestedRootCause = "Root cause",
            remediationSteps = "Step 1",
            confidence = "HIGH",
            verification = "VERIFIED",
            createdAt = baseTime
        )

        // Act
        val domain = diagnosisEntity.toDomain()

        // Assert
        assertTrue(domain.verification is DiagnosisVerification.VerifiedByHuman)
    }

    @Test
    @TestTransaction
    fun `toDomain maps UNVERIFIED to Unverified`() {
        // Arrange
        val baseTime = Instant.now()
        val incidentEntity = IncidentEntity(
            id = 1L,
            source = "monitoring",
            title = "Test incident",
            description = "Description",
            severity = "HIGH",
            status = "OPEN",
            createdAt = baseTime,
            updatedAt = baseTime
        )

        val diagnosisEntity = DiagnosisEntity(
            id = 1L,
            incident = incidentEntity,
            suggestedRootCause = "Root cause",
            remediationSteps = "Step 1",
            confidence = "HIGH",
            verification = "UNVERIFIED",
            createdAt = baseTime
        )

        // Act
        val domain = diagnosisEntity.toDomain()

        // Assert
        assertTrue(domain.verification is DiagnosisVerification.Unverified)
    }

    @Test
    @TestTransaction
    fun `toDomain handles unknown verification strings by coercing to Unverified`() {
        // Arrange
        val baseTime = Instant.now()
        val incidentEntity = IncidentEntity(
            id = 1L,
            source = "monitoring",
            title = "Test incident",
            description = "Description",
            severity = "HIGH",
            status = "OPEN",
            createdAt = baseTime,
            updatedAt = baseTime
        )

        val diagnosisEntity = DiagnosisEntity(
            id = 1L,
            incident = incidentEntity,
            suggestedRootCause = "Root cause",
            remediationSteps = "Step 1",
            confidence = "HIGH",
            verification = "UNKNOWN_STATUS",
            createdAt = baseTime
        )

        // Act
        val domain = diagnosisEntity.toDomain()

        // Assert - Unknown verification should coerce to Unverified as per implementation
        assertTrue(domain.verification is DiagnosisVerification.Unverified)
    }

    @Test
    @TestTransaction
    fun `toDomain maps all confidence levels correctly`() {
        // Arrange
        val baseTime = Instant.now()
        val incidentEntity = IncidentEntity(
            id = 1L,
            source = "monitoring",
            title = "Test incident",
            description = "Description",
            severity = "HIGH",
            status = "OPEN",
            createdAt = baseTime,
            updatedAt = baseTime
        )

        val confidences = listOf("HIGH", "MEDIUM", "LOW", "UNKNOWN")
        confidences.forEachIndexed { index, confidence ->
            val diagnosisEntity = DiagnosisEntity(
                id = (index + 1).toLong(),
                incident = incidentEntity,
                suggestedRootCause = "Root cause $index",
                remediationSteps = "Step $index",
                confidence = confidence,
                verification = "UNVERIFIED",
                createdAt = baseTime
            )

            // Act
            val domain = diagnosisEntity.toDomain()

            // Assert
            assertEquals(confidence, domain.confidence.name)
        }
    }

    @Test
    @TestTransaction
    fun `toDomain splits steps by newlines correctly`() {
        // Arrange
        val baseTime = Instant.now()
        val incidentEntity = IncidentEntity(
            id = 1L,
            source = "monitoring",
            title = "Test incident",
            description = "Description",
            severity = "HIGH",
            status = "OPEN",
            createdAt = baseTime,
            updatedAt = baseTime
        )

        val diagnosisEntity = DiagnosisEntity(
            id = 1L,
            incident = incidentEntity,
            suggestedRootCause = "Root cause",
            remediationSteps = "Step 1: Check logs\nStep 2: Identify issue\nStep 3: Fix bug\n\nStep 4: Verify",
            confidence = "HIGH",
            verification = "UNVERIFIED",
            createdAt = baseTime
        )

        // Act
        val domain = diagnosisEntity.toDomain()

        // Assert - Blank lines are filtered out
        assertEquals(4, domain.steps.size)
        assertEquals("Step 1: Check logs", domain.steps[0])
        assertEquals("Step 2: Identify issue", domain.steps[1])
        assertEquals("Step 3: Fix bug", domain.steps[2])
        assertEquals("Step 4: Verify", domain.steps[3])
    }

    @Test
    @TestTransaction
    fun `toDomain handles empty remediation steps`() {
        // Arrange
        val baseTime = Instant.now()
        val incidentEntity = IncidentEntity(
            id = 1L,
            source = "monitoring",
            title = "Test incident",
            description = "Description",
            severity = "HIGH",
            status = "OPEN",
            createdAt = baseTime,
            updatedAt = baseTime
        )

        val diagnosisEntity = DiagnosisEntity(
            id = 1L,
            incident = incidentEntity,
            suggestedRootCause = "Root cause",
            remediationSteps = "",
            confidence = "HIGH",
            verification = "UNVERIFIED",
            createdAt = baseTime
        )

        // Act
        val domain = diagnosisEntity.toDomain()

        // Assert
        assertEquals(0, domain.steps.size)
    }

    @Test
    @TestTransaction
    fun `toDomain handles single step`() {
        // Arrange
        val baseTime = Instant.now()
        val incidentEntity = IncidentEntity(
            id = 1L,
            source = "monitoring",
            title = "Test incident",
            description = "Description",
            severity = "HIGH",
            status = "OPEN",
            createdAt = baseTime,
            updatedAt = baseTime
        )

        val diagnosisEntity = DiagnosisEntity(
            id = 1L,
            incident = incidentEntity,
            suggestedRootCause = "Root cause",
            remediationSteps = "Single step",
            confidence = "HIGH",
            verification = "UNVERIFIED",
            createdAt = baseTime
        )

        // Act
        val domain = diagnosisEntity.toDomain()

        // Assert
        assertEquals(1, domain.steps.size)
        assertEquals("Single step", domain.steps[0])
    }

    @Test
    @TestTransaction
    fun `toDomain preserves timestamp fields`() {
        // Arrange
        val createdTime = Instant.now().minusSeconds(3600)
        val baseTime = Instant.now()
        val incidentEntity = IncidentEntity(
            id = 1L,
            source = "monitoring",
            title = "Test incident",
            description = "Description",
            severity = "HIGH",
            status = "OPEN",
            createdAt = baseTime,
            updatedAt = baseTime
        )

        val diagnosisEntity = DiagnosisEntity(
            id = 1L,
            incident = incidentEntity,
            suggestedRootCause = "Root cause",
            remediationSteps = "Step 1",
            confidence = "HIGH",
            verification = "UNVERIFIED",
            createdAt = createdTime
        )

        // Act
        val domain = diagnosisEntity.toDomain()

        // Assert
        assertEquals(createdTime, domain.createdAt)
    }
}
