# TICKET-012: Knowledge Promotion Logic (COMPLETED)

## Overview
Implement the ability for humans to "Verify" an AI-generated diagnosis. This verification should trigger the "Promotion" of this knowledge into the RAG vector store, marking it as a "Gold Standard" for future incidents.

## Tasks
- [x] Add `verifiedAt: Instant?` and `verifiedBy: String?` to `DiagnosisEntity` and `Diagnosis` domain model.
- [x] Update `DiagnosisResource` to handle a `POST /diagnoses/{id}/verify` action.
- [x] Enhance `EmbeddingService` to support embedding a `VerifiedDiagnosis` (combining Incident + Diagnosis text).
- [x] Create an event-driven trigger (or direct call) that promotes a diagnosis to the vector store upon verification.

## Acceptance Criteria
- [x] A user can click "Verify" on a diagnosis in the UI.
- [x] The diagnosis state changes to `VERIFIED`.
- [x] A new entry appears in the `incident_embeddings` table representing the verified knowledge.
- [x] Tests confirm that verified diagnoses are persisted correctly.
