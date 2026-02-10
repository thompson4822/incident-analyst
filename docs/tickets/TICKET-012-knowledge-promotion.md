# TICKET-012: Knowledge Promotion Logic

## Overview
Implement the ability for humans to "Verify" an AI-generated diagnosis. This verification should trigger the "Promotion" of this knowledge into the RAG vector store, marking it as a "Gold Standard" for future incidents.

## Tasks
- [ ] Add `verifiedAt: Instant?` and `verifiedBy: String?` to `DiagnosisEntity` and `Diagnosis` domain model.
- [ ] Update `DiagnosisResource` to handle a `POST /diagnoses/{id}/verify` action.
- [ ] Enhance `EmbeddingService` to support embedding a `VerifiedDiagnosis` (combining Incident + Diagnosis text).
- [ ] Create an event-driven trigger (or direct call) that promotes a diagnosis to the vector store upon verification.

## Acceptance Criteria
- A user can click "Verify" on a diagnosis in the UI.
- The diagnosis state changes to `VERIFIED`.
- A new entry appears in the `incident_embeddings` table representing the verified knowledge.
- Tests confirm that verified diagnoses are persisted correctly.
