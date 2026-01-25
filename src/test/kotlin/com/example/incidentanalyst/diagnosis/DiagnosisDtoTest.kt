package com.example.incidentanalyst.diagnosis

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import com.example.incidentanalyst.incident.IncidentId
import java.time.Instant

class DiagnosisDtoTest {

    @Test
    fun `toResponseDto maps id correctly`() {
        // Arrange
        val diagnosis = Diagnosis(
            id = DiagnosisId(123L),
            incidentId = IncidentId(10L),
            rootCause = "Root cause",
            steps = listOf("Step 1"),
            confidence = Confidence.HIGH,
            verification = DiagnosisVerification.VerifiedByHuman,
            createdAt = Instant.now()
        )

        // Act
        val dto = diagnosis.toResponseDto()

        // Assert
        assertEquals(123L, dto.id)
    }

    @Test
    fun `toResponseDto maps incidentId correctly`() {
        // Arrange
        val diagnosis = Diagnosis(
            id = DiagnosisId(1L),
            incidentId = IncidentId(456L),
            rootCause = "Root cause",
            steps = listOf("Step 1"),
            confidence = Confidence.HIGH,
            verification = DiagnosisVerification.VerifiedByHuman,
            createdAt = Instant.now()
        )

        // Act
        val dto = diagnosis.toResponseDto()

        // Assert
        assertEquals(456L, dto.incidentId)
    }

    @Test
    fun `toResponseDto maps rootCause correctly`() {
        // Arrange
        val diagnosis = Diagnosis(
            id = DiagnosisId(1L),
            incidentId = IncidentId(10L),
            rootCause = "CPU spike due to runaway process",
            steps = listOf("Step 1"),
            confidence = Confidence.HIGH,
            verification = DiagnosisVerification.VerifiedByHuman,
            createdAt = Instant.now()
        )

        // Act
        val dto = diagnosis.toResponseDto()

        // Assert
        assertEquals("CPU spike due to runaway process", dto.rootCause)
    }

    @Test
    fun `toResponseDto maps steps list correctly`() {
        // Arrange
        val diagnosis = Diagnosis(
            id = DiagnosisId(1L),
            incidentId = IncidentId(10L),
            rootCause = "Root cause",
            steps = listOf("Step 1: Check CPU", "Step 2: Kill process", "Step 3: Verify"),
            confidence = Confidence.HIGH,
            verification = DiagnosisVerification.VerifiedByHuman,
            createdAt = Instant.now()
        )

        // Act
        val dto = diagnosis.toResponseDto()

        // Assert
        assertEquals(3, dto.steps.size)
        assertEquals("Step 1: Check CPU", dto.steps[0])
        assertEquals("Step 2: Kill process", dto.steps[1])
        assertEquals("Step 3: Verify", dto.steps[2])
    }

    @Test
    fun `toResponseDto maps confidence correctly`() {
        // Arrange
        val diagnosis = Diagnosis(
            id = DiagnosisId(1L),
            incidentId = IncidentId(10L),
            rootCause = "Root cause",
            steps = listOf("Step 1"),
            confidence = Confidence.MEDIUM,
            verification = DiagnosisVerification.VerifiedByHuman,
            createdAt = Instant.now()
        )

        // Act
        val dto = diagnosis.toResponseDto()

        // Assert
        assertEquals("MEDIUM", dto.confidence)
    }

    @Test
    fun `toResponseDto maps verified true for VerifiedByHuman`() {
        // Arrange
        val diagnosis = Diagnosis(
            id = DiagnosisId(1L),
            incidentId = IncidentId(10L),
            rootCause = "Root cause",
            steps = listOf("Step 1"),
            confidence = Confidence.HIGH,
            verification = DiagnosisVerification.VerifiedByHuman,
            createdAt = Instant.now()
        )

        // Act
        val dto = diagnosis.toResponseDto()

        // Assert
        assertTrue(dto.verified)
    }

    @Test
    fun `toResponseDto maps verified false for Unverified`() {
        // Arrange
        val diagnosis = Diagnosis(
            id = DiagnosisId(1L),
            incidentId = IncidentId(10L),
            rootCause = "Root cause",
            steps = listOf("Step 1"),
            confidence = Confidence.HIGH,
            verification = DiagnosisVerification.Unverified,
            createdAt = Instant.now()
        )

        // Act
        val dto = diagnosis.toResponseDto()

        // Assert
        assertFalse(dto.verified)
    }

    @Test
    fun `DiagnosisVerificationUpdateRequestDto accepts true`() {
        // Arrange & Act
        val requestDto = DiagnosisVerificationUpdateRequestDto(verified = true)

        // Assert
        assertTrue(requestDto.verified)
    }

    @Test
    fun `DiagnosisVerificationUpdateRequestDto accepts false`() {
        // Arrange & Act
        val requestDto = DiagnosisVerificationUpdateRequestDto(verified = false)

        // Assert
        assertFalse(requestDto.verified)
    }

    @Test
    fun `All DTO fields are included in response`() {
        // Arrange
        val diagnosis = Diagnosis(
            id = DiagnosisId(123L),
            incidentId = IncidentId(456L),
            rootCause = "Custom root cause",
            steps = listOf("Step A", "Step B"),
            confidence = Confidence.LOW,
            verification = DiagnosisVerification.VerifiedByHuman,
            createdAt = Instant.now()
        )

        // Act
        val dto = diagnosis.toResponseDto()

        // Assert - All fields should be populated
        assertEquals(123L, dto.id)
        assertEquals(456L, dto.incidentId)
        assertEquals("Custom root cause", dto.rootCause)
        assertEquals(2, dto.steps.size)
        assertEquals("Step A", dto.steps[0])
        assertEquals("Step B", dto.steps[1])
        assertEquals("LOW", dto.confidence)
        assertTrue(dto.verified)
    }

    @Test
    fun `Steps list preserved in DTO`() {
        // Arrange
        val steps = listOf(
            "Step 1: Identify the issue",
            "Step 2: Analyze logs",
            "Step 3: Determine root cause",
            "Step 4: Implement fix",
            "Step 5: Verify resolution"
        )
        val diagnosis = Diagnosis(
            id = DiagnosisId(1L),
            incidentId = IncidentId(10L),
            rootCause = "Root cause",
            steps = steps,
            confidence = Confidence.HIGH,
            verification = DiagnosisVerification.Unverified,
            createdAt = Instant.now()
        )

        // Act
        val dto = diagnosis.toResponseDto()

        // Assert
        assertEquals(steps.size, dto.steps.size)
        steps.forEachIndexed { index, step ->
            assertEquals(step, dto.steps[index])
        }
    }

    @Test
    fun `Confidence enum name used in DTO`() {
        // Arrange
        val confidences = listOf(Confidence.HIGH, Confidence.MEDIUM, Confidence.LOW, Confidence.UNKNOWN)
        confidences.forEach { confidence ->
            val diagnosis = Diagnosis(
                id = DiagnosisId(1L),
                incidentId = IncidentId(10L),
                rootCause = "Root cause",
                steps = listOf("Step 1"),
                confidence = confidence,
                verification = DiagnosisVerification.Unverified,
                createdAt = Instant.now()
            )

            // Act
            val dto = diagnosis.toResponseDto()

            // Assert
            assertEquals(confidence.name, dto.confidence)
        }
    }

    @Test
    fun `toResponseDto handles empty steps list`() {
        // Arrange
        val diagnosis = Diagnosis(
            id = DiagnosisId(1L),
            incidentId = IncidentId(10L),
            rootCause = "Root cause",
            steps = emptyList(),
            confidence = Confidence.HIGH,
            verification = DiagnosisVerification.VerifiedByHuman,
            createdAt = Instant.now()
        )

        // Act
        val dto = diagnosis.toResponseDto()

        // Assert
        assertEquals(0, dto.steps.size)
    }

    @Test
    fun `toResponseDto handles single step`() {
        // Arrange
        val diagnosis = Diagnosis(
            id = DiagnosisId(1L),
            incidentId = IncidentId(10L),
            rootCause = "Root cause",
            steps = listOf("Single remediation step"),
            confidence = Confidence.HIGH,
            verification = DiagnosisVerification.VerifiedByHuman,
            createdAt = Instant.now()
        )

        // Act
        val dto = diagnosis.toResponseDto()

        // Assert
        assertEquals(1, dto.steps.size)
        assertEquals("Single remediation step", dto.steps[0])
    }

    @Test
    fun `toResponseDto creates complete DTO with verified diagnosis`() {
        // Arrange
        val diagnosis = Diagnosis(
            id = DiagnosisId(999L),
            incidentId = IncidentId(888L),
            rootCause = "Database connection pool exhaustion",
            steps = listOf("Check connection pool size", "Increase pool limit", "Restart application"),
            confidence = Confidence.HIGH,
            verification = DiagnosisVerification.VerifiedByHuman,
            createdAt = Instant.now()
        )

        // Act
        val dto = diagnosis.toResponseDto()

        // Assert
        assertEquals(999L, dto.id)
        assertEquals(888L, dto.incidentId)
        assertEquals("Database connection pool exhaustion", dto.rootCause)
        assertEquals(3, dto.steps.size)
        assertEquals("Check connection pool size", dto.steps[0])
        assertEquals("Increase pool limit", dto.steps[1])
        assertEquals("Restart application", dto.steps[2])
        assertEquals("HIGH", dto.confidence)
        assertTrue(dto.verified)
    }

    @Test
    fun `toResponseDto creates complete DTO with unverified diagnosis`() {
        // Arrange
        val diagnosis = Diagnosis(
            id = DiagnosisId(777L),
            incidentId = IncidentId(666L),
            rootCause = "Memory leak in background worker",
            steps = listOf("Analyze heap dump", "Identify leaking objects", "Fix object references"),
            confidence = Confidence.MEDIUM,
            verification = DiagnosisVerification.Unverified,
            createdAt = Instant.now()
        )

        // Act
        val dto = diagnosis.toResponseDto()

        // Assert
        assertEquals(777L, dto.id)
        assertEquals(666L, dto.incidentId)
        assertEquals("Memory leak in background worker", dto.rootCause)
        assertEquals(3, dto.steps.size)
        assertEquals("Analyze heap dump", dto.steps[0])
        assertEquals("Identify leaking objects", dto.steps[1])
        assertEquals("Fix object references", dto.steps[2])
        assertEquals("MEDIUM", dto.confidence)
        assertFalse(dto.verified)
    }
}
