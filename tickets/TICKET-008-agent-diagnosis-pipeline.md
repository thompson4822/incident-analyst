# TICKET-008: Agent diagnosis pipeline (COMPLETED)

## Goal
Orchestrate retrieval and AI diagnosis with explicit results and persistence.

## Scope
- [x] Wire `IncidentAnalystAgent` with LangChain4j/Ollama.
- [x] Implement `IncidentDiagnosisService` to assemble context, call agent, parse JSON, and persist.
- [x] Add pure functions to render prompts and parse JSON into domain types.

## Acceptance criteria
- [x] Diagnosis flow returns `DiagnosisResult.Success` or `DiagnosisResult.Failure` for all outcomes.
- [x] Invalid AI responses are mapped to `DiagnosisError.LlmResponseInvalid`.
- [x] Successful diagnoses are persisted and linked to incidents.

## Progress
- ✅ `IncidentAnalystAgent.kt` with `@RegisterAiService` annotation
- ✅ `IncidentDiagnosisService.kt` - core orchestration service
- ✅ `IncidentTools.kt` - LangChain4j `@Tool` for fetching incidents
- ✅ RAG integration via `IncidentRetrievalAugmentorSupplier`
- ✅ `ProfileService` integration for application context in prompts
- ✅ JSON parsing with markdown code block handling
- ✅ Error handling ADTs: `DiagnosisError`, `DiagnosisSuccess`
- ✅ Unit tests with mocked AI service

## Implementation Details

### Files Created
- `src/main/kotlin/com/example/incidentanalyst/agent/IncidentAnalystAgent.kt`
- `src/main/kotlin/com/example/incidentanalyst/agent/IncidentDiagnosisService.kt`
- `src/main/kotlin/com/example/incidentanalyst/agent/IncidentTools.kt`

### Test Files
- `src/test/kotlin/com/example/incidentanalyst/agent/IncidentDiagnosisServiceTest.kt` (3 tests)

### Agent Configuration
```properties
quarkus.langchain4j.ollama.base-url=http://localhost:11434
quarkus.langchain4j.ollama.chat-model.model-id=qwen2.5:7b-instruct
```

### IncidentDiagnosisService Flow
1. Retrieves incident by ID from repository
2. Checks for existing diagnosis (avoids redundant AI calls)
3. Assembles context from `ProfileService` (appName, appStack, appComponents)
4. Calls AI service with structured prompt
5. Parses LLM JSON response into `LlmDiagnosisResponse` DTO
6. Handles markdown code blocks in LLM responses
7. Persists diagnosis via `DiagnosisRepository`
8. Updates incident status to `DIAGNOSED:<id>`

### Error Handling (ADTs)
```kotlin
sealed interface DiagnosisError {
    data object IncidentNotFound : DiagnosisError
    data object RetrievalFailed : DiagnosisError
    data object LlmUnavailable : DiagnosisError
    data class LlmResponseInvalid(val reason: String) : DiagnosisError
    data object NotFound : DiagnosisError
    data object UpdateFailed : DiagnosisError
}

sealed interface DiagnosisSuccess {
    data class NewDiagnosisGenerated(val diagnosis: Diagnosis) : DiagnosisSuccess
    data class ExistingDiagnosisFound(val diagnosis: Diagnosis) : DiagnosisSuccess
}
```

### RAG Integration
- Agent uses `@RegisterAiService(retrievalAugmentor = IncidentRetrievalAugmentorSupplier::class)`
- Content retriever configured with: `maxResults=10`, `minScore=0.6`

## Dependencies
- TICKET-003, TICKET-004, TICKET-007.
