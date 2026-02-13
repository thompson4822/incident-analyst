# TICKET-003: Incident slice core (COMPLETED)

## Goal
Provide data-oriented incident domain models and HTTP endpoints for listing and reading incidents.

## Scope
- [x] Incident models: `IncidentId`, `Severity`, `IncidentStatus`, and `Incident`.
- [x] Mapping functions between `IncidentEntity` and domain model.
- [x] `IncidentRepository` queries for recent incidents.
- [x] `IncidentService` for list/read operations returning domain models.
- [x] `IncidentResource` HTTP endpoints and DTOs.

## Acceptance criteria
- [x] Endpoints return DTOs without exposing persistence types.
- [x] Domain models use meaningful types and ADTs.
- [x] No exceptions used for normal control flow.

## Progress
- ✅ Domain models (IncidentId, Severity, IncidentStatus, Incident) are well-defined with ADTs
- ✅ IncidentEntity properly mapped to snake_case columns
- ✅ IncidentRepository with findRecent query implemented
- ✅ IncidentService with listRecent and getById methods, using explicit IncidentResult type
- ✅ IncidentResource with GET / (list) and GET /{id} (read) endpoints
- ✅ Proper handling of DIAGNOSED status with diagnosisId parsing
- ✅ Endpoints return DTOs only, not entities
- ✅ Explicit result types (IncidentResult.Success/Failure) instead of exceptions
- ✅ ~70 tests covering service, repository, resource, and models

## Implementation Details

### Files Created
- `src/main/kotlin/com/example/incidentanalyst/incident/IncidentModels.kt`
- `src/main/kotlin/com/example/incidentanalyst/incident/IncidentEntity.kt`
- `src/main/kotlin/com/example/incidentanalyst/incident/IncidentRepository.kt`
- `src/main/kotlin/com/example/incidentanalyst/incident/IncidentService.kt`
- `src/main/kotlin/com/example/incidentanalyst/incident/IncidentResource.kt`
- `src/main/kotlin/com/example/incidentanalyst/incident/IncidentDto.kt`

### Domain Types
- `IncidentId` - value class for incident IDs
- `Severity` - enum (CRITICAL, HIGH, MEDIUM, LOW, INFO)
- `IncidentStatus` - sealed interface (Open, Acknowledged, Diagnosed, Resolved)
- `IncidentResult` - sealed interface (Success, NotFound)

## Dependencies
- TICKET-002.
