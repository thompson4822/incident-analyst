# TICKET-001: Bootstrap dependencies and config (COMPLETED)

## Goal
Wire up core Quarkus/Kotlin dependencies and base configuration for persistence, RAG, AI, AWS SDK, and Qute.

## Scope
- [x] Update `pom.xml` with Quarkus BOM and required extensions (Hibernate Panache, Flyway, Postgres, Qute, LangChain4j + Ollama, AWS SDK v2, Scheduler).
- [x] Configure `src/main/resources/application.properties` for database, Flyway, and LangChain4j/Ollama defaults.
- [x] Keep configuration data-oriented and explicit (no magic defaults). 

## Acceptance criteria
- [x] Project builds with all required dependencies resolved.
- [x] Configuration properties exist for DB, Flyway, Ollama model, and scheduler.
- [x] No runtime secrets committed; placeholders documented.

## Progress
- ✅ Dependencies added in `pom.xml` (Panache, Flyway, Postgres, LangChain4j, Scheduler, Qute)
- ✅ Dev Services configured for pgvector image (`ankane/pgvector:latest`)
- ✅ LangChain4j model defaults set (`nomic-embed-text`, `qwen2.5:7b-instruct`)
- ✅ `quarkus.langchain4j.pgvector.dimension=768` set for current embedding model
- ✅ AWS SDK v2 dependencies for CloudWatch and CloudWatch Logs
- ✅ Application profile configuration for dev/prod environments

## Dependencies
- None.
