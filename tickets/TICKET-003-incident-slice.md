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

## Progress
- ✅ Domain models (IncidentId, Severity, IncidentStatus, Incident) are well-defined with ADTs
- ✅ IncidentEntity properly mapped to snake_case columns
- ✅ IncidentRepository with findRecent query implemented
- ✅ IncidentService with listRecent and getById methods, using explicit IncidentResult type
- ✅ IncidentResource with GET / (list) and GET /{id} (read) endpoints
- ✅ Proper handling of DIAGNOSED status with diagnosisId parsing
- ✅ Endpoints return DTOs only, not entities
- ✅ Explicit result types (IncidentResult.Success/Failure) instead of exceptions

## Dependencies
- TICKET-002.
