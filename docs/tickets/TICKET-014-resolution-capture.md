# TICKET-014: Resolution Capture Loop (COMPLETED)

## Overview
Ensure that every resolved incident captures the "Actual Fix" (Resolution). This is the most valuable data point for the learning loop.

## Tasks
- [x] Add `resolutionText: String?` to `IncidentEntity` and `Incident` domain model.
- [x] Update the "Resolve" UI to show a modal or text area asking "What was the actual fix?".
- [x] Update `IncidentResource.resolve()` to accept this text.
- [x] Trigger a "Knowledge Promotion" for the Resolution text into the RAG store.

## Acceptance Criteria
- [x] An incident cannot be resolved without providing a brief resolution description.
- [x] The resolution is stored in the database.
- [x] The resolution is searchable in the RAG pipeline for future similar incidents.
