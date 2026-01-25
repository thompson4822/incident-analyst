# TICKET-003: Incident slice core

## Goal
Provide data-oriented incident domain models and HTTP endpoints for listing and reading incidents.

## Scope
- Incident models: `IncidentId`, `Severity`, `IncidentStatus`, and `Incident`.
- Mapping functions between `IncidentEntity` and domain model.
- `IncidentRepository` queries for recent incidents.
- `IncidentService` for list/read operations returning domain models.
- `IncidentResource` HTTP endpoints and DTOs.

## Acceptance criteria
- Endpoints return DTOs without exposing persistence types.
- Domain models use meaningful types and ADTs.
- No exceptions used for normal control flow.

## Dependencies
- TICKET-002.
