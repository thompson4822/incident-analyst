# Incident Analyst Plan

## Purpose
Build a Quarkus/Kotlin application that ingests AWS CloudWatch incidents into Postgres, augments them with RAG-backed context, and produces AI-assisted diagnoses with a human-in-the-loop UI. The system is single-tenant, AWS-only, and uses data-oriented programming (meaningful types, ADTs, explicit results).

## Guiding principles
- Data-oriented domain types and ADTs (no primitive soup).
- Explicit result types over exceptions in core flows.
- Vertical slices own entity/repo/service/resource/DTOs.
- RAG and agent logic is pure-ish and testable.
- UI is server-rendered Qute + HTMX, progressive enhancement.

## Feature map (tickets)
1) Bootstrap project dependencies and core configuration. (TICKET-001)
2) Persistence schema and migrations with pgvector. (TICKET-002)
3) Incident slice: list/detail, DTOs, mappings, endpoints. (TICKET-003)
4) Diagnosis slice: list/verify, DTOs, mappings, endpoints. (TICKET-004)
5) Runbook slice: list/edit, DTOs, endpoints. (TICKET-005)
6) AWS ingestion pipeline from CloudWatch. (TICKET-006)
7) RAG embeddings + retrieval context assembly. (TICKET-007)
8) Agent orchestration and diagnosis pipeline. (TICKET-008)
9) UX with Qute + HTMX for key flows. (TICKET-009)
10) Testing strategy across slices and integrations. (TICKET-010)

## Milestones
- Milestone 1: Project bootstrap and schema (TICKET-001, TICKET-002).
- Milestone 2: Core slices in place (TICKET-003, TICKET-004, TICKET-005).
- Milestone 3: Ingestion + RAG + Agent (TICKET-006, TICKET-007, TICKET-008).
- Milestone 4: UX and interaction loop (TICKET-009).
- Milestone 5: Tests and CI readiness (TICKET-010).

## Current status
- Quarkus dev mode boots with Dev Services using pgvector image.
- Base configuration for LangChain4j and pgvector dimension set for the current embedding model.
- Entity mappings aligned to snake_case columns and identity strategy; schema migration updated.
