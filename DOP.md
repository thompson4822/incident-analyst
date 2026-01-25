## Data‑Oriented Programming Principles

Incident Analyst will emphasize **data‑oriented design** in its domain and application layers:

- Use **meaningful types** instead of primitive strings/ints for key concepts (incident IDs, severities, confidence, etc.).  
- Model state with **ADTs (sealed hierarchies)** rather than flags and nullable fields.  
- Prefer **total, explicit operations** over pervasive exceptions, using results and error types.  
- Keep entities as relatively “dumb” data, and handle behavior in services and pure functions.

These choices make the system more explicit, testable, and agent‑friendly (both for humans and LLMs).

***

## Meaningful Types and ADTs

### Domain value types

Instead of bare `String`/`Int`, define small value types:

```kotlin
@JvmInline
value class IncidentId(val value: String)

@JvmInline
value class RunbookFragmentId(val value: String)

enum class Severity {
    CRITICAL, HIGH, MEDIUM, LOW, INFO
}

enum class Confidence {
    HIGH, MEDIUM, LOW, UNKNOWN
}
```

DTOs and services use these value types; entities can store them as strings/enums but the outer API never passes raw primitives when a meaningful domain type exists.

### State as ADTs instead of flags

For incident status or lifecycle, use sealed hierarchies:

```kotlin
sealed interface IncidentStatus {
    data object Open : IncidentStatus
    data object Acknowledged : IncidentStatus
    data class Diagnosed(val diagnosisId: IncidentId) : IncidentStatus
    data object Resolved : IncidentStatus
}

sealed interface DiagnosisVerification {
    data object Unverified : DiagnosisVerification
    data object VerifiedByHuman : DiagnosisVerification
}
```

At the persistence layer you might still have columns like `status` and `verification`, but in the application layer you convert them to ADTs, which:

- Prevents invalid combinations (e.g., resolved but not diagnosed).  
- Makes branching exhaustive in `when` expressions.

***

## Entities vs DTOs (Data‑First Design)

Entities remain mutable and framework‑friendly, but DTOs and service data structures stay **immutable** and **data‑oriented**.

### Entity (Panache, mutable)

```kotlin
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

    var severity: String = "",  // mapped to Severity in DTO
    var status: String = "OPEN", // mapped to IncidentStatus

    var createdAt: Instant = Instant.now(),
    var updatedAt: Instant = Instant.now()
) : PanacheEntityBase()
```

### DTO / domain model (immutable, typed)

```kotlin
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

The mapping layer is small and mechanical, but it means:

- All “real” logic sees rich types, not loose strings.  
- Changing persistence details doesn’t leak into core behavior.

***

## Minimizing Exceptions with Result Types

Rather than throwing exceptions in normal control flow, service operations return **explicit success/error ADTs**.

### Result ADTs

```kotlin
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

### Service example

```kotlin
class IncidentDiagnosisService(
    private val incidentRepository: IncidentRepository,
    private val retrievalService: RetrievalService,
    private val llmClient: LlmClient
) {

    fun diagnose(incidentId: IncidentId): DiagnosisResult {
        val entity = incidentRepository.findByIdOrNull(incidentId.value.toLong())
            ?: return DiagnosisResult.Failure(DiagnosisError.IncidentNotFound)

        val incident = entity.toDomain() // maps to typed Incident

        val context = retrievalService.retrieveContext(incident)
            ?: return DiagnosisResult.Failure(DiagnosisError.RetrievalFailed)

        val llmOutput = llmClient.proposeDiagnosis(incident, context)
            ?: return DiagnosisResult.Failure(DiagnosisError.LlmUnavailable)

        val diagnosis = llmOutput.toDiagnosisOrNull()
            ?: return DiagnosisResult.Failure(
                DiagnosisError.LlmResponseInvalid("missing required fields")
            )

        // persist, then return
        // diagnosisRepository.persist(...)
        return DiagnosisResult.Success(diagnosis)
    }
}
```

Calling code then uses exhaustive `when`:

```kotlin
when (val result = diagnosisService.diagnose(id)) {
    is DiagnosisResult.Success -> { /* render diagnosis */ }
    is DiagnosisResult.Failure -> { /* map error to HTTP or UI message */ }
}
```

No hidden exceptions; every outcome is explicit and testable.

***

## Data‑Oriented RAG and Agent Interfaces

The RAG layer can expose **pure, data‑oriented functions**:

```kotlin
data class RetrievalContext(
    val similarIncidents: List<Incident>,
    val runbookFragments: List<RunbookFragment>
)

fun retrieveContext(incident: Incident): RetrievalContext
```

The agent interface can be similarly data‑centric:

```kotlin
data class Diagnosis(
    val id: IncidentId,
    val rootCause: String,
    val steps: List<String>,
    val confidence: Confidence,
    val verification: DiagnosisVerification
)

interface IncidentAnalystAgent {
    fun proposeDiagnosis(
        incident: Incident,
        context: RetrievalContext
    ): DiagnosisResult
}
```

This keeps the LangChain4j/Ollama internals hidden behind a pure data interface, which is both easier to mock and more robust when models change.

***

## Vertical Slices With Data‑Oriented Boundaries

Within each slice:

- **Entities**: mutable, JPA‑friendly, minimal logic.  
- **Repositories**: Panache, DB concerns only.  
- **Domain models/DTOs**: immutable, rich types, ADTs for state and errors.  
- **Services**: operate on domain models, return ADTs instead of throwing.  
- **Resources**: translate HTTP ↔ DTOs ↔ ADTs and map errors to status codes.

Example for the `incident` slice:

- `incident/IncidentEntity.kt`  
- `incident/IncidentRepository.kt`  
- `incident/IncidentModels.kt` (IncidentId, Incident, Severity, IncidentStatus)  
- `incident/IncidentService.kt` (pure-ish, uses Incident + ADTs)  
- `incident/IncidentResource.kt` (REST + HTTP concerns)  

The same pattern applies to `diagnosis`, `runbook`, and `rag`, giving you vertical slices that are internally data‑oriented and externally well‑typed.

***
