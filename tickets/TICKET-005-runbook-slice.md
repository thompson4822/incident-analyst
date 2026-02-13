# TICKET-005: Runbook slice core (COMPLETED)

## Goal
Support listing and editing runbook fragments with data-oriented models and endpoints.

## Scope
- [x] Runbook models: `RunbookFragmentId`, `RunbookFragment`.
- [x] Mapping functions between `RunbookFragmentEntity` and domain model.
- [x] Repository, service, and resource endpoints for list and update.
- [x] DTOs for web/API responses.

## Acceptance criteria
- [x] Runbook endpoints return DTOs only.
- [x] Domain models use meaningful types.
- [x] Updates trigger persistence operations cleanly.

## Progress
- ✅ RunbookFragmentRepository with findRecent() query
- ✅ RunbookService with listRecent(), getById(), updateFragment() methods
- ✅ RunbookFragmentResult ADT with Success and Failure cases
- ✅ RunbookFragmentError with NotFound and ValidationFailed cases
- ✅ GET /runbooks endpoint uses service layer and toResponseDto() helper
- ✅ GET /runbooks/{id} endpoint with proper error type discrimination
- ✅ PUT /runbooks/{id} endpoint with validation and proper HTTP status codes
- ✅ RunbookFragmentUpdateRequestDto created
- ✅ toResponseDto() helper function to eliminate code duplication
- ✅ Validation in service layer (blank title/content checks)
- ✅ Error responses include MediaType.APPLICATION_JSON
- ✅ Proper HTTP status code mapping (200, 404, 400)
- ✅ 83 tests covering service, repository, resource, models, and DTO mappings

## Implementation Details

### Files Created
- `src/main/kotlin/com/example/incidentanalyst/runbook/RunbookModels.kt`
- `src/main/kotlin/com/example/incidentanalyst/runbook/RunbookFragmentEntity.kt`
- `src/main/kotlin/com/example/incidentanalyst/runbook/RunbookFragmentRepository.kt`
- `src/main/kotlin/com/example/incidentanalyst/runbook/RunbookService.kt`
- `src/main/kotlin/com/example/incidentanalyst/runbook/RunbookResource.kt`
- `src/main/kotlin/com/example/incidentanalyst/runbook/RunbookDto.kt`

### Domain Types
- `RunbookFragmentId` - value class for runbook IDs
- `RunbookFragment` - domain model with title, content, category
- `RunbookFragmentResult` - sealed interface (Success, Failure)
- `RunbookFragmentError` - sealed interface (NotFound, ValidationFailed)

## Dependencies
- TICKET-002.
