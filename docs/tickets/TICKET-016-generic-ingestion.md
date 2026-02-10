# TICKET-016: Generic Webhook Ingestion

## Overview
Implement a generic webhook ingestion endpoint that allows external systems (Sentry, GitHub, custom scripts) to push incidents into the platform. This proves the domain-agnostic nature of the "Generic Operational Brain."

## Tasks
- [ ] Create a `WebhookIngestionResource` with a `POST /ingest/webhook` endpoint.
- [ ] Define a `GenericIncidentRequestDto` that maps common incident fields (title, description, severity, source).
- [ ] Implement a mapping layer to convert the generic JSON payload into the core `Incident` domain model.
- [ ] Add security (e.g., a simple API key check via header) to the webhook endpoint.
- [ ] Update the UI to show the source of these incidents correctly.

## Acceptance Criteria
- External systems can create incidents via a simple `curl` command.
- The system correctly identifies the source as provided in the webhook payload.
- Ingested incidents are immediately visible on the dashboard and searchable.
- AI diagnosis works for these incidents using the same RAG pipeline.
