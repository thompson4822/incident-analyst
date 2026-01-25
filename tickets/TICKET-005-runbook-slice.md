# TICKET-005: Runbook slice core

## Goal
Support listing and editing runbook fragments with data-oriented models and endpoints.

## Scope
- Runbook models: `RunbookFragmentId`, `RunbookFragment`.
- Mapping functions between `RunbookFragmentEntity` and domain model.
- Repository, service, and resource endpoints for list and update.
- DTOs for web/API responses.

## Acceptance criteria
- Runbook endpoints return DTOs only.
- Domain models use meaningful types.
- Updates trigger persistence operations cleanly.

## Dependencies
- TICKET-002.
