# TICKET-014: Resolution Capture Loop

## Overview
Ensure that every resolved incident captures the "Actual Fix" (Resolution). This is the most valuable data point for the learning loop.

## Tasks
- [ ] Add `resolutionText: String?` to `IncidentEntity` and `Incident` domain model.
- [ ] Update the "Resolve" UI to show a modal or text area asking "What was the actual fix?".
- [ ] Update `IncidentResource.resolve()` to accept this text.
- [ ] Trigger a "Knowledge Promotion" for the Resolution text into the RAG store.

## Acceptance Criteria
- An incident cannot be resolved without providing a brief resolution description.
- The resolution is stored in the database.
- The resolution is searchable in the RAG pipeline for future similar incidents.
