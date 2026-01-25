# AGENTS.md

This file guides agentic coding tools working in this repository.

## Repository overview
- Stack: Quarkus 3.3 + Kotlin 2.0, Panache ORM, Flyway, Postgres + pgvector, LangChain4j + Ollama, Qute + HTMX + DaisyUI.
- Architecture: vertical slices (incident, diagnosis, runbook, rag, agent, aws).
- Design: data‑oriented programming (meaningful types, ADTs, explicit results).

## Build, run, test commands
### Run (dev mode)
- `mvn quarkus:dev`
  - Uses Dev Services for Postgres.
  - Uses pgvector image: `ankane/pgvector:latest` (configured in `src/main/resources/application.properties`).

### Build
- `mvn clean package`

### Tests
- Run all tests: `mvn test`
- Run a single test class: `mvn -Dtest=IncidentServiceTest test`
- Run a single test method: `mvn -Dtest=IncidentServiceTest#testName test`

### Lint/format
- No dedicated lint or formatter configured in this repo.
- Keep formatting consistent with existing Kotlin files.

## Configuration notes
- Dev Services should handle Postgres automatically. Avoid hard‑coding JDBC URLs in dev.
- LangChain4j pgvector dimension must match embedding model:
  - Current config: `nomic-embed-text` → 768 dims (`quarkus.langchain4j.pgvector.dimension=768`).
- Dev profile cleans Flyway at start: `%dev.quarkus.flyway.clean-at-start=true`.

## Code style guidelines
### Kotlin style
- Use Kotlin data classes for domain models and DTOs.
- Entities are `open class` for Hibernate proxying.
- Prefer `val` for domain models; `var` only in entities.
- Keep files small and single‑purpose per slice.

### Imports
- Use explicit imports; avoid wildcards.
- Order: Kotlin/Java stdlib → third‑party → project imports.

### Naming conventions
- Packages: `com.example.incidentanalyst.<slice>`.
- Types: `IncidentEntity`, `IncidentRepository`, `IncidentService`, `IncidentResource`, `IncidentDto`.
- DTOs end with `RequestDto`/`ResponseDto` when needed.
- IDs are value classes (e.g., `IncidentId`, `DiagnosisId`).

### Domain modeling (DOP)
- Use meaningful types instead of primitives for IDs and enums.
- Model state with sealed interfaces (ADTs) instead of flags.
- Convert between persistence and domain via small mapping functions.

Example (value types + ADT):
```kotlin
@JvmInline
value class IncidentId(val value: Long)

sealed interface IncidentStatus {
    data object Open : IncidentStatus
    data object Acknowledged : IncidentStatus
    data class Diagnosed(val diagnosisId: Long) : IncidentStatus
    data object Resolved : IncidentStatus
}
```

### Error handling
- Avoid exceptions for expected control flow.
- Prefer result ADTs (e.g., `DiagnosisResult` with `DiagnosisError`).
- Resources map errors to HTTP status codes explicitly.

Example (explicit results):
```kotlin
sealed interface DiagnosisResult {
    data class Success(val diagnosis: Diagnosis) : DiagnosisResult
    data class Failure(val error: DiagnosisError) : DiagnosisResult
}
```

### Persistence conventions
- Entities map to snake_case columns and tables.
- Use `@Column(name = "...")` for snake_case fields.
- Use `@GeneratedValue(strategy = GenerationType.IDENTITY)` for IDs.
- Keep entities mutable and minimal (no heavy logic).

Example (entity pattern):
```kotlin
@Entity
@Table(name = "incidents")
open class IncidentEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    open var id: Long? = null,
    open var source: String = "",
    @Column(name = "created_at")
    open var createdAt: Instant = Instant.now()
) : PanacheEntityBase
```

### Service design
- Services operate on domain models, not entities.
- Pure functions for parsing and rendering where possible.
- Keep external integrations behind service boundaries (AWS, LLM, embeddings).

Example (service pattern):
```kotlin
@ApplicationScoped
class IncidentService(private val incidentRepository: IncidentRepository) {
    fun listRecent(limit: Int = 50): List<Incident> =
        incidentRepository.findRecent(limit).map { it.toDomain() }
}
```

### HTTP resources
- Use JAX‑RS annotations (`@Path`, `@GET`, `@POST`).
- Return DTOs, not entities.
- Keep resources thin; call services for logic.

Example (resource pattern):
```kotlin
@Path("/incidents")
class IncidentResource(private val incidentService: IncidentService) {
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    fun list(): List<IncidentResponseDto> =
        incidentService.listRecent().map { it.toResponseDto() }
}
```

### Mapping functions
- Prefer local `toDomain()` / `toEntity()` helpers in slice files.
- Keep mapping mechanical and side‑effect free.

Example:
```kotlin
fun DiagnosisEntity.toDomain(): Diagnosis = Diagnosis(
    id = DiagnosisId(requireNotNull(id)),
    incidentId = IncidentId(requireNotNull(incident?.id)),
    rootCause = suggestedRootCause,
    steps = remediationSteps.split("\n").filter { it.isNotBlank() },
    confidence = Confidence.valueOf(confidence),
    verification = if (verification == "VERIFIED")
        DiagnosisVerification.VerifiedByHuman else DiagnosisVerification.Unverified,
    createdAt = createdAt
)
```

### RAG and agent integration
- Embedding and retrieval are separate services (`EmbeddingService`, `RetrievalService`).
- Agent interface uses `@RegisterAiService` and `@UserMessage` with named variables (`@V`).
- Parse LLM JSON into typed domain models, return explicit errors on invalid responses.

Example (AI service signature):
```kotlin
@RegisterAiService
interface IncidentAnalystAgent {
    @UserMessage("""... {incident} ... {context} ...""")
    fun proposeDiagnosis(@V("incident") incident: String, @V("context") context: String): String
}
```

### Tests
- Test classes follow the slice naming (e.g., `IncidentServiceTest`).
- Prefer focused unit tests for mappings and ADTs.
- Use Quarkus tests for repository/service integration with Flyway.
- Separate integration tests into `*IntegrationTest` classes.
  - Unit tests should use `@InjectMock` and Mockito.
  - Integration tests should avoid `@InjectMock` and use real Panache repositories/DB.
- Kotlin + Mockito matcher guidance:
  - Avoid `any()` on non‑nullable parameters unless using Mockito‑Kotlin correctly.
  - Prefer `Mockito.any(Foo::class.java)` or stub with a concrete object.

Example (single‑test run):
```bash
mvn -Dtest=IncidentServiceTest#mapsEntityToDomain test
```

Example (single‑test run with spaces):
```bash
mvn -Dtest="CloudWatchIngestionServiceTest#ingestAlarms returns PersistenceError when incidentService create throws" test
```

Note: error logs in tests are expected when exercising failure‑path cases; treat them as failures only if tests fail.

## Files and locations
- Kotlin main: `src/main/kotlin/com/example/incidentanalyst/...`
- Resources: `src/main/resources/` (Flyway migrations, Qute templates)
- Tests: `src/test/kotlin/com/example/incidentanalyst/...`

## Cursor/Copilot rules
- No `.cursor/rules`, `.cursorrules`, or `.github/copilot-instructions.md` found.

## Practical tips for agents
- Keep schema and entity mappings in sync (`V1__init.sql` ↔ entity annotations).
- Use Dev Services for Postgres during dev; avoid local DB overrides unless required.
- If Flyway checksum changes, clean the dev schema or bump migration version.
