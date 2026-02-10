# TICKET-015: Weighted RAG Retrieval (COMPLETED)

## Overview
Update the `RetrievalService` to prioritize "Verified Knowledge" (past resolutions and verified diagnoses) over general runbooks or unverified incidents.

## Tasks
- [x] Add a `source_type` (e.g., VERIFIED_CASE, RUNBOOK, RAW_INCIDENT) to the embedding metadata.
- [x] Update `RetrievalService` to perform a multi-stage search or apply a "boost" to `VERIFIED_CASE` matches.
- [x] Update the AI prompt to clearly distinguish between "This happened before and was fixed by X" vs "This runbook suggests Y".

## Acceptance Criteria
- [x] Verified past resolutions appear at the top of the AI context.
- [x] The AI explicitly references past verified fixes in its proposed diagnosis.
- [x] Retrieval scores are adjusted based on the "Quality" of the source.
