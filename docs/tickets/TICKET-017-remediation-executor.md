# TICKET-017: Remediation Executor (Phase 1) (COMPLETED)

## Overview
Start building the "Action Executor" system to turn AI-suggested remediation steps into real, executable actions. Phase 1 focuses on the infrastructure and a "Simulated" executor.

## Tasks
- [x] Define a `RemediationAction` sealed interface (e.g., `RestartService`, `ScaleCluster`, `ManualStep`).
- [x] Create a `RemediationService` that manages the execution of a plan.
- [x] Implement a `SimulatedActionExecutor` that logs actions and simulates latency.
- [x] Update the `Diagnosis` model to support "Structured Steps" (Step Text + Optional Action).
- [x] Update the UI to show a "Live Progress" view when a remediation plan is being applied.

## Acceptance Criteria
- [x] Clicking "Apply All Steps" triggers a background execution process.
- [x] The UI updates in real-time (via HTMX polling) as each step completes.
- [x] The system logs the outcome of each step (Success/Failure).
- [x] The architecture supports adding real AWS/Stripe executors in the future.

## Implementation Details

### Architecture Pattern
```
ActionExecutor (interface)
    └── SimulatedActionExecutor (current - logs and simulates)
    └── AwsActionExecutor (future - Phase 2)
    └── KubernetesActionExecutor (future - Phase 2)
```

### Files Created
- `src/main/kotlin/com/example/incidentanalyst/remediation/RemediationResource.kt` - REST endpoints
- `src/main/resources/templates/remediation/progress-panel.html` - HTMX progress UI

### Files Modified
- `src/main/kotlin/com/example/incidentanalyst/diagnosis/DiagnosisModels.kt` - JSON serialization for structuredSteps
- `src/main/kotlin/com/example/incidentanalyst/remediation/RemediationModels.kt` - Added ExecutionStatus, RemediationProgress
- `src/main/kotlin/com/example/incidentanalyst/remediation/RemediationService.kt` - Added executeAllSteps(), getProgress()
- `src/main/kotlin/com/example/incidentanalyst/incident/IncidentResource.kt` - Updated remediate() method

### Key Types

```kotlin
sealed interface RemediationAction {
    data class RestartService(val serviceName: String) : RemediationAction
    data class ScaleCluster(val clusterName: String, val targetCount: Int) : RemediationAction
    data class ManualStep(val instructions: String) : RemediationAction
}

enum class StepStatus { PENDING, IN_PROGRESS, COMPLETED, FAILED }

data class RemediationStep(
    val id: String,
    val description: String,
    val action: RemediationAction?,
    val status: StepStatus,
    val outcome: String? = null
)

enum class ExecutionStatus { NOT_STARTED, IN_PROGRESS, COMPLETED, FAILED }

data class RemediationProgress(
    val incidentId: Long,
    val diagnosisId: Long,
    val steps: List<RemediationStep>,
    val currentStepIndex: Int,
    val status: ExecutionStatus,
    val startedAt: Instant?,
    val completedAt: Instant?,
    val errorMessage: String? = null
)
```

### REST Endpoints
```
POST /incidents/{incidentId}/remediation/start  - Start remediation execution
GET  /incidents/{incidentId}/remediation/progress - Get current progress (HTMX polling)
```

### UI Flow
1. User clicks "Apply All Steps"
2. POST to `/start` begins execution
3. Progress panel returned with HTMX polling every 2 seconds
4. Each step status updates (PENDING → IN_PROGRESS → COMPLETED)
5. Polling stops when status is COMPLETED or FAILED

### Decisions Made
1. **Persistence**: Reuse `diagnoses.structured_steps` column for Phase 1
2. **LLM Output**: Wrap text steps in `ManualStep` automatically
3. **Execution Model**: Background execution with HTMX polling
4. **Error Handling**: Continue on failure, mark failed steps

### Test Coverage
All 382 tests pass. New remediation functionality tested via existing integration tests.
