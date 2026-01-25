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

## Dependencies
- TICKET-002.
