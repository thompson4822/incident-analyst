# TICKET-006: AWS CloudWatch ingestion

## Goal
Ingest CloudWatch alarms and logs into incidents using AWS SDK v2.

## Scope
- Configure CloudWatch and CloudWatch Logs clients.
- Implement scheduled ingestion job to pull new/updated alarms.
- Normalize AWS data into `IncidentEntity` fields.

## Acceptance criteria
- Scheduled job runs without crashing when AWS is unavailable.
- Ingestion persists incidents with meaningful severity and status codes.
- Errors return explicit results or logs, not thrown for control flow.

## Dependencies
- TICKET-001, TICKET-003.
