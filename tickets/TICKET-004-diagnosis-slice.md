# TICKET-004: Diagnosis slice core

## Goal
Define data-oriented diagnosis models and endpoints for listing and verification status.

## Scope
- Diagnosis models: `DiagnosisId`, `Confidence`, `DiagnosisVerification`, `DiagnosisError`, `DiagnosisResult`.
- Mapping functions between `DiagnosisEntity` and domain model.
- `DiagnosisRepository` and `DiagnosisService` with explicit results.
- `DiagnosisResource` for list and verification update endpoints.

## Acceptance criteria
- Domain models use ADTs and meaningful IDs.
- Resources map results to DTOs and HTTP codes without exceptions.
- Verification state is represented as an ADT in the domain.

## Progress
- ✅ DiagnosisRepository with findRecent() query added
- ✅ DiagnosisService with listRecent(), getById(), updateVerification() methods
- ✅ DiagnosisResult used for explicit error handling
- ✅ GET /diagnoses endpoint returns list with toResponseDto() helper
- ✅ GET /diagnoses/{id} endpoint with ID validation and proper HTTP codes
- ✅ PUT /diagnoses/{id}/verification endpoint for verification updates
- ✅ DiagnosisVerificationUpdateRequestDto created
- ✅ HTTP status code mapping: 200 (Success), 404 (NotFound), 500 (UpdateFailed), 400 (Invalid ID)
- ✅ toResponseDto() extension function eliminates code duplication
- ✅ DiagnosisError ADT expanded with NotFound and UpdateFailed cases
- ✅ Comprehensive test suite: 85 tests passing across 5 test files
- ✅ Tests cover service, repository, resource, models, and DTO mappings

## Dependencies
- TICKET-002.
