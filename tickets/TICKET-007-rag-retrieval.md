# TICKET-007: RAG embeddings and retrieval (COMPLETED)

## Goal
Embed incident and runbook text in pgvector and retrieve similar context for diagnoses.

## Scope
- [x] Configure LangChain4j embedding model and pgvector store.
- [x] Implement `EmbeddingService` to create and persist embeddings.
- [x] Implement `RetrievalService` to return `RetrievalContext` with top-k matches.

## Acceptance criteria
- [x] Embeddings are stored and queryable from Postgres.
- [x] Retrieval returns deterministic context for seeded test data.
- [x] Service interfaces are data-oriented and testable.

## Progress
- ✅ `EmbeddingService.kt` - creates and persists embeddings for incidents, runbooks, diagnoses, resolutions
- ✅ `RetrievalService.kt` - returns `RetrievalContext` with top-k matches
- ✅ `RetrievalContext.kt` - domain model containing similar incidents and runbooks
- ✅ `RetrievalMatch.kt` - generic match type with ID, score, snippet, and sourceType
- ✅ `RetrievalConfiguration.kt` - CDI producer for `ContentRetriever` (maxResults=10, minScore=0.6)
- ✅ `RetrievalAugmentor.kt` - supplier for `DefaultRetrievalAugmentor`
- ✅ `EmbeddingStoreConfiguration.kt` - InMemoryEmbeddingStore for test profile
- ✅ `EmbeddingError.kt` and `RetrievalError.kt` - ADTs for error handling
- ✅ `SourceType.kt` enum for embedding source types
- ✅ `EmbeddingScore.kt` value class for scores
- ✅ 20 tests (unit + integration)

## Implementation Details

### Files Created
- `src/main/kotlin/com/example/incidentanalyst/rag/EmbeddingService.kt`
- `src/main/kotlin/com/example/incidentanalyst/rag/RetrievalService.kt`
- `src/main/kotlin/com/example/incidentanalyst/rag/RetrievalContext.kt`
- `src/main/kotlin/com/example/incidentanalyst/rag/RetrievalMatch.kt`
- `src/main/kotlin/com/example/incidentanalyst/rag/RetrievalConfiguration.kt`
- `src/main/kotlin/com/example/incidentanalyst/rag/RetrievalAugmentor.kt`
- `src/main/kotlin/com/example/incidentanalyst/rag/EmbeddingStoreConfiguration.kt`
- `src/main/kotlin/com/example/incidentanalyst/rag/EmbeddingError.kt`
- `src/main/kotlin/com/example/incidentanalyst/rag/RetrievalError.kt`
- `src/main/kotlin/com/example/incidentanalyst/rag/SourceType.kt`
- `src/main/kotlin/com/example/incidentanalyst/rag/EmbeddingScore.kt`

### Test Files
- `src/test/kotlin/com/example/incidentanalyst/rag/EmbeddingServiceTest.kt` (7 tests)
- `src/test/kotlin/com/example/incidentanalyst/rag/EmbeddingServiceIntegrationTest.kt` (5 tests)
- `src/test/kotlin/com/example/incidentanalyst/rag/RetrievalServiceTest.kt` (5 tests)
- `src/test/kotlin/com/example/incidentanalyst/rag/RetrievalServiceIntegrationTest.kt` (3 tests)

### Configuration
```properties
quarkus.langchain4j.ollama.embedding-model.model-id=nomic-embed-text
quarkus.langchain4j.pgvector.dimension=768
quarkus.langchain4j.pgvector.table-name=incident_knowledge
quarkus.langchain4j.pgvector.metadata.storage-mode=combined-jsonb
```

### EmbeddingService Methods
| Method | Description |
|--------|-------------|
| `embedIncident(incidentId)` | Embeds incident title + description |
| `embedRunbook(fragmentId)` | Embeds runbook fragment title + content |
| `embedVerifiedDiagnosis(diagnosisId)` | Embeds verified diagnosis with root cause and steps |
| `embedResolution(incidentId)` | Embeds resolved incident with resolution text |
| `embedBatch(incidentIds, fragmentIds)` | Batch embedding with partial failure handling |

## Dependencies
- TICKET-001, TICKET-002, TICKET-005.
