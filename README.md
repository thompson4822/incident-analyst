# Incident Analyst

Incident Analyst is a Quarkus/Kotlin application that ingests AWS CloudWatch incidents into Postgres, enriches them with RAG-backed context, and produces AI-assisted diagnoses with a human-in-the-loop UI. The design follows data-oriented programming principles: meaningful types, ADTs for state and errors, and explicit result types over exceptions.

## Stack
- Quarkus (Kotlin), Panache, Flyway
- Postgres + pgvector
- LangChain4j + Ollama
- Qute + HTMX + DaisyUI
- AWS SDK v2 (CloudWatch, CloudWatch Logs)

## Quickstart (local)
1) Start Postgres (pgvector) via Docker:
   - `docker compose up -d`
2) Configure local settings in `src/main/resources/application.properties`.
3) Run the app:
   - `./mvnw quarkus:dev`

## Notes
- This is a single-tenant, AWS-only system intended as a portfolio-quality project.
- Configuration values are placeholders and should be set per environment.
