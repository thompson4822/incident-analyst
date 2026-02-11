package com.example.incidentanalyst.remediation

import com.example.incidentanalyst.incident.IncidentId
import com.example.incidentanalyst.incident.IncidentService
import jakarta.enterprise.context.ApplicationScoped
import org.jboss.logging.Logger
import java.util.concurrent.ConcurrentHashMap

@ApplicationScoped
class RemediationService(
    private val incidentService: IncidentService,
    private val actionExecutor: ActionExecutor
) {
    private val log = Logger.getLogger(javaClass)
    private val activePlans = ConcurrentHashMap<Long, RemediationPlan>()

    fun createPlan(incidentId: IncidentId, steps: List<RemediationStep>): RemediationPlan {
        val plan = RemediationPlan(incidentId, steps)
        activePlans[incidentId.value] = plan
        return plan
    }

    fun getPlan(incidentId: IncidentId): RemediationPlan? = activePlans[incidentId.value]

    fun executeStep(incidentId: IncidentId, stepId: String): RemediationStep {
        val plan = activePlans[incidentId.value] ?: throw IllegalArgumentException("No plan found for incident ${incidentId.value}")
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

        val outcome = step.action?.let { actionExecutor.execute(it) } ?: "Step completed (no automated action)"

        val finalStep = updatedStep.copy(
            status = StepStatus.COMPLETED,
            outcome = outcome
        )
        
        updatedSteps[stepIndex] = finalStep
        activePlans[incidentId.value] = plan.copy(steps = updatedSteps)
        
        return finalStep
    }
}
