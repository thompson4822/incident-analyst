package com.example.incidentanalyst.diagnosis

import com.example.incidentanalyst.common.Either
import com.example.incidentanalyst.incident.IncidentEntity
import com.example.incidentanalyst.rag.EmbeddingService
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
class DiagnosisServiceTest {

    @InjectMock
    lateinit var diagnosisRepository: DiagnosisRepository

    @InjectMock
    lateinit var embeddingService: EmbeddingService

    @Inject
    lateinit var diagnosisService: DiagnosisService

    @BeforeEach
    fun setup() {
        reset(diagnosisRepository, embeddingService)
    }

    @Test
    fun `listRecent returns diagnoses ordered by createdAt desc`() {
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

        val oldDiagnosis = DiagnosisEntity(
            id = 1L,
            incident = incidentEntity,
            suggestedRootCause = "Old root cause",
            remediationSteps = "Old step 1\nOld step 2",
            confidence = "HIGH",
            verification = "UNVERIFIED",
            createdAt = baseTime.minusSeconds(3600)
        )

        val newDiagnosis = DiagnosisEntity(
            id = 2L,
            incident = incidentEntity,
            suggestedRootCause = "New root cause",
            remediationSteps = "New step 1\nNew step 2",
            confidence = "MEDIUM",
            verification = "VERIFIED",
            createdAt = baseTime
        )

        whenever(diagnosisRepository.findRecent(50)).thenReturn(listOf(newDiagnosis, oldDiagnosis))

        // Act
        val result = diagnosisService.listRecent()

        // Assert
        assertEquals(2, result.size)
        assertEquals(2L, result[0].id.value)
        assertEquals(1L, result[1].id.value)
    }

    @Test
    fun `listRecent with limit parameter works`() {
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

        val diagnoses = (1..10).map { i ->
            DiagnosisEntity(
                id = i.toLong(),
                incident = incidentEntity,
                suggestedRootCause = "Root cause $i",
                remediationSteps = "Step $i",
                confidence = "HIGH",
                verification = "UNVERIFIED",
                createdAt = baseTime.minusSeconds((10 - i).toLong())
            )
        }

        whenever(diagnosisRepository.findRecent(5)).thenReturn(diagnoses.take(5))

        // Act
        val result = diagnosisService.listRecent(5)

        // Assert
        assertEquals(5, result.size)
        assertEquals(1L, result[0].id.value)
        assertEquals(5L, result[4].id.value)
    }

    @Test
    fun `listRecent returns empty list when no diagnoses exist`() {
        // Arrange
        whenever(diagnosisRepository.findRecent(50)).thenReturn(emptyList())

        // Act
        val result = diagnosisService.listRecent()

        // Assert
        assertEquals(0, result.size)
    }

    @Test
    fun `getById returns Success for valid diagnosis`() {
        // Arrange
        val testId = 123L
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

        val entity = DiagnosisEntity(
            id = testId,
            incident = incidentEntity,
            suggestedRootCause = "Root cause: CPU spike",
            remediationSteps = "Step 1: Check CPU\nStep 2: Kill process",
            confidence = "HIGH",
            verification = "VERIFIED",
            createdAt = baseTime
        )
        whenever(diagnosisRepository.findById(testId)).thenReturn(entity)

        // Act
        val result = diagnosisService.getById(DiagnosisId(testId))

        // Assert
        assertTrue(result is Either.Right)
        val diagnosis = (result as Either.Right).value
        assertEquals(testId, diagnosis.id.value)
        assertEquals("Root cause: CPU spike", diagnosis.rootCause)
        assertEquals(Confidence.HIGH, diagnosis.confidence)
        assertTrue(diagnosis.verification is DiagnosisVerification.VerifiedByHuman)
    }

    @Test
    fun `getById returns Failure NotFound for non-existent diagnosis`() {
        // Arrange
        val testId = 999L
        whenever(diagnosisRepository.findById(testId)).thenReturn(null)

        // Act
        val result = diagnosisService.getById(DiagnosisId(testId))

        // Assert
        assertTrue(result is Either.Left)
        val error = (result as Either.Left).value
        assertTrue(error is DiagnosisError.NotFound)
    }

    @Test
    fun `getById Success contains correct steps list`() {
        // Arrange
        val testId = 456L
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

        val entity = DiagnosisEntity(
            id = testId,
            incident = incidentEntity,
            suggestedRootCause = "Root cause",
            remediationSteps = "Step 1\nStep 2\nStep 3",
            confidence = "MEDIUM",
            verification = "UNVERIFIED",
            createdAt = baseTime
        )
        whenever(diagnosisRepository.findById(testId)).thenReturn(entity)

        // Act
        val result = diagnosisService.getById(DiagnosisId(testId))

        // Assert
        assertTrue(result is Either.Right)
        val diagnosis = (result as Either.Right).value
        assertEquals(3, diagnosis.steps.size)
        assertEquals("Step 1", diagnosis.steps[0])
        assertEquals("Step 2", diagnosis.steps[1])
        assertEquals("Step 3", diagnosis.steps[2])
    }

    @Test
    fun `getById Success contains Unverified verification`() {
        // Arrange
        val testId = 789L
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

        val entity = DiagnosisEntity(
            id = testId,
            incident = incidentEntity,
            suggestedRootCause = "Root cause",
            remediationSteps = "Step 1",
            confidence = "LOW",
            verification = "UNVERIFIED",
            createdAt = baseTime
        )
        whenever(diagnosisRepository.findById(testId)).thenReturn(entity)

        // Act
        val result = diagnosisService.getById(DiagnosisId(testId))

        // Assert
        assertTrue(result is Either.Right)
        val diagnosis = (result as Either.Right).value
        assertTrue(diagnosis.verification is DiagnosisVerification.Unverified)
    }

    @Test
    fun `Either Success pattern matching works correctly`() {
        // Arrange
        val testId = 111L
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

        val entity = DiagnosisEntity(
            id = testId,
            incident = incidentEntity,
            suggestedRootCause = "Test root cause",
            remediationSteps = "Test step",
            confidence = "UNKNOWN",
            verification = "UNVERIFIED",
            createdAt = baseTime
        )
        whenever(diagnosisRepository.findById(testId)).thenReturn(entity)

        // Act
        val result = diagnosisService.getById(DiagnosisId(testId))

        // Assert
        when (result) {
            is Either.Right -> {
                val diagnosis = result.value
                assertEquals("Test root cause", diagnosis.rootCause)
                assertNotNull(diagnosis.id)
            }
            is Either.Left -> {
                org.junit.jupiter.api.Assertions.fail<Any>("Expected Right but got Left")
            }
        }
    }

    @Test
    fun `Either Left pattern matching works correctly`() {
        // Arrange
        val testId = 222L
        whenever(diagnosisRepository.findById(testId)).thenReturn(null)

        // Act
        val result = diagnosisService.getById(DiagnosisId(testId))

        // Assert
        when (result) {
            is Either.Right -> {
                org.junit.jupiter.api.Assertions.fail<Any>("Expected Left but got Right")
            }
            is Either.Left -> {
                assertTrue(result.value is DiagnosisError.NotFound)
            }
        }
    }

    @Test
    fun `updateVerification updates status successfully`() {
        // Arrange
        val testId = 123L
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

        val entity = DiagnosisEntity(
            id = testId,
            incident = incidentEntity,
            suggestedRootCause = "Test root cause",
            remediationSteps = "Step 1",
            confidence = "MEDIUM",
            verification = "UNVERIFIED",
            createdAt = baseTime
        )
        whenever(diagnosisRepository.findById(testId)).thenReturn(entity)

        // Act
        val result = diagnosisService.updateVerification(DiagnosisId(testId), DiagnosisVerification.VerifiedByHuman)

        // Assert
        assertTrue(result is Either.Right)
        val diagnosis = (result as Either.Right).value
        assertTrue(diagnosis.verification is DiagnosisVerification.VerifiedByHuman)
        assertEquals("VERIFIED", entity.verification)
    }

    @Test
    fun `updateVerification returns Failure NotFound for non-existent diagnosis`() {
        // Arrange
        val testId = 999L
        whenever(diagnosisRepository.findById(testId)).thenReturn(null)

        // Act
        val result = diagnosisService.updateVerification(DiagnosisId(testId), DiagnosisVerification.VerifiedByHuman)

        // Assert
        assertTrue(result is Either.Left)
        val error = (result as Either.Left).value
        assertTrue(error is DiagnosisError.NotFound)
    }

    @Test
    fun `verify updates verification status, verifiedAt, and verifiedBy`() {
        // Arrange
        val testId = 123L
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
        val entity = DiagnosisEntity(
            id = testId,
            incident = incidentEntity,
            suggestedRootCause = "Root cause",
            remediationSteps = "Step 1",
            confidence = "MEDIUM",
            verification = "UNVERIFIED",
            createdAt = baseTime
        )
        whenever(diagnosisRepository.findById(testId)).thenReturn(entity)
        whenever(embeddingService.embedVerifiedDiagnosis(any())).thenReturn(Either.Right(1))

        // Act
        val result = diagnosisService.verify(DiagnosisId(testId), "testuser")

        // Assert
        assertTrue(result is Either.Right)
        val diagnosis = (result as Either.Right).value
        assertEquals("VERIFIED", entity.verification)
        assertNotNull(entity.verifiedAt)
        assertEquals("testuser", entity.verifiedBy)
        assertTrue(diagnosis.verification is DiagnosisVerification.VerifiedByHuman)
    }

    @Test
    fun `verify calls embeddingService with correct diagnosis ID`() {
        // Arrange
        val testId = 456L
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
        val entity = DiagnosisEntity(
            id = testId,
            incident = incidentEntity,
            suggestedRootCause = "Root cause",
            remediationSteps = "Step 1",
            confidence = "MEDIUM",
            verification = "UNVERIFIED",
            createdAt = baseTime
        )
        whenever(diagnosisRepository.findById(testId)).thenReturn(entity)
        whenever(embeddingService.embedVerifiedDiagnosis(any())).thenReturn(Either.Right(1))

        // Act
        diagnosisService.verify(DiagnosisId(testId), "user123")

        // Assert
        verify(embeddingService).embedVerifiedDiagnosis(eq(DiagnosisId(testId)))
    }

    @Test
    fun `verify returns NotFound for non-existent diagnosis`() {
        // Arrange
        val testId = 999L
        whenever(diagnosisRepository.findById(testId)).thenReturn(null)

        // Act
        val result = diagnosisService.verify(DiagnosisId(testId), "testuser")

        // Assert
        assertTrue(result is Either.Left)
        val error = (result as Either.Left).value
        assertTrue(error is DiagnosisError.NotFound)
    }

    @Test
    fun `verify succeeds even when embedding service fails`() {
        // Arrange
        val testId = 789L
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
        val entity = DiagnosisEntity(
            id = testId,
            incident = incidentEntity,
            suggestedRootCause = "Root cause",
            remediationSteps = "Step 1",
            confidence = "MEDIUM",
            verification = "UNVERIFIED",
            createdAt = baseTime
        )
        whenever(diagnosisRepository.findById(testId)).thenReturn(entity)
        whenever(embeddingService.embedVerifiedDiagnosis(any())).thenReturn(Either.Left(com.example.incidentanalyst.rag.EmbeddingError.EmbeddingFailed))

        // Act
        val result = diagnosisService.verify(DiagnosisId(testId), "testuser")

        // Assert - verification should succeed despite embedding failure
        assertTrue(result is Either.Right)
        val diagnosis = (result as Either.Right).value
        assertEquals("VERIFIED", entity.verification)
        assertNotNull(entity.verifiedAt)
        assertEquals("testuser", entity.verifiedBy)
    }
}
