package com.example.incidentanalyst.remediation

import com.example.incidentanalyst.incident.IncidentId

sealed interface RemediationAction {
    data class RestartService(val serviceName: String) : RemediationAction
    data class ScaleCluster(val clusterId: String, val desiredCapacity: Int) : RemediationAction
    data class ManualStep(val instructions: String) : RemediationAction
}

enum class StepStatus {
    PENDING, IN_PROGRESS, COMPLETED, FAILED
}

data class RemediationStep(
    val id: String,
    val description: String,
    val action: RemediationAction? = null,
    val status: StepStatus = StepStatus.PENDING,
    val outcome: String? = null
)

data class RemediationPlan(
    val incidentId: IncidentId,
    val steps: List<RemediationStep>
)

sealed interface RemediationError {
    data object IncidentNotFound : RemediationError
    data object ExecutionFailed : RemediationError
}

sealed interface RemediationResult {
    data class Success(val plan: RemediationPlan) : RemediationResult
    data class Failure(val error: RemediationError) : RemediationResult
}

enum class ExecutionStatus {
    NOT_STARTED,
    IN_PROGRESS,
    COMPLETED,
    FAILED
}

data class RemediationProgress(
    val incidentId: Long,
    val diagnosisId: Long,
    val steps: List<RemediationStep>,
    val currentStepIndex: Int,
    val status: ExecutionStatus,
    val startedAt: java.time.Instant?,
    val completedAt: java.time.Instant?,
    val errorMessage: String? = null
)
