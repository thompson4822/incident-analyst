package com.example.incidentanalyst.remediation

import com.example.incidentanalyst.diagnosis.Confidence
import com.example.incidentanalyst.diagnosis.Diagnosis
import com.example.incidentanalyst.diagnosis.DiagnosisId
import com.example.incidentanalyst.diagnosis.DiagnosisVerification
import com.example.incidentanalyst.incident.IncidentId
import com.example.incidentanalyst.incident.IncidentSource
import io.quarkus.test.InjectMock
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import java.time.Instant

@QuarkusTest
class RemediationServiceTest {

    @InjectMock
    lateinit var incidentService: com.example.incidentanalyst.incident.IncidentService

    @InjectMock
    lateinit var actionExecutor: ActionExecutor

    @Inject
    lateinit var remediationService: RemediationService

    @BeforeEach
    fun setup() {
        reset(incidentService, actionExecutor)
    }

    // ========== createPlan Tests ==========

    @Test
    fun `createPlan stores and returns plan`() {
        val incidentId = IncidentId(1L)
        val steps = listOf(
            RemediationStep("step-1", "First step"),
            RemediationStep("step-2", "Second step")
        )

        val plan = remediationService.createPlan(incidentId, steps)

        assertEquals(incidentId, plan.incidentId)
        assertEquals(2, plan.steps.size)
    }

    @Test
    fun `getPlan returns null when no plan exists`() {
        val result = remediationService.getPlan(IncidentId(999L))
        
        assertNull(result)
    }

    @Test
    fun `getPlan returns created plan`() {
        val incidentId = IncidentId(2L)
        val steps = listOf(RemediationStep("step-1", "Test"))
        
        remediationService.createPlan(incidentId, steps)
        
        val retrieved = remediationService.getPlan(incidentId)
        
        assertNotNull(retrieved)
        assertEquals(incidentId, retrieved?.incidentId)
    }

    // ========== getProgress Tests ==========

    @Test
    fun `getProgress returns null when no execution started`() {
        val result = remediationService.getProgress(IncidentId(999L))
        
        assertNull(result)
    }

    // ========== executeAllSteps Tests ==========

    @Test
    fun `executeAllSteps wraps text steps in ManualStep when structuredSteps is empty`() {
        val incidentId = IncidentId(10L)
        val diagnosis = createDiagnosis(
            id = DiagnosisId(1L),
            incidentId = incidentId,
            textSteps = listOf("Check logs", "Restart service"),
            structuredSteps = emptyList()
        )

        whenever(actionExecutor.execute(any())).thenReturn("Simulated result")

        remediationService.executeAllSteps(incidentId, diagnosis)

        val progress = remediationService.getProgress(incidentId)
        assertNotNull(progress)
        assertEquals(2, progress?.steps?.size)
        assertTrue(progress?.steps?.get(0)?.action is RemediationAction.ManualStep)
        assertTrue(progress?.steps?.get(1)?.action is RemediationAction.ManualStep)
    }

    @Test
    fun `executeAllSteps uses structuredSteps when available`() {
        val incidentId = IncidentId(20L)
        val structuredSteps = listOf(
            RemediationStep(
                id = "step-1",
                description = "Restart API",
                action = RemediationAction.RestartService("api-service"),
                status = StepStatus.PENDING
            )
        )
        val diagnosis = createDiagnosis(
            id = DiagnosisId(2L),
            incidentId = incidentId,
            textSteps = listOf("This should be ignored"),
            structuredSteps = structuredSteps
        )

        whenever(actionExecutor.execute(any())).thenReturn("Service restarted successfully")

        remediationService.executeAllSteps(incidentId, diagnosis)

        val progress = remediationService.getProgress(incidentId)
        assertNotNull(progress)
        assertEquals(1, progress?.steps?.size)
        assertTrue(progress?.steps?.get(0)?.action is RemediationAction.RestartService)
    }

    @Test
    fun `executeAllSteps marks all steps as completed`() {
        val incidentId = IncidentId(30L)
        val diagnosis = createDiagnosis(
            id = DiagnosisId(3L),
            incidentId = incidentId,
            textSteps = listOf("Step 1", "Step 2", "Step 3"),
            structuredSteps = emptyList()
        )

        whenever(actionExecutor.execute(any())).thenReturn("Done")

        remediationService.executeAllSteps(incidentId, diagnosis)

        val progress = remediationService.getProgress(incidentId)
        assertNotNull(progress)
        assertEquals(ExecutionStatus.COMPLETED, progress?.status)
        progress?.steps?.forEach { step ->
            assertEquals(StepStatus.COMPLETED, step.status)
            assertNotNull(step.outcome)
        }
    }

    @Test
    fun `executeAllSteps sets timestamps correctly`() {
        val incidentId = IncidentId(40L)
        val diagnosis = createDiagnosis(
            id = DiagnosisId(4L),
            incidentId = incidentId,
            textSteps = listOf("Single step"),
            structuredSteps = emptyList()
        )

        whenever(actionExecutor.execute(any())).thenReturn("Done")

        remediationService.executeAllSteps(incidentId, diagnosis)

        val progress = remediationService.getProgress(incidentId)
        assertNotNull(progress)
        assertNotNull(progress?.startedAt)
        assertNotNull(progress?.completedAt)
        assertTrue(progress?.completedAt?.isAfter(progress?.startedAt) ?: false)
    }

    @Test
    fun `executeAllSteps records outcome from executor`() {
        val incidentId = IncidentId(50L)
        val diagnosis = createDiagnosis(
            id = DiagnosisId(5L),
            incidentId = incidentId,
            textSteps = listOf("Do something"),
            structuredSteps = emptyList()
        )

        whenever(actionExecutor.execute(any())).thenReturn("Custom outcome message")

        remediationService.executeAllSteps(incidentId, diagnosis)

        val progress = remediationService.getProgress(incidentId)
        assertEquals("Custom outcome message", progress?.steps?.get(0)?.outcome)
    }

    @Test
    fun `executeAllSteps handles step without action`() {
        val incidentId = IncidentId(60L)
        val structuredSteps = listOf(
            RemediationStep(
                id = "step-1",
                description = "Manual review needed",
                action = null,  // No action
                status = StepStatus.PENDING
            )
        )
        val diagnosis = createDiagnosis(
            id = DiagnosisId(6L),
            incidentId = incidentId,
            textSteps = emptyList(),
            structuredSteps = structuredSteps
        )

        remediationService.executeAllSteps(incidentId, diagnosis)

        val progress = remediationService.getProgress(incidentId)
        assertEquals("Step completed (no automated action)", progress?.steps?.get(0)?.outcome)
    }

    // ========== Helper Functions ==========

    private fun createDiagnosis(
        id: DiagnosisId,
        incidentId: IncidentId,
        textSteps: List<String>,
        structuredSteps: List<RemediationStep>
    ): Diagnosis {
        return Diagnosis(
            id = id,
            incidentId = incidentId,
            rootCause = "Test root cause",
            steps = textSteps,
            structuredSteps = structuredSteps,
            confidence = Confidence.HIGH,
            verification = DiagnosisVerification.Unverified,
            createdAt = Instant.now()
        )
    }
}
