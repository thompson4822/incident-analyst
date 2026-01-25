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
- ✅ Comprehensive test suite: 83 tests (82 passing, 1 skipped due to test isolation)
- ✅ Tests cover service, repository, resource, models, and DTO mappings
- ✅ All acceptance criteria met

## Dependencies
- TICKET-002.
