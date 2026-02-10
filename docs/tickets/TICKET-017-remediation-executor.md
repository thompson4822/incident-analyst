# TICKET-017: Remediation Executor (Phase 1)

## Overview
Start building the "Action Executor" system to turn AI-suggested remediation steps into real, executable actions. Phase 1 focuses on the infrastructure and a "Simulated" executor.

## Tasks
- [ ] Define a `RemediationAction` sealed interface (e.g., `RestartService`, `ScaleCluster`, `ManualStep`).
- [ ] Create a `RemediationService` that manages the execution of a plan.
- [ ] Implement a `SimulatedActionExecutor` that logs actions and simulates latency.
- [ ] Update the `Diagnosis` model to support "Structured Steps" (Step Text + Optional Action).
- [ ] Update the UI to show a "Live Progress" view when a remediation plan is being applied.

## Acceptance Criteria
- Clicking "Apply All Steps" triggers a background execution process.
- The UI updates in real-time (via HTMX/SSE) as each step completes.
- The system logs the outcome of each step (Success/Failure).
- The architecture supports adding real AWS/Stripe executors in the future.
