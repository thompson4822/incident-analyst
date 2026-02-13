package com.example.incidentanalyst.remediation

import com.example.incidentanalyst.diagnosis.Diagnosis
import com.example.incidentanalyst.incident.IncidentId
import com.example.incidentanalyst.incident.IncidentService
import jakarta.enterprise.context.ApplicationScoped
import org.jboss.logging.Logger
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

@ApplicationScoped
class RemediationService(
    private val incidentService: IncidentService,
    private val actionExecutor: ActionExecutor
) {
    private val log = Logger.getLogger(javaClass)
    private val activePlans = ConcurrentHashMap<Long, RemediationPlan>()
    private val activeExecutions = ConcurrentHashMap<Long, RemediationProgress>()

    fun createPlan(incidentId: IncidentId, steps: List<RemediationStep>): RemediationPlan {
        val plan = RemediationPlan(incidentId, steps)
        activePlans[incidentId.value] = plan
        return plan
    }

    fun getPlan(incidentId: IncidentId): RemediationPlan? = activePlans[incidentId.value]

    fun getProgress(incidentId: IncidentId): RemediationProgress? = 
        activeExecutions[incidentId.value]

    fun executeAllSteps(incidentId: IncidentId, diagnosis: Diagnosis) {
        val steps = diagnosis.structuredSteps.ifEmpty {
            // Wrap text steps in ManualStep automatically
            diagnosis.steps.mapIndexed { index, stepText ->
                RemediationStep(
                    id = "step-${index + 1}",
                    description = stepText,
                    action = RemediationAction.ManualStep(stepText),
                    status = StepStatus.PENDING,
                    outcome = null
                )
            }
        }

        // Initialize progress
        val progress = RemediationProgress(
            incidentId = incidentId.value,
            diagnosisId = diagnosis.id.value,
            steps = steps,
            currentStepIndex = 0,
            status = ExecutionStatus.IN_PROGRESS,
            startedAt = Instant.now(),
            completedAt = null
        )
        activeExecutions[incidentId.value] = progress

        // Execute steps sequentially (in-memory, blocking for Phase 1)
        var currentProgress = progress
        for ((index, step) in steps.withIndex()) {
            // Update step status to IN_PROGRESS
            val inProgressSteps = currentProgress.steps.toMutableList()
            inProgressSteps[index] = step.copy(status = StepStatus.IN_PROGRESS)
            currentProgress = currentProgress.copy(
                steps = inProgressSteps,
                currentStepIndex = index
            )
            activeExecutions[incidentId.value] = currentProgress

            log.infof("Executing remediation step %d: %s", index + 1, step.description)

            // Simulate 1-2 second delay
            Thread.sleep(1000 + (Math.random() * 1000).toLong())

            // Execute the action
            val outcome = step.action?.let { actionExecutor.execute(it) } 
                ?: "Step completed (no automated action)"

            // Update step status to COMPLETED
            val completedSteps = currentProgress.steps.toMutableList()
            completedSteps[index] = inProgressSteps[index].copy(
                status = StepStatus.COMPLETED,
                outcome = outcome
            )
            currentProgress = currentProgress.copy(steps = completedSteps)
            activeExecutions[incidentId.value] = currentProgress
        }

        // Mark execution as completed
        activeExecutions[incidentId.value] = currentProgress.copy(
            status = ExecutionStatus.COMPLETED,
            completedAt = Instant.now()
        )
        log.infof("Remediation completed for incident %d", incidentId.value)
    }

    fun executeStep(incidentId: IncidentId, stepId: String): RemediationStep {
        val plan = activePlans[incidentId.value] 
            ?: throw IllegalArgumentException("No plan found for incident ${incidentId.value}")
        val stepIndex = plan.steps.indexOfFirst { it.id == stepId }
        if (stepIndex == -1) throw IllegalArgumentException("Step $stepId not found in plan")

        val step = plan.steps[stepIndex]
        val updatedStep = step.copy(status = StepStatus.IN_PROGRESS)
        
        val updatedSteps = plan.steps.toMutableList()
        updatedSteps[stepIndex] = updatedStep
        activePlans[incidentId.value] = plan.copy(steps = updatedSteps)

        // Simulate execution
        log.infof("Executing remediation step: %s", step.description)
        Thread.sleep(1000) // Simulate latency

        val outcome = step.action?.let { actionExecutor.execute(it) } 
            ?: "Step completed (no automated action)"

        val finalStep = updatedStep.copy(
            status = StepStatus.COMPLETED,
            outcome = outcome
        )
        
        updatedSteps[stepIndex] = finalStep
        activePlans[incidentId.value] = plan.copy(steps = updatedSteps)
        
        return finalStep
    }
}
