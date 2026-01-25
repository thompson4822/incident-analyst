# TICKET-001: Bootstrap dependencies and config

## Goal
Wire up core Quarkus/Kotlin dependencies and base configuration for persistence, RAG, AI, AWS SDK, and Qute.

## Scope
- Update `pom.xml` with Quarkus BOM and required extensions (Hibernate Panache, Flyway, Postgres, Qute, LangChain4j + Ollama, AWS SDK v2, Scheduler).
- Configure `src/main/resources/application.properties` for database, Flyway, and LangChain4j/Ollama defaults.
- Keep configuration data-oriented and explicit (no magic defaults). 

## Acceptance criteria
- Project builds with all required dependencies resolved.
- Configuration properties exist for DB, Flyway, Ollama model, and scheduler.
- No runtime secrets committed; placeholders documented.

## Progress
- Dependencies added in `pom.xml` (Panache, Flyway, Postgres, LangChain4j, Scheduler, Qute).
- Dev Services configured for pgvector image; LangChain4j model defaults set.
- `quarkus.langchain4j.pgvector.dimension` set for current embedding model.

## Dependencies
- None.
