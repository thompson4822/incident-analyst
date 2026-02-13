# TICKET-006: AWS CloudWatch ingestion (COMPLETED)

## Goal
Ingest CloudWatch alarms and logs into incidents using AWS SDK v2.

## Scope
- [x] Configure CloudWatch and CloudWatch Logs clients.
- [x] Implement scheduled ingestion job to pull new/updated alarms.
- [x] Normalize AWS data into `IncidentEntity` fields.

## Acceptance criteria
- [x] Scheduled job runs without crashing when AWS is unavailable.
- [x] Ingestion persists incidents with meaningful severity and status codes.
- [x] Errors return explicit results or logs, not thrown for control flow.

## Progress
- ✅ `CloudWatchIngestionService.kt` with scheduled polling (every 60s)
- ✅ `CloudWatchAlarmClient.kt` interface for CloudWatch alarm client
- ✅ `AwsCloudWatchAlarmClient.kt` - AWS SDK implementation using `CloudWatchClient`
- ✅ `MockCloudWatchAlarmClient.kt` - Mock client for testing (active in `test` profile)
- ✅ `AlarmDto.kt` - Data transfer object for alarm data
- ✅ `AwsError.kt` - ADTs for error handling (`AwsError`, `IngestionError`, `IngestionSuccess`)
- ✅ Normalization logic: `mapAlarmToIncident()` with severity derivation
- ✅ Profile-based configuration: Dev uses LocalStack, Production uses real AWS
- ✅ 30+ unit tests + integration tests with mocked AWS client

## Implementation Details

### Files Created
- `src/main/kotlin/com/example/incidentanalyst/aws/CloudWatchIngestionService.kt`
- `src/main/kotlin/com/example/incidentanalyst/aws/CloudWatchAlarmClient.kt`
- `src/main/kotlin/com/example/incidentanalyst/aws/AwsCloudWatchAlarmClient.kt`
- `src/main/kotlin/com/example/incidentanalyst/aws/MockCloudWatchAlarmClient.kt`
- `src/main/kotlin/com/example/incidentanalyst/aws/AlarmDto.kt`
- `src/main/kotlin/com/example/incidentanalyst/aws/AwsError.kt`

### Test Files
- `src/test/kotlin/com/example/incidentanalyst/aws/CloudWatchIngestionServiceTest.kt` (30+ tests)
- `src/test/kotlin/com/example/incidentanalyst/aws/CloudWatchIngestionIntegrationTest.kt` (7 tests)
- `src/test/kotlin/com/example/incidentanalyst/aws/MockCloudWatchAlarmClientTest.kt` (4 tests)

### Key Features
- **Scheduled Job**: `@Scheduled(every = "60s")` polls CloudWatch alarms
- **Severity Mapping**: Threshold-based (CRITICAL >= 95, HIGH >= 90, MEDIUM >= 70, LOW >= 50, INFO)
- **Error Handling**: Uses Either pattern with `AwsError` and `IngestionError` ADTs
- **Configurable**: Can be disabled via `app.ingestion.cloudwatch.enabled=false`

## Dependencies
- TICKET-001, TICKET-003.
