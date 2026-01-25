# TICKET-010: Testing strategy

## Goal
Create reliable tests for core slices and integrations using data-oriented results.

## Scope
- Unit tests for domain mapping and result ADTs.
- Quarkus tests for repositories and services with H2 + Flyway.
- Mock AWS SDK and AI services for deterministic tests.

## Acceptance criteria
- Tests cover incident, diagnosis, retrieval, and diagnosis pipeline flows.
- Failure modes are asserted via explicit result ADTs.
- Test data setup mirrors production schema.

## Dependencies
- TICKET-002 through TICKET-009.
