package com.example.incidentanalyst.remediation

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

class RemediationModelsTest {

    // ========== RemediationAction Tests ==========

    @Test
    fun `RestartService action holds service name`() {
        val action = RemediationAction.RestartService("user-service")
        
        assertEquals("user-service", action.serviceName)
        assertTrue(action is RemediationAction)
    }

    @Test
    fun `ScaleCluster action holds cluster and capacity`() {
        val action = RemediationAction.ScaleCluster("prod-cluster", 5)
        
        assertEquals("prod-cluster", action.clusterId)
        assertEquals(5, action.desiredCapacity)
    }

    @Test
    fun `ManualStep action holds instructions`() {
        val action = RemediationAction.ManualStep("Check the logs for errors")
        
        assertEquals("Check the logs for errors", action.instructions)
    }

    // ========== StepStatus Tests ==========

    @Test
    fun `StepStatus has all expected values`() {
        val values = StepStatus.entries
        
        assertEquals(4, values.size)
        assertTrue(values.contains(StepStatus.PENDING))
        assertTrue(values.contains(StepStatus.IN_PROGRESS))
        assertTrue(values.contains(StepStatus.COMPLETED))
        assertTrue(values.contains(StepStatus.FAILED))
    }

    // ========== RemediationStep Tests ==========

    @Test
    fun `RemediationStep creates with defaults`() {
        val step = RemediationStep(
            id = "step-1",
            description = "Test step"
        )
        
        assertEquals("step-1", step.id)
        assertEquals("Test step", step.description)
        assertNull(step.action)
        assertEquals(StepStatus.PENDING, step.status)
        assertNull(step.outcome)
    }

    @Test
    fun `RemediationStep can have action and custom status`() {
        val action = RemediationAction.RestartService("api-gateway")
        val step = RemediationStep(
            id = "step-2",
            description = "Restart the API gateway",
            action = action,
            status = StepStatus.COMPLETED,
            outcome = "Service restarted successfully"
        )
        
        assertEquals(action, step.action)
        assertEquals(StepStatus.COMPLETED, step.status)
        assertEquals("Service restarted successfully", step.outcome)
    }

    @Test
    fun `RemediationStep copy works correctly`() {
        val original = RemediationStep(
            id = "step-1",
            description = "Original step"
        )
        
        val updated = original.copy(
            status = StepStatus.IN_PROGRESS
        )
        
        assertEquals("step-1", updated.id)
        assertEquals("Original step", updated.description)
        assertEquals(StepStatus.IN_PROGRESS, updated.status)
        assertEquals(StepStatus.PENDING, original.status) // Original unchanged
    }

    // ========== RemediationPlan Tests ==========

    @Test
    fun `RemediationPlan holds incidentId and steps`() {
        val incidentId = com.example.incidentanalyst.incident.IncidentId(42L)
        val steps = listOf(
            RemediationStep("step-1", "First step"),
            RemediationStep("step-2", "Second step")
        )
        
        val plan = RemediationPlan(incidentId, steps)
        
        assertEquals(incidentId, plan.incidentId)
        assertEquals(2, plan.steps.size)
        assertEquals("step-1", plan.steps[0].id)
    }

    // ========== ExecutionStatus Tests ==========

    @Test
    fun `ExecutionStatus has all expected values`() {
        val values = ExecutionStatus.entries
        
        assertEquals(4, values.size)
        assertTrue(values.contains(ExecutionStatus.NOT_STARTED))
        assertTrue(values.contains(ExecutionStatus.IN_PROGRESS))
        assertTrue(values.contains(ExecutionStatus.COMPLETED))
        assertTrue(values.contains(ExecutionStatus.FAILED))
    }

    // ========== RemediationProgress Tests ==========

    @Test
    fun `RemediationProgress creates with all fields`() {
        val now = Instant.now()
        val steps = listOf(
            RemediationStep("step-1", "First", status = StepStatus.COMPLETED),
            RemediationStep("step-2", "Second", status = StepStatus.PENDING)
        )
        
        val progress = RemediationProgress(
            incidentId = 123L,
            diagnosisId = 456L,
            steps = steps,
            currentStepIndex = 0,
            status = ExecutionStatus.IN_PROGRESS,
            startedAt = now,
            completedAt = null
        )
        
        assertEquals(123L, progress.incidentId)
        assertEquals(456L, progress.diagnosisId)
        assertEquals(2, progress.steps.size)
        assertEquals(0, progress.currentStepIndex)
        assertEquals(ExecutionStatus.IN_PROGRESS, progress.status)
        assertEquals(now, progress.startedAt)
        assertNull(progress.completedAt)
    }

    @Test
    fun `RemediationProgress can have error message`() {
        val progress = RemediationProgress(
            incidentId = 1L,
            diagnosisId = 1L,
            steps = emptyList(),
            currentStepIndex = 0,
            status = ExecutionStatus.FAILED,
            startedAt = Instant.now(),
            completedAt = Instant.now(),
            errorMessage = "Something went wrong"
        )
        
        assertEquals("Something went wrong", progress.errorMessage)
        assertEquals(ExecutionStatus.FAILED, progress.status)
    }

    @Test
    fun `RemediationProgress copy works correctly`() {
        val original = RemediationProgress(
            incidentId = 1L,
            diagnosisId = 1L,
            steps = listOf(RemediationStep("step-1", "Test")),
            currentStepIndex = 0,
            status = ExecutionStatus.IN_PROGRESS,
            startedAt = Instant.now(),
            completedAt = null
        )
        
        val updated = original.copy(
            status = ExecutionStatus.COMPLETED,
            completedAt = Instant.now()
        )
        
        assertEquals(ExecutionStatus.COMPLETED, updated.status)
        assertEquals(ExecutionStatus.IN_PROGRESS, original.status)
        assertNull(original.completedAt)
    }
}
