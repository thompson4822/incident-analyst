# TICKET-010: Testing strategy (COMPLETED)

## Goal
Create reliable tests for core slices and integrations using data-oriented results.

## Scope
- [x] Unit tests for domain mapping and result ADTs.
- [x] Quarkus tests for repositories and services with H2 + Flyway.
- [x] Mock AWS SDK and AI services for deterministic tests.

## Acceptance criteria
- [x] Tests cover incident, diagnosis, retrieval, and diagnosis pipeline flows.
- [x] Failure modes are asserted via explicit result ADTs.
- [x] Test data setup mirrors production schema.

## Progress
- ✅ ~459 tests across 35 test files
- ✅ All slices covered: incident, diagnosis, runbook, rag, agent, aws, remediation, ingestion
- ✅ Unit tests with `@InjectMock` for isolated testing
- ✅ Integration tests with `@QuarkusTest` and `@TestTransaction`
- ✅ Mock AWS SDK via `MockCloudWatchAlarmClient`
- ✅ Mock AI services via `@InjectMock IncidentAnalystAgent`
- ✅ Mock embedding model and store for deterministic RAG tests
- ✅ Either pattern tested for all failure modes

## Implementation Details

### Test Files by Slice

#### Incident Slice (6 files)
| File | Tests |
|------|-------|
| `IncidentServiceTest.kt` | 8 |
| `IncidentServiceIntegrationTest.kt` | 2 |
| `IncidentRepositoryTest.kt` | 14 |
| `IncidentModelsTest.kt` | 30+ |
| `IncidentResourceTest.kt` | ~10 |
| `IncidentDtoTest.kt` | ~5 |

#### Diagnosis Slice (6 files)
| File | Tests |
|------|-------|
| `DiagnosisServiceTest.kt` | 18 |
| `DiagnosisRepositoryTest.kt` | 17 |
| `DiagnosisModelsTest.kt` | 27 |
| `DiagnosisResourceTest.kt` | ~5 |
| `DiagnosisDtoTest.kt` | ~5 |

#### RAG/Embeddings Slice (5 files)
| File | Tests |
|------|-------|
| `EmbeddingServiceTest.kt` | 9 |
| `EmbeddingServiceIntegrationTest.kt` | 5 |
| `RetrievalServiceTest.kt` | 5 |
| `RetrievalServiceIntegrationTest.kt` | 3 |

#### Remediation Slice (4 files)
| File | Tests |
|------|-------|
| `RemediationServiceTest.kt` | 10 |
| `RemediationModelsTest.kt` | 12 |
| `RemediationResourceTest.kt` | 7 |
| `SimulatedActionExecutorTest.kt` | 9 |

#### AWS/CloudWatch Slice (5 files)
| File | Tests |
|------|-------|
| `CloudWatchIngestionServiceTest.kt` | 32 |
| `CloudWatchIngestionIntegrationTest.kt` | ~5 |
| `MockCloudWatchAlarmClientTest.kt` | 4 |
| `CloudWatchTestDataGeneratorServiceTest.kt` | ~5 |
| `CloudWatchTestDataResourceTest.kt` | ~3 |

#### Runbook Slice (5 files)
| File | Tests |
|------|-------|
| `RunbookServiceTest.kt` | ~8 |
| `RunbookRepositoryTest.kt` | ~5 |
| `RunbookModelsTest.kt` | ~10 |
| `RunbookResourceTest.kt` | ~12 |
| `RunbookDtoTest.kt` | ~5 |

#### Other (4 files)
| File | Tests |
|------|-------|
| `IncidentDiagnosisServiceTest.kt` | 3 |
| `WebhookIngestionServiceTest.kt` | 12 |
| `WebhookIngestionResourceTest.kt` | 2 |
| `EitherTest.kt` | 6 |

### Test Patterns Used
1. **Arrange-Act-Assert** - Consistent pattern across all tests
2. **@InjectMock + Mockito** - For unit tests with mocked dependencies
3. **@QuarkusTest + @TestTransaction** - For integration tests with real database
4. **Either pattern matching** - Explicit testing of both Left (error) and Right (success) paths
5. **Edge case testing** - Invalid formats, null values, empty strings, boundary conditions

### Failure Modes Tested via ADTs
- `IncidentError.NotFound`
- `DiagnosisError.NotFound`, `DiagnosisError.UpdateFailed`, `DiagnosisError.LlmResponseInvalid`
- `EmbeddingError.EmbeddingFailed`, `EmbeddingError.InvalidText`
- `RetrievalError.SearchFailed`, `RetrievalError.InvalidQuery`
- `IngestionError.AwsError` (Throttled, Unauthorized, NetworkError, ServiceUnavailable)
- `IngestionError.PersistenceError`

### Mock Configurations
- **AWS**: `MockCloudWatchAlarmClient` with `@IfBuildProfile("test")`
- **AI**: `@InjectMock IncidentAnalystAgent` in test classes
- **Embedding**: `@InjectMock EmbeddingModel`, `@InjectMock EmbeddingStore`
- **Retrieval**: `@InjectMock ContentRetriever`

## Dependencies
- TICKET-002 through TICKET-009.
