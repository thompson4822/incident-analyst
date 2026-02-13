# TICKET-004: Diagnosis slice core (COMPLETED)

## Goal
Define data-oriented diagnosis models and endpoints for listing and verification status.

## Scope
- [x] Diagnosis models: `DiagnosisId`, `Confidence`, `DiagnosisVerification`, `DiagnosisError`, `DiagnosisResult`.
- [x] Mapping functions between `DiagnosisEntity` and domain model.
- [x] `DiagnosisRepository` and `DiagnosisService` with explicit results.
- [x] `DiagnosisResource` for list and verification update endpoints.

## Acceptance criteria
- [x] Domain models use ADTs and meaningful IDs.
- [x] Resources map results to DTOs and HTTP codes without exceptions.
- [x] Verification state is represented as an ADT in the domain.

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
- ✅ 85 tests passing across 5 test files

## Implementation Details

### Files Created
- `src/main/kotlin/com/example/incidentanalyst/diagnosis/DiagnosisModels.kt`
- `src/main/kotlin/com/example/incidentanalyst/diagnosis/DiagnosisEntity.kt`
- `src/main/kotlin/com/example/incidentanalyst/diagnosis/DiagnosisRepository.kt`
- `src/main/kotlin/com/example/incidentanalyst/diagnosis/DiagnosisService.kt`
- `src/main/kotlin/com/example/incidentanalyst/diagnosis/DiagnosisResource.kt`
- `src/main/kotlin/com/example/incidentanalyst/diagnosis/DiagnosisDto.kt`

### Domain Types
- `DiagnosisId` - value class for diagnosis IDs
- `Confidence` - enum (HIGH, MEDIUM, LOW)
- `DiagnosisVerification` - sealed interface (VerifiedByHuman, Unverified)
- `DiagnosisError` - sealed interface (NotFound, UpdateFailed, IncidentNotFound, RetrievalFailed, LlmUnavailable, LlmResponseInvalid)
- `DiagnosisResult` - sealed interface (Success, Failure)

## Dependencies
- TICKET-002.
