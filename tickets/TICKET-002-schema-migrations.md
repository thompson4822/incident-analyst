# TICKET-002: Database schema and migrations (COMPLETED)

## Goal
Define the initial Postgres schema for incidents, diagnoses, runbook fragments, and embeddings with pgvector enabled.

## Scope
- [x] Create or refine Flyway migration(s) to add tables and constraints.
- [x] Ensure pgvector extension is enabled and vector columns exist.
- [x] Align column names with entity mappings.

## Acceptance criteria
- [x] Flyway runs cleanly on a fresh database.
- [x] Tables exist with expected columns and constraints.
- [x] Vector columns are present for incident and runbook embeddings.

## Progress
- ✅ `V1__init.sql` created with snake_case columns and `TIMESTAMPTZ` types
- ✅ Tables: `incidents`, `diagnoses`, `runbook_fragments`, `incident_embeddings`
- ✅ pgvector extension enabled
- ✅ Entity mappings adjusted to match column names used in the migration
- ✅ H2 test schema in `db/migration-h2/V1__init.sql` for testing
- ✅ Dev profile Flyway config: `%dev.quarkus.flyway.clean-at-start=true`

## Implementation Details

### Tables Created
- **incidents**: id, source, title, description, severity, status, created_at, updated_at, resolved_at, resolution_text
- **diagnoses**: id, incident_id, suggested_root_cause, remediation_steps, confidence, verification, verified_at, verified_by, created_at
- **runbook_fragments**: id, title, content, category, created_at, updated_at
- **incident_embeddings**: id, embedding_id, content, embedding (vector), metadata, created_at

### Key Schema Features
- Snake_case column names throughout
- `TIMESTAMPTZ` for all timestamp columns
- Foreign key constraints with proper relationships
- pgvector column type for embeddings

## Dependencies
- TICKET-001.
