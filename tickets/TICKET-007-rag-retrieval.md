# TICKET-007: RAG embeddings and retrieval

## Goal
Embed incident and runbook text in pgvector and retrieve similar context for diagnoses.

## Scope
- Configure LangChain4j embedding model and pgvector store.
- Implement `EmbeddingService` to create and persist embeddings.
- Implement `RetrievalService` to return `RetrievalContext` with top-k matches.

## Acceptance criteria
- Embeddings are stored and queryable from Postgres.
- Retrieval returns deterministic context for seeded test data.
- Service interfaces are data-oriented and testable.

## Dependencies
- TICKET-001, TICKET-002, TICKET-005.
