## Project Directory Structure
---
Here’s a concrete, vertical‑slice directory structure tailored to the proposal, assuming:

- GroupId: `com.example`  
- ArtifactId: `incident-analyst`  
- Kotlin sources under `src/main/kotlin` and tests under `src/test/kotlin`. [quarkus](https://quarkus.io/guides/kotlin)

## Top-level layout

```text
incident-analyst/
  pom.xml
  src/
    main/
      kotlin/
        com/
          example/
            incidentanalyst/
              incident/
              diagnosis/
              runbook/
              rag/
              agent/
              aws/
              web/
              config/
      resources/
        application.properties
        db/
          migration/
            V1__init.sql
        templates/
          layout/
          incident/
          diagnosis/
          runbook/
    test/
      kotlin/
        com/
          example/
            incidentanalyst/
              incident/
              diagnosis/
              runbook/
              rag/
              agent/
              aws/
```

## Package-by-slice details

### `incident` slice

```text
src/main/kotlin/com/example/incidentanalyst/incident/
  IncidentEntity.kt
  IncidentRepository.kt
  IncidentModels.kt
  IncidentService.kt
  IncidentResource.kt
  IncidentDto.kt
```

- `IncidentEntity.kt`: Panache entity with `var` fields.  
- `IncidentModels.kt`: `IncidentId`, `Incident`, `Severity`, `IncidentStatus` (sealed).  
- `IncidentRepository.kt`: `PanacheRepository<IncidentEntity>`.  
- `IncidentService.kt`: data‑oriented operations on `Incident`.  
- `IncidentResource.kt`: REST endpoints for listing/reading incidents.  
- `IncidentDto.kt`: request/response DTOs for the HTTP API.

Tests:

```text
src/test/kotlin/com/example/incidentanalyst/incident/
  IncidentServiceTest.kt
  IncidentRepositoryTest.kt
```

### `diagnosis` slice

```text
src/main/kotlin/com/example/incidentanalyst/diagnosis/
  DiagnosisEntity.kt
  DiagnosisRepository.kt
  DiagnosisModels.kt
  DiagnosisService.kt
  DiagnosisResource.kt
  DiagnosisDto.kt
```

- `DiagnosisModels.kt`: `DiagnosisId`, `Diagnosis`, `Confidence`, `DiagnosisVerification`, `DiagnosisError`, `DiagnosisResult`.  
- `DiagnosisService.kt`: domain‑level operations (e.g., verifying a diagnosis).  
- `DiagnosisResource.kt`: HTTP endpoints for listing/confirming diagnoses.

Tests:

```text
src/test/kotlin/com/example/incidentanalyst/diagnosis/
  DiagnosisServiceTest.kt
```

### `runbook` slice

```text
src/main/kotlin/com/example/incidentanalyst/runbook/
  RunbookFragmentEntity.kt
  RunbookFragmentRepository.kt
  RunbookModels.kt
  RunbookService.kt
  RunbookResource.kt
  RunbookDto.kt
```

Represents prescriptive guidance and manual notes analysts add.

### `rag` slice

```text
src/main/kotlin/com/example/incidentanalyst/rag/
  IncidentEmbeddingEntity.kt
  RunbookEmbeddingEntity.kt
  IncidentEmbeddingRepository.kt
  RunbookEmbeddingRepository.kt
  EmbeddingService.kt
  RetrievalModels.kt
  RetrievalService.kt
```

- `EmbeddingService`: talks to LangChain4j embedding model + pgvector store. [docs.quarkiverse](https://docs.quarkiverse.io/quarkus-langchain4j/dev/rag-pgvector-store.html)
- `RetrievalModels.kt`: `RetrievalContext`.  
- `RetrievalService`: similarity search + context assembly.

### `agent` slice

```text
src/main/kotlin/com/example/incidentanalyst/agent/
  IncidentAnalystAgent.kt
  IncidentDiagnosisService.kt
  IncidentTools.kt   (optional, for @Tool methods later)
```

- `IncidentAnalystAgent.kt`: `@RegisterAiService` interface with `@UserMessage` method for diagnosis calls. [quarkus](https://quarkus.io/blog/quarkus-meets-langchain4j/)
- `IncidentDiagnosisService.kt`: orchestrates Incident → RetrievalService → Agent → DiagnosisRepository, returning `DiagnosisResult`.  
- `IncidentTools.kt`: optional `@Tool` methods to expose selected functions to the LLM.

Tests:

```text
src/test/kotlin/com/example/incidentanalyst/agent/
  IncidentDiagnosisServiceTest.kt
```

### `aws` slice

```text
src/main/kotlin/com/example/incidentanalyst/aws/
  CloudWatchClientConfig.kt
  CloudWatchIngestionService.kt
```

- `CloudWatchClientConfig.kt`: builds AWS SDK v2 clients for CloudWatch and CloudWatch Logs (region, credentials). [docs.aws.amazon](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/setup-project-maven.html)
- `CloudWatchIngestionService.kt`: `@Scheduled` job that reads alarms/logs and persists `IncidentEntity`.

Tests:

```text
src/test/kotlin/com/example/incidentanalyst/aws/
  CloudWatchIngestionServiceTest.kt
```

### `web` / shared

```text
src/main/kotlin/com/example/incidentanalyst/web/
  ErrorHandlers.kt
  ViewModels.kt   (optional)
```

- Shared exception mappers, view models, etc.

### `config`

Optional, if you want to separate configuration beans:

```text
src/main/kotlin/com/example/incidentanalyst/config/
  LangChainConfig.kt
  DatabaseConfig.kt   (if any custom)
```

***

## Templates & resources

```text
src/main/resources/
  application.properties
  db/
    migration/
      V1__init.sql           (pgvector extension + tables)
  templates/
    layout/
      base.html
      navbar.html
    incident/
      list.html
      detail.html
    diagnosis/
      diagnosis-panel.html
    runbook/
      runbook-list.html
      runbook-edit.html
```

- Qute templates wired to `IncidentResource` / `DiagnosisResource`; include HTMX + DaisyUI via CDN in `layout/base.html`. [the-main-thread](https://www.the-main-thread.com/p/htmx-quarkus-server-rendered-ui-java)

