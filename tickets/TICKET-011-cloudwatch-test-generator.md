# TICKET-011: CloudWatch Test Data Generator

## Goal
Create a REST endpoint and generation logic to automatically generate test CloudWatch alarms with random variations for development, load testing, and AI training data.

## Scope
- REST endpoint for generating test CloudWatch alarms
- Random generation of namespaces, metrics, thresholds, and severities
- Support for batch generation (high-volume testing)
- Realistic alarm scenarios with varied timestamps
- Integration with existing training data ingestion pipeline

## Out of Scope
- Full CloudWatch simulation (use existing LocalStack methods)
- Alarm state transitions (OK → ALARM → OK)
- Custom AWS account management
- UI for generator (pure REST API only)

## Acceptance Criteria

### Functional Requirements
- [x] REST endpoint `POST /test/cloudwatch/generate-alarms` accepts generation parameters
- [x] Generates specified number of alarms (default 10)
- [x] Randomly selects from provided namespaces and severities
- [x] Generates realistic alarm names and descriptions
- [x] Supports custom min/max severity thresholds
- [x] Generates timestamps within optional time range
- [x] Creates incidents via existing `IncidentService`
- [x] Returns summary of generated incidents (count, severity breakdown)

### Data Generation Requirements
- [x] Alarms have unique names (e.g., `HighCPUAlarm-001`, `MediumDiskAlarm-042`)
- [x] Thresholds vary realistically (50, 60, 70, 80, 90, 95)
- [x] Metrics map to namespaces (e.g., `CPUUtilization` → `AWS/EC2`, `RequestCount` → `AWS/ELB`)
- [x] Timestamps distributed randomly across specified time window
- [x] State reasons use realistic CloudWatch messages
- [x] Descriptions include metric-specific details

### Quality Requirements
- [x] Deterministic generation (same seed produces same output)
- [x] Optional seed parameter for reproducible test data
- [x] Validation of input parameters (count must be 1-1000, time range valid)
- [x] Error handling for invalid parameters
- [x] No duplicate alarm names within same batch

### Integration Requirements
- [x] Uses existing `IncidentService.create()` for persistence
- [ ] Alarms flow through existing `CloudWatchIngestionService` (optional)
- [x] Compatible with existing training data pipeline
- [x] Works with mock CloudWatch client for isolated testing

### Testing Requirements
- [x] Unit tests for generation logic
- [x] Unit tests for parameter validation
- [x] Integration test for REST endpoint
- [x] Verify incidents persisted correctly
- [ ] Test with existing training pipeline
- [ ] Verify high-volume generation (100+ alarms)

### Documentation Requirements
- [ ] API documentation with examples
- [ ] Usage guide for different generation scenarios
- [ ] Examples of generated alarm data
- [ ] Integration with existing CloudWatch simulation guide

## Dependencies
- TICKET-001 (dependencies and config)
- TICKET-003 (incident slice)
- TICKET-006 (CloudWatch ingestion - for integration option)

## Implementation Status

### Completed Components
- **DTOs**: `CloudWatchTestDataRequestDto`, `CloudWatchTestDataResponseDto`, `CloudWatchTestDataErrorResponseDto`
- **Service**: `CloudWatchTestDataGeneratorService` with generation logic and mapping functions
- **Resource**: `CloudWatchTestDataResource` REST endpoint
- **Tests**: `CloudWatchTestDataGeneratorServiceTest` (14 tests), `CloudWatchTestDataResourceTest` (2 tests)
- **Result ADT**: `CloudWatchTestDataGenerationResult` (Success/ValidationError)

### Code Review Fixes Applied
After initial implementation, the following issues were identified and fixed:

#### Critical Fixes
- **Severity Mapping Bug**: Fixed `deriveSeverity()` to properly map thresholds >= 95.0 to CRITICAL severity
  - Previous: Thresholds 95-99 incorrectly mapped to HIGH
  - Now: Thresholds >= 95.0 correctly map to CRITICAL

#### Quality Improvements
- **Type Safety**: Replaced `mapOf("error" to message)` with typed `CloudWatchTestDataErrorResponseDto`
- **Code Cleanliness**: Added proper `import Severity` and removed fully qualified references
- **Unused Code**: Removed unused `index` parameter from `generateUniqueAlarmName()`
- **Test Quality**: Improved Mockito matcher usage in tests

### Test Results
- **CloudWatchTestDataGeneratorServiceTest**: 14/14 tests passing ✅
- **CloudWatchTestDataResourceTest**: 2/2 tests passing ✅
- **Full Test Suite**: 372/372 tests passing ✅
- **No Regressions**: All existing tests continue to pass

### Files Modified
- `src/main/kotlin/com/example/incidentanalyst/aws/CloudWatchTestDataDto.kt` (new)
- `src/main/kotlin/com/example/incidentanalyst/aws/CloudWatchTestDataGeneratorService.kt` (new)
- `src/main/kotlin/com/example/incidentanalyst/aws/CloudWatchTestDataResource.kt` (new)
- `src/test/kotlin/com/example/incidentanalyst/aws/CloudWatchTestDataGeneratorServiceTest.kt` (new)
- `src/test/kotlin/com/example/incidentanalyst/aws/CloudWatchTestDataResourceTest.kt` (new)
- `src/main/resources/db/migration-h2/V1__init.sql` (new, for H2 test schema)
- `src/test/kotlin/com/example/incidentanalyst/agent/IncidentDiagnosisServiceTest.kt` (deleted, was empty)

## Implementation Notes

### Design Considerations
- Generator should be data-oriented with clear separation of concerns
- Use existing domain models (`Incident`, `Severity`, `IncidentStatus`)
- Support for both single-incident and batch-incident generation
- Consider future extension: predefined alarm scenarios for specific system patterns

### Future Enhancements (Out of Scope for This Ticket)
- Export generated data to JSON for external tooling
- Import/export of test datasets
- Cron job for periodic test data generation
- Configuration profiles for dev/test/prod generation strategies
