# TICKET-002: Database schema and migrations

## Goal
Define the initial Postgres schema for incidents, diagnoses, runbook fragments, and embeddings with pgvector enabled.

## Scope
- Create or refine Flyway migration(s) to add tables and constraints.
- Ensure pgvector extension is enabled and vector columns exist.
- Align column names with entity mappings.

## Acceptance criteria
- Flyway runs cleanly on a fresh database.
- Tables exist with expected columns and constraints.
- Vector columns are present for incident and runbook embeddings.

## Progress
- `V1__init.sql` updated with snake_case columns and `TIMESTAMPTZ` types.
- Entity mappings adjusted to match column names used in the migration.

## Dependencies
- TICKET-001.
