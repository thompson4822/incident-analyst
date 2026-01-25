package com.example.incidentanalyst.diagnosis

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import com.example.incidentanalyst.incident.IncidentEntity
import com.example.incidentanalyst.incident.IncidentId
import java.time.Instant

class DiagnosisModelsTest {

    @Test
    fun `DiagnosisId value class wraps Long value`() {
        // Arrange & Act
        val id = DiagnosisId(123L)

        // Assert
        assertEquals(123L, id.value)
    }

    @Test
    fun `Confidence enum values are correct`() {
        // Arrange & Act & Assert
        assertEquals(4, Confidence.entries.size)
        assertEquals("HIGH", Confidence.HIGH.name)
        assertEquals("MEDIUM", Confidence.MEDIUM.name)
        assertEquals("LOW", Confidence.LOW.name)
        assertEquals("UNKNOWN", Confidence.UNKNOWN.name)
    }

    @Test
    fun `DiagnosisVerification Unverified exists`() {
        // Arrange & Act
        val unverified: DiagnosisVerification = DiagnosisVerification.Unverified

        // Assert
        assertTrue(unverified is DiagnosisVerification.Unverified)
    }

    @Test
    fun `DiagnosisVerification VerifiedByHuman exists`() {
        // Arrange & Act
        val verified: DiagnosisVerification = DiagnosisVerification.VerifiedByHuman

        // Assert
        assertTrue(verified is DiagnosisVerification.VerifiedByHuman)
    }

    @Test
    fun `DiagnosisVerification ADT has correct subtypes`() {
        // Arrange & Act
        val unverified: DiagnosisVerification = DiagnosisVerification.Unverified
        val verified: DiagnosisVerification = DiagnosisVerification.VerifiedByHuman

        // Assert
        assertTrue(unverified is DiagnosisVerification.Unverified)
        assertTrue(verified is DiagnosisVerification.VerifiedByHuman)
    }

    @Test
    fun `Diagnosis data class properties are correct`() {
        // Arrange
        val baseTime = Instant.now()
        val diagnosis = Diagnosis(
            id = DiagnosisId(1L),
            incidentId = IncidentId(10L),
            rootCause = "Root cause",
            steps = listOf("Step 1", "Step 2"),
            confidence = Confidence.HIGH,
            verification = DiagnosisVerification.VerifiedByHuman,
            createdAt = baseTime
        )

        // Act & Assert
        assertEquals(1L, diagnosis.id.value)
        assertEquals(10L, diagnosis.incidentId.value)
        assertEquals("Root cause", diagnosis.rootCause)
        assertEquals(2, diagnosis.steps.size)
        assertEquals(Confidence.HIGH, diagnosis.confidence)
        assertTrue(diagnosis.verification is DiagnosisVerification.VerifiedByHuman)
        assertEquals(baseTime, diagnosis.createdAt)
    }

    @Test
    fun `DiagnosisError NotFound exists`() {
        // Arrange & Act
        val notFound: DiagnosisError = DiagnosisError.NotFound

        // Assert
        assertTrue(notFound is DiagnosisError.NotFound)
    }

    @Test
    fun `DiagnosisError UpdateFailed exists`() {
        // Arrange & Act
        val updateFailed: DiagnosisError = DiagnosisError.UpdateFailed

        // Assert
        assertTrue(updateFailed is DiagnosisError.UpdateFailed)
    }

    @Test
    fun `DiagnosisError IncidentNotFound exists`() {
        // Arrange & Act
        val incidentNotFound: DiagnosisError = DiagnosisError.IncidentNotFound

        // Assert
        assertTrue(incidentNotFound is DiagnosisError.IncidentNotFound)
    }

    @Test
    fun `DiagnosisError RetrievalFailed exists`() {
        // Arrange & Act
        val retrievalFailed: DiagnosisError = DiagnosisError.RetrievalFailed

        // Assert
        assertTrue(retrievalFailed is DiagnosisError.RetrievalFailed)
    }

    @Test
    fun `DiagnosisError LlmUnavailable exists`() {
        // Arrange & Act
        val llmUnavailable: DiagnosisError = DiagnosisError.LlmUnavailable

        // Assert
        assertTrue(llmUnavailable is DiagnosisError.LlmUnavailable)
    }

    @Test
    fun `DiagnosisError LlmResponseInvalid exists with reason`() {
        // Arrange & Act
        val reason = "Invalid JSON format"
        val llmResponseInvalid: DiagnosisError = DiagnosisError.LlmResponseInvalid(reason)

        // Assert
        assertTrue(llmResponseInvalid is DiagnosisError.LlmResponseInvalid)
        assertEquals(reason, (llmResponseInvalid as DiagnosisError.LlmResponseInvalid).reason)
    }

    @Test
    fun `DiagnosisResult Success wraps diagnosis`() {
        // Arrange
        val baseTime = Instant.now()
        val diagnosis = Diagnosis(
            id = DiagnosisId(1L),
            incidentId = IncidentId(10L),
            rootCause = "Root cause",
            steps = listOf("Step 1"),
            confidence = Confidence.HIGH,
            verification = DiagnosisVerification.VerifiedByHuman,
            createdAt = baseTime
        )
        val successResult: DiagnosisResult = DiagnosisResult.Success(diagnosis)

        // Act & Assert
        assertTrue(successResult is DiagnosisResult.Success)
        assertEquals(diagnosis, (successResult as DiagnosisResult.Success).diagnosis)
    }

    @Test
    fun `DiagnosisResult Failure wraps error`() {
        // Arrange
        val failureResult: DiagnosisResult = DiagnosisResult.Failure(DiagnosisError.NotFound)

        // Act & Assert
        assertTrue(failureResult is DiagnosisResult.Failure)
        assertTrue((failureResult as DiagnosisResult.Failure).error is DiagnosisError.NotFound)
    }

    @Test
    fun `pattern matching on DiagnosisResult works correctly`() {
        // Arrange
        val baseTime = Instant.now()
        val diagnosis = Diagnosis(
            id = DiagnosisId(1L),
            incidentId = IncidentId(10L),
            rootCause = "Root cause",
            steps = listOf("Step 1"),
            confidence = Confidence.HIGH,
            verification = DiagnosisVerification.VerifiedByHuman,
            createdAt = baseTime
        )

        // Act & Assert - Success case
        val successResult: DiagnosisResult = DiagnosisResult.Success(diagnosis)
        var successHandled = false
        var failureHandled = false

        when (successResult) {
            is DiagnosisResult.Success -> successHandled = true
            is DiagnosisResult.Failure -> failureHandled = true
        }

        assertTrue(successHandled)
        assertFalse(failureHandled)

        // Act & Assert - Failure case
        val failureResult: DiagnosisResult = DiagnosisResult.Failure(DiagnosisError.NotFound)
        successHandled = false
        failureHandled = false

        when (failureResult) {
            is DiagnosisResult.Success -> successHandled = true
            is DiagnosisResult.Failure -> failureHandled = true
        }

        assertFalse(successHandled)
        assertTrue(failureHandled)
    }

    @Test
    fun `toDomain maps verification VERIFIED correctly`() {
        // Arrange
        val baseTime = Instant.now()
        val incidentEntity = IncidentEntity(
            id = 10L,
            source = "monitoring",
            title = "Test incident",
            description = "Description",
            severity = "HIGH",
            status = "OPEN",
            createdAt = baseTime,
            updatedAt = baseTime
        )

        val entity = DiagnosisEntity(
            id = 1L,
            incident = incidentEntity,
            suggestedRootCause = "Root cause",
            remediationSteps = "Step 1",
            confidence = "HIGH",
            verification = "VERIFIED",
            createdAt = baseTime
        )

        // Act
        val domain = entity.toDomain()

        // Assert
        assertTrue(domain.verification is DiagnosisVerification.VerifiedByHuman)
    }

    @Test
    fun `toDomain maps verification UNVERIFIED correctly`() {
        // Arrange
        val baseTime = Instant.now()
        val incidentEntity = IncidentEntity(
            id = 10L,
            source = "monitoring",
            title = "Test incident",
            description = "Description",
            severity = "HIGH",
            status = "OPEN",
            createdAt = baseTime,
            updatedAt = baseTime
        )

        val entity = DiagnosisEntity(
            id = 1L,
            incident = incidentEntity,
            suggestedRootCause = "Root cause",
            remediationSteps = "Step 1",
            confidence = "HIGH",
            verification = "UNVERIFIED",
            createdAt = baseTime
        )

        // Act
        val domain = entity.toDomain()

        // Assert
        assertTrue(domain.verification is DiagnosisVerification.Unverified)
    }

    @Test
    fun `toDomain handles unknown verification strings`() {
        // Arrange
        val baseTime = Instant.now()
        val incidentEntity = IncidentEntity(
            id = 10L,
            source = "monitoring",
            title = "Test incident",
            description = "Description",
            severity = "HIGH",
            status = "OPEN",
            createdAt = baseTime,
            updatedAt = baseTime
        )

        val entity = DiagnosisEntity(
            id = 1L,
            incident = incidentEntity,
            suggestedRootCause = "Root cause",
            remediationSteps = "Step 1",
            confidence = "HIGH",
            verification = "UNKNOWN_STATUS",
            createdAt = baseTime
        )

        // Act
        val domain = entity.toDomain()

        // Assert - Unknown verification should coerce to Unverified as per implementation
        assertTrue(domain.verification is DiagnosisVerification.Unverified)
    }

    @Test
    fun `toDomain handles empty verification string`() {
        // Arrange
        val baseTime = Instant.now()
        val incidentEntity = IncidentEntity(
            id = 10L,
            source = "monitoring",
            title = "Test incident",
            description = "Description",
            severity = "HIGH",
            status = "OPEN",
            createdAt = baseTime,
            updatedAt = baseTime
        )

        val entity = DiagnosisEntity(
            id = 1L,
            incident = incidentEntity,
            suggestedRootCause = "Root cause",
            remediationSteps = "Step 1",
            confidence = "HIGH",
            verification = "",
            createdAt = baseTime
        )

        // Act
        val domain = entity.toDomain()

        // Assert - Empty verification should coerce to Unverified as per implementation
        assertTrue(domain.verification is DiagnosisVerification.Unverified)
    }

    @Test
    fun `toEntity maps VerifiedByHuman to VERIFIED`() {
        // Arrange
        val baseTime = Instant.now()
        val incidentEntity = IncidentEntity(
            id = 10L,
            source = "monitoring",
            title = "Test incident",
            description = "Description",
            severity = "HIGH",
            status = "OPEN",
            createdAt = baseTime,
            updatedAt = baseTime
        )

        val domain = Diagnosis(
            id = DiagnosisId(1L),
            incidentId = IncidentId(10L),
            rootCause = "Root cause",
            steps = listOf("Step 1"),
            confidence = Confidence.HIGH,
            verification = DiagnosisVerification.VerifiedByHuman,
            createdAt = baseTime
        )

        // Act
        val entity = domain.toEntity(incidentEntity)

        // Assert
        assertEquals("VERIFIED", entity.verification)
    }

    @Test
    fun `toEntity maps Unverified to UNVERIFIED`() {
        // Arrange
        val baseTime = Instant.now()
        val incidentEntity = IncidentEntity(
            id = 10L,
            source = "monitoring",
            title = "Test incident",
            description = "Description",
            severity = "HIGH",
            status = "OPEN",
            createdAt = baseTime,
            updatedAt = baseTime
        )

        val domain = Diagnosis(
            id = DiagnosisId(1L),
            incidentId = IncidentId(10L),
            rootCause = "Root cause",
            steps = listOf("Step 1"),
            confidence = Confidence.HIGH,
            verification = DiagnosisVerification.Unverified,
            createdAt = baseTime
        )

        // Act
        val entity = domain.toEntity(incidentEntity)

        // Assert
        assertEquals("UNVERIFIED", entity.verification)
    }

    @Test
    fun `toEntity maps confidence enum correctly`() {
        // Arrange
        val baseTime = Instant.now()
        val incidentEntity = IncidentEntity(
            id = 10L,
            source = "monitoring",
            title = "Test incident",
            description = "Description",
            severity = "HIGH",
            status = "OPEN",
            createdAt = baseTime,
            updatedAt = baseTime
        )

        val confidences = listOf(Confidence.HIGH, Confidence.MEDIUM, Confidence.LOW, Confidence.UNKNOWN)
        confidences.forEach { confidence ->
            val domain = Diagnosis(
                id = DiagnosisId(1L),
                incidentId = IncidentId(10L),
                rootCause = "Root cause",
                steps = listOf("Step 1"),
                confidence = confidence,
                verification = DiagnosisVerification.Unverified,
                createdAt = baseTime
            )

            // Act
            val entity = domain.toEntity(incidentEntity)

            // Assert
            assertEquals(confidence.name, entity.confidence)
        }
    }

    @Test
    fun `toEntity maps steps list correctly`() {
        // Arrange
        val baseTime = Instant.now()
        val incidentEntity = IncidentEntity(
            id = 10L,
            source = "monitoring",
            title = "Test incident",
            description = "Description",
            severity = "HIGH",
            status = "OPEN",
            createdAt = baseTime,
            updatedAt = baseTime
        )

        val domain = Diagnosis(
            id = DiagnosisId(1L),
            incidentId = IncidentId(10L),
            rootCause = "Root cause",
            steps = listOf("Step 1", "Step 2", "Step 3"),
            confidence = Confidence.HIGH,
            verification = DiagnosisVerification.Unverified,
            createdAt = baseTime
        )

        // Act
        val entity = domain.toEntity(incidentEntity)

        // Assert
        assertEquals("Step 1\nStep 2\nStep 3", entity.remediationSteps)
    }

    @Test
    fun `toEntity handles empty steps list`() {
        // Arrange
        val baseTime = Instant.now()
        val incidentEntity = IncidentEntity(
            id = 10L,
            source = "monitoring",
            title = "Test incident",
            description = "Description",
            severity = "HIGH",
            status = "OPEN",
            createdAt = baseTime,
            updatedAt = baseTime
        )

        val domain = Diagnosis(
            id = DiagnosisId(1L),
            incidentId = IncidentId(10L),
            rootCause = "Root cause",
            steps = emptyList(),
            confidence = Confidence.HIGH,
            verification = DiagnosisVerification.Unverified,
            createdAt = baseTime
        )

        // Act
        val entity = domain.toEntity(incidentEntity)

        // Assert
        assertEquals("", entity.remediationSteps)
    }

    @Test
    fun `toEntity preserves timestamp`() {
        // Arrange
        val createdTime = Instant.now().minusSeconds(3600)
        val baseTime = Instant.now()
        val incidentEntity = IncidentEntity(
            id = 10L,
            source = "monitoring",
            title = "Test incident",
            description = "Description",
            severity = "HIGH",
            status = "OPEN",
            createdAt = baseTime,
            updatedAt = baseTime
        )

        val domain = Diagnosis(
            id = DiagnosisId(1L),
            incidentId = IncidentId(10L),
            rootCause = "Root cause",
            steps = listOf("Step 1"),
            confidence = Confidence.HIGH,
            verification = DiagnosisVerification.Unverified,
            createdAt = createdTime
        )

        // Act
        val entity = domain.toEntity(incidentEntity)

        // Assert
        assertEquals(createdTime, entity.createdAt)
    }

    private fun assertFalse(condition: Boolean) {
        assertEquals(false, condition)
    }
}
