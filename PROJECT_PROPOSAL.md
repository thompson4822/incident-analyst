# Project Proposal: Incident Analyst  
*(Panache, Vertical Slices, Data‑Oriented Design, Quarkus LangChain4j)*

## 1. Problem & Objectives

AWS CloudWatch generates large volumes of alarms, metrics, and logs, yet triage and diagnosis often remain manual, slow, and dependent on tribal knowledge. Incident Analyst aims to:

- Ingest and normalize CloudWatch incidents into a structured domain model stored in Postgres.  
- Use a RAG‑backed knowledge base (Postgres + pgvector) of incidents, diagnoses, and runbooks to power an intelligent agent that suggests likely root causes and remediations. [docs.quarkiverse](https://docs.quarkiverse.io/quarkus-langchain4j/dev/rag-pgvector-store.html)
- Provide a human‑in‑the‑loop UX so engineers can refine diagnoses and add prescriptive knowledge over time.

The system is **single‑tenant**, **AWS‑only**, built with **Quarkus/Kotlin/LangChain4j**, and designed as a **portfolio‑quality** project for a **small team**.

***

## 2. Architectural Overview

### 2.1 High‑level architecture

```text
[CloudWatch (AWS)]
   ↓ (AWS SDK v2)
[Quarkus/Kotlin – Incident Analyst]
   ├─ incident/   (entity + repo + service + resource + DTO)
   ├─ diagnosis/  (entity + repo + service + resource + DTO)
   ├─ runbook/    (entity + repo + service + resource + DTO)
   ├─ rag/        (embeddings + retrieval infra)
   ├─ agent/      (LLM orchestration, AI service)
   ├─ aws/        (CloudWatch ingestion)
   └─ web/        (Qute templates, layout)
        ↓
     [HTMX + DaisyUI UX]
```

### 2.2 Technology stack

| Layer            | Technology                                      | Notes |
|------------------|-------------------------------------------------|-------|
| Runtime          | Quarkus (JVM, Kotlin)                           | Kotlin + Panache for concise persistence. [quarkus](https://quarkus.io/guides/hibernate-orm-panache-kotlin) |
| Persistence      | Postgres + pgvector                             | Vector similarity for RAG. [docs.quarkiverse](https://docs.quarkiverse.io/quarkus-langchain4j/dev/rag-pgvector-store.html) |
| ORM              | Hibernate ORM with Panache Kotlin               | Panache entities + repositories. [quarkus](https://quarkus.io/guides/hibernate-orm-panache-kotlin) |
| LLM & RAG        | LangChain4j + Quarkus LangChain4j + Ollama + PgVectorEmbeddingStore | Local Qwen‑family 7–8B model, embeddings stored in Postgres. [quarkus](https://quarkus.io/extensions/io.quarkiverse.langchain4j/quarkus-langchain4j-ollama/) |
| AWS access       | AWS SDK v2 (CloudWatch & CloudWatch Logs)       | Lightweight, read‑only ingestion. |
| UI               | Qute templates + HTMX + DaisyUI (CDN)           | Server‑rendered UI with progressive enhancement. [the-main-thread](https://www.the-main-thread.com/p/htmx-quarkus-server-rendered-ui-java) |
| Tests            | JUnit 5, Mockito, H2                            | Mockito for mocks; H2 + Flyway in tests. [quarkus](https://quarkus.io/guides/hibernate-orm-panache-kotlin) |

The project is organized by **vertical slices**, and the domain is modeled with **data‑oriented programming**: meaningful types, ADTs for state and errors, and minimized use of exceptions.

***

## 3. Data‑Oriented Domain Design

### 3.1 Meaningful types

Core concepts use explicit types instead of raw primitives:

```kotlin
@JvmInline
value class IncidentId(val value: Long)

@JvmInline
value class DiagnosisId(val value: Long)

@JvmInline
value class RunbookFragmentId(val value: Long)

enum class Severity {
    CRITICAL, HIGH, MEDIUM, LOW, INFO
}

enum class Confidence {
    HIGH, MEDIUM, LOW, UNKNOWN
}
```

DTOs and services use these types to avoid mixing IDs and to make APIs self‑documenting.

### 3.2 State as ADTs

Lifecycle and verification state are modeled as sealed hierarchies:

```kotlin
sealed interface IncidentStatus {
    data object Open : IncidentStatus
    data object Acknowledged : IncidentStatus
    data class Diagnosed(val diagnosisId: DiagnosisId) : IncidentStatus
    data object Resolved : IncidentStatus
}

sealed interface DiagnosisVerification {
    data object Unverified : DiagnosisVerification
    data object VerifiedByHuman : DiagnosisVerification
}
```

Persistence uses simple code values (e.g., `status = "OPEN"`), with mapping functions converting to/from typed ADTs.

### 3.3 Entities vs domain models (Panache + DOP)

Entities: mutable, JPA‑friendly, minimal behavior, as recommended for Panache. [quarkus](https://quarkus.io/guides/hibernate-orm-panache-kotlin)

```kotlin
// incident/IncidentEntity.kt
@Entity
@Table(name = "incidents")
class IncidentEntity(
    @Id
    @GeneratedValue
    var id: Long? = null,

    var source: String = "",
    var title: String = "",

    @Column(columnDefinition = "text")
    var description: String = "",

    var severity: String = "",   // enum name
    var status: String = "OPEN", // status code

    var createdAt: Instant = Instant.now(),
    var updatedAt: Instant = Instant.now()
) : PanacheEntityBase()
```

Domain model: immutable, meaningful types and ADTs:

```kotlin
// incident/IncidentModels.kt
data class Incident(
    val id: IncidentId,
    val source: String,
    val title: String,
    val description: String,
    val severity: Severity,
    val status: IncidentStatus,
    val createdAt: Instant,
    val updatedAt: Instant
)
```

Mapping:

```kotlin
fun IncidentEntity.toDomain(): Incident = Incident(
    id = IncidentId(requireNotNull(id)),
    source = source,
    title = title,
    description = description,
    severity = Severity.valueOf(severity),
    status = when (status) {
        "OPEN" -> IncidentStatus.Open
        "ACK" -> IncidentStatus.Acknowledged
        "RESOLVED" -> IncidentStatus.Resolved
        else -> IncidentStatus.Open
    },
    createdAt = createdAt,
    updatedAt = updatedAt
)
```

Entities stay simple mutable bags; the domain logic uses the richer, typed models.

### 3.4 Explicit results instead of exceptions

Service operations use result ADTs rather than throwing for normal control flow:

```kotlin
// diagnosis/DiagnosisModels.kt
data class Diagnosis(
    val id: DiagnosisId,
    val incidentId: IncidentId,
    val rootCause: String,
    val steps: List<String>,
    val confidence: Confidence,
    val verification: DiagnosisVerification
)

sealed interface DiagnosisError {
    data object IncidentNotFound : DiagnosisError
    data object RetrievalFailed : DiagnosisError
    data object LlmUnavailable : DiagnosisError
    data class LlmResponseInvalid(val reason: String) : DiagnosisError
}

sealed interface DiagnosisResult {
    data class Success(val diagnosis: Diagnosis) : DiagnosisResult
    data class Failure(val error: DiagnosisError) : DiagnosisResult
}
```

This style makes all outcomes explicit and testable.

***

## 4. Vertical Slice Structure

Feature‑centric structure aligns with vertical slice architecture guidance. [bensampica](https://www.bensampica.com/post/verticalslice/)

### 4.1 Example package layout

- `com.example.incident`  
  - `IncidentEntity.kt`  
  - `IncidentRepository.kt`  
  - `IncidentModels.kt` (IncidentId, Incident, Severity, IncidentStatus)  
  - `IncidentService.kt`  
  - `IncidentResource.kt`  
  - `IncidentDto.kt`  
- `com.example.diagnosis`  
  - `DiagnosisEntity.kt`  
  - `DiagnosisRepository.kt`  
  - `DiagnosisModels.kt` (DiagnosisId, Diagnosis, Confidence, DiagnosisVerification, DiagnosisResult/DiagnosisError)  
  - `DiagnosisService.kt`  
  - `DiagnosisResource.kt`  
- `com.example.runbook`  
  - `RunbookFragmentEntity.kt`  
  - `RunbookFragmentRepository.kt`  
  - `RunbookModels.kt` (RunbookFragmentId, RunbookFragment)  
  - `RunbookService.kt`  
  - `RunbookResource.kt`  
- `com.example.rag`  
  - `IncidentEmbeddingEntity.kt`, `RunbookEmbeddingEntity.kt`  
  - `IncidentEmbeddingRepository.kt`, `RunbookEmbeddingRepository.kt`  
  - `EmbeddingService.kt` (Ollama embeddings)  
  - `RetrievalService.kt` (similarity search + context assembly)  
- `com.example.agent`  
  - `IncidentAnalystAgent.kt` (AI service interface, `@RegisterAiService`)  
  - `IncidentDiagnosisService.kt` (RAG + AI + persistence)  
  - Optional `IncidentTools.kt` (`@Tool` methods)  
- `com.example.aws`  
  - `CloudWatchClientConfig.kt`  
  - `CloudWatchIngestionService.kt`  
- `com.example.web`  
  - Shared Qute layouts and helper view models  

Each slice owns its entity, repository, service, resource, and DTOs.

***

## 5. Persistence & RAG

### 5.1 Panache entities and repositories

Entities use mutable `var` fields; repositories implement `PanacheRepository`. [quarkus](https://quarkus.io/guides/hibernate-orm-panache)

```kotlin
// incident/IncidentRepository.kt
@ApplicationScoped
class IncidentRepository : PanacheRepository<IncidentEntity> {

    fun findRecent(limit: Int): List<IncidentEntity> =
        find("order by createdAt desc").page(0, limit).list()
}
```

### 5.2 pgvector & LangChain4j integration

Use the Quarkus LangChain4j pgvector store to persist embeddings. [github](https://github.com/langchain4j/langchain4j/blob/main/docs/docs/integrations/embedding-stores/pgvector.md)

- Flyway migrations enable `pgvector` and define `vector` columns.  
- `EmbeddingService` uses a local embedding model via LangChain4j and stores embeddings in Postgres using `PgVectorEmbeddingStore`. [docs.quarkiverse](https://docs.quarkiverse.io/quarkus-langchain4j/dev/rag-pgvector-store.html)
- `RetrievalService` queries top‑k similar incidents and runbook fragments via similarity search and returns a `RetrievalContext`.

```kotlin
// rag/RetrievalModels.kt
data class RetrievalContext(
    val similarIncidents: List<Incident>,
    val runbookFragments: List<RunbookFragment>
)

interface RetrievalService {
    fun retrieveContext(incident: Incident): RetrievalContext?
}
```

***

## 6. Ingestion & AWS Integration

- Use AWS SDK v2:
  - `CloudWatchClient` for alarms/metrics.  
  - `CloudWatchLogsClient` for logs. [docs.aws.amazon](https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/Investigations.html)
- `CloudWatchIngestionService`:
  - Scheduled Quarkus job (e.g., every minute).  
  - Pulls new/updated alarms and relevant logs.  
  - Normalizes them into `IncidentEntity` and persists via `IncidentRepository`.  

This avoids Quarkus AWS extensions and keeps CloudWatch access lightweight and read‑only.

***

## 7. Agent & AI Integration (Quarkus LangChain4j)

### 7.1 AI service with `@RegisterAiService`

The primary LLM integration is a declarative AI service using Quarkus LangChain4j’s `@RegisterAiService`. [quarkus](https://quarkus.io/blog/quarkus-meets-langchain4j/)

```kotlin
// agent/IncidentAnalystAgent.kt
package com.example.agent

import io.quarkiverse.langchain4j.RegisterAiService
import dev.langchain4j.service.UserMessage

@RegisterAiService
interface IncidentAnalystAgent {

    @UserMessage(
        """
        You are an AWS incident analyst.
        Given the incident and context below, respond with a JSON object:
        {
          "rootCause": "...",
          "steps": ["...", "..."],
          "confidence": "HIGH|MEDIUM|LOW"
        }

        Incident:
        {incident}

        Context:
        {context}
        """
    )
    fun proposeDiagnosis(incident: String, context: String): String
}
```

Quarkus generates an implementation and wires it to the configured Ollama model (e.g., Qwen‑family 7–8B quantized for 8 GB VRAM), via LangChain4j’s AI service integration. [quarkus](https://quarkus.io/extensions/io.quarkiverse.langchain4j/quarkus-langchain4j-ollama/)

### 7.2 Data‑oriented diagnosis service

`IncidentDiagnosisService` coordinates retrieval and AI usage and returns `DiagnosisResult`:

```kotlin
// agent/IncidentDiagnosisService.kt
package com.example.agent

import com.example.incident.IncidentId
import com.example.incident.IncidentRepository
import com.example.rag.RetrievalService
import com.example.diagnosis.*

class IncidentDiagnosisService(
    private val incidentRepository: IncidentRepository,
    private val retrievalService: RetrievalService,
    private val aiService: IncidentAnalystAgent,
    private val diagnosisRepository: DiagnosisRepository
) {

    fun diagnose(incidentId: IncidentId): DiagnosisResult {
        val entity = incidentRepository.findByIdOrNull(incidentId.value)
            ?: return DiagnosisResult.Failure(DiagnosisError.IncidentNotFound)

        val incident = entity.toDomain()

        val context = retrievalService.retrieveContext(incident)
            ?: return DiagnosisResult.Failure(DiagnosisError.RetrievalFailed)

        val incidentText = renderIncident(incident)
        val contextText = renderContext(context)

        val raw = try {
            aiService.proposeDiagnosis(incidentText, contextText)
        } catch (e: Exception) {
            return DiagnosisResult.Failure(DiagnosisError.LlmUnavailable)
        }

        val diagnosis = parseDiagnosisJson(raw, incidentId)
            ?: return DiagnosisResult.Failure(
                DiagnosisError.LlmResponseInvalid("Invalid or incomplete JSON")
            )

        diagnosisRepository.persist(diagnosis.toEntity())
        return DiagnosisResult.Success(diagnosis)
    }
}
```

- `renderIncident` / `renderContext`: pure functions turning domain models into prompt text or compact JSON.  
- `parseDiagnosisJson`: pure function mapping LLM JSON to the typed `Diagnosis`.  
- No domain exceptions; errors are explicit via `DiagnosisError`.

### 7.3 Optional `@Tool` usage (future)

Tools can be added later to expose selected capabilities to the model. [quarkus](https://quarkus.io/quarkus-workshop-langchain4j/section-1/step-07/)

```kotlin
// agent/IncidentTools.kt
package com.example.agent

import com.example.incident.IncidentRepository
import dev.langchain4j.agent.tool.Tool
import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
class IncidentTools(
    private val incidentRepository: IncidentRepository
) {

    @Tool("Fetch recent incidents by source")
    fun fetchRecentIncidentsBySource(source: String): String {
        val incidents = incidentRepository.find("source", source).page(0, 5).list()
        return serializeIncidents(incidents)
    }
}
```

The proposal treats tools as a **future enhancement**; initial versions rely on explicit retrieval in `RetrievalService`.

***

## 8. UX: Qute + HTMX + DaisyUI

- Qute templates (`templates/`) provide layout and per‑slice pages (e.g., `incident/list.html`, `incident/detail.html`). [the-main-thread](https://www.the-main-thread.com/p/htmx-quarkus-server-rendered-ui-java)
- HTMX enhances interactivity without a SPA:
  - “Diagnose” button triggers LLM call and updates a diagnosis panel.  
  - Verification and runbook edits are partial updates.  
- DaisyUI via CDN provides styling for tables, cards, buttons. [daisyui](https://daisyui.com/htmx-component-library/?lang=en)

Example:

```html
<button
  hx-post="/incidents/{id}/diagnose"
  hx-target="#diagnosis-panel"
  class="btn btn-primary">
  Run Diagnosis
</button>

<div id="diagnosis-panel">
  <!-- server-rendered diagnosis partial -->
</div>
```

***

## 9. Testing Strategy

- JUnit 5 + Quarkus test for slice and integration tests. [opensource](https://opensource.com/article/22/3/simplify-java-persistence-implementation-kotlin-quarkus)
- Mockito for mocking AWS SDK clients, embedding services, and AI services.  
- H2 with Flyway in tests to mirror schema.  
- Slice‑focused tests:
  - `IncidentService` for ingestion and mapping.  
  - `RetrievalService` for similarity and context assembly using seeded data.  
  - `IncidentDiagnosisService` for result ADTs and error handling.

***

## 10. Implementation Plan

1. **Bootstrap & dependencies**  
   - Quarkus project with Kotlin, Panache, Flyway, Postgres, pgvector, LangChain4j + Quarkus LangChain4j + Ollama, AWS SDK, Qute, HTMX, DaisyUI.

2. **Entities, repositories, migrations**  
   - Create Panache entities with `var` fields for `IncidentEntity`, `DiagnosisEntity`, `RunbookFragmentEntity`, and embeddings. [quarkus](https://quarkus.io/guides/hibernate-orm-panache-kotlin)
   - Add Panache repositories and Flyway migrations (including `pgvector`). [docs.quarkiverse](https://docs.quarkiverse.io/quarkus-langchain4j/dev/rag-pgvector-store.html)

3. **Data‑oriented domain models & ADTs**  
   - Implement value types, enums, sealed hierarchies for state and errors.  
   - Add mapping functions between entities and domain models.

4. **AWS ingestion (aws slice)**  
   - Implement CloudWatch clients and `CloudWatchIngestionService` to persist incidents.

5. **RAG (rag slice)**  
   - Configure `PgVectorEmbeddingStore` and `EmbeddingService`. [github](https://github.com/langchain4j/langchain4j/blob/main/docs/docs/integrations/embedding-stores/pgvector.md)
   - Implement `RetrievalService` returning `RetrievalContext`.

6. **Agent (agent slice)**  
   - Configure Ollama model and Quarkus LangChain4j.  
   - Implement `IncidentAnalystAgent` with `@RegisterAiService` and `IncidentDiagnosisService`.  
   - Optional: introduce `@Tool` methods later.

7. **UX & feedback loop**  
   - Build Qute + HTMX pages for incident list/detail and diagnosis UX.  
   - Support editing/verifying diagnoses and adding runbook fragments; re‑embed updated text.
