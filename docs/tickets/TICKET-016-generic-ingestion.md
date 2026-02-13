# TICKET-016: Generic Webhook Ingestion (COMPLETED)

## Overview
Implement a generic webhook ingestion endpoint that allows external systems (Sentry, GitHub, custom scripts) to push incidents into the platform. This proves the domain-agnostic nature of the "Generic Operational Brain."

## Tasks
- [x] Create a `WebhookIngestionResource` with a `POST /ingest/webhook` endpoint.
- [x] Define a `GenericIncidentRequestDto` that maps common incident fields (title, description, severity, source).
- [x] Implement a mapping layer to convert the generic JSON payload into the core `Incident` domain model.
- [x] Add security (e.g., a simple API key check via header) to the webhook endpoint.
- [x] Update the UI to show the source of these incidents correctly.

## Acceptance Criteria
- [x] External systems can create incidents via a simple `curl` command.
- [x] The system correctly identifies the source as provided in the webhook payload.
- [x] Ingested incidents are immediately visible on the dashboard and searchable.
- [x] AI diagnosis works for these incidents using the same RAG pipeline.

## Implementation Details

### Architecture Pattern
Uses the **Either pattern** for explicit error handling, consistent with the project's data-oriented programming style:
- `WebhookIngestionService` returns `Either<WebhookIngestionError, WebhookIngestionSuccess>`
- `WebhookIngestionResource` uses `.fold()` to map results to HTTP responses

### Files Created
- `src/main/kotlin/com/example/incidentanalyst/ingestion/WebhookDto.kt` - DTOs with validation
- `src/main/kotlin/com/example/incidentanalyst/ingestion/WebhookIngestionModels.kt` - Sealed interfaces for Either types
- `src/main/kotlin/com/example/incidentanalyst/ingestion/WebhookIngestionService.kt` - Business logic returning Either
- `src/main/kotlin/com/example/incidentanalyst/ingestion/WebhookIngestionResource.kt` - REST endpoint
- `src/test/kotlin/com/example/incidentanalyst/ingestion/WebhookIngestionServiceTest.kt` - 12 service tests
- `src/test/kotlin/com/example/incidentanalyst/ingestion/WebhookIngestionResourceTest.kt` - 25 resource tests

### Files Modified
- `src/main/resources/templates/incident/list.html` - UI source filter updated

### Either Types

```kotlin
sealed interface WebhookIngestionError {
    data object Unauthorized : WebhookIngestionError
    data class ValidationError(val errors: List<String>) : WebhookIngestionError
    data class PersistenceError(val message: String) : WebhookIngestionError
}

sealed interface WebhookIngestionSuccess {
    data class IncidentCreated(val id: Long, val source: String, val title: String) : WebhookIngestionSuccess
}
```

### API Endpoint
```
POST /ingest/webhook
Headers: X-API-Key: <your-api-key>
Content-Type: application/json

{
  "title": "Incident title",
  "description": "Detailed description",
  "severity": "HIGH",
  "source": "sentry"
}
```

### Response Codes
| Status | Condition |
|--------|-----------|
| 201 Created | Incident successfully created |
| 400 Bad Request | Validation failed (with error details) |
| 401 Unauthorized | Missing or invalid API key |
| 500 Internal Server Error | Persistence error |

### Validation Rules
- `title`: required, not blank, max 500 characters
- `description`: required, not blank
- `source`: required, not blank, max 100 characters
- `severity`: optional, defaults to MEDIUM if invalid (case-insensitive)

### Example curl Command
```bash
curl -X POST http://localhost:8080/ingest/webhook \
  -H "Content-Type: application/json" \
  -H "X-API-Key: dev-secret-key" \
  -d '{
    "title": "Sentry Error: NullPointerException",
    "description": "NPE in UserService.kt:42",
    "severity": "CRITICAL",
    "source": "sentry"
  }'
```

### Configuration
API key configured in `application.properties`:
```properties
app.ingestion.webhook.api-key=dev-secret-key
```

### Test Coverage
- **37 total tests** (25 resource + 12 service):
  - All severity levels (CRITICAL, HIGH, MEDIUM, LOW, INFO)
  - Case-insensitive severity handling
  - Validation errors (missing/blank/oversized fields)
  - API key authentication (missing/invalid)
  - Either pattern: Left/Right assertions
  - Unicode support
  - Long descriptions
  - Persistence error handling
