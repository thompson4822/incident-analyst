# TICKET-006 Phased Implementation Plan

## Context
TICKET-006: AWS CloudWatch Ingestion needs to integrate AWS SDK v2, query CloudWatch alarms, map to incidents, and implement proper error handling. Previous coding agent encountered encoding and type compatibility issues.

## Recommendation: Phased Approach

Instead of implementing everything at once (which caused encoding/type errors), let's tackle this ticket in phases:

---

## Phase 1: Foundation (Simple Start)

**Goal:** Establish data-oriented programming foundation with proper types

### Tasks:
1. ✅ Create `AwsError.kt` - ADT for AWS errors (ServiceUnavailable, Throttled, Unauthorized, NetworkError, Unknown)
2. ✅ Create `CloudWatchIngestionResult.kt` - Result ADT (Success(count), Failure(AwsError))
3. ✅ Create `AlarmQueryResult.kt` - Result ADT (Success(List<AlarmDto>), Failure(AwsError))
4. ✅ Create `AlarmDto.kt` - Data class for alarm fields
5. ✅ Update `CloudWatchIngestionService.kt` - Use dummy data with proper result types (no real AWS calls yet)
6. ✅ Write simple unit tests for the new types

**Acceptance Criteria for Phase 1:**
- Result ADTs are properly structured
- Service returns explicit results, not exceptions
- Code compiles without errors
- Tests pass

**Estimated Complexity:** 2/10

---

## Phase 2: AWS Integration

**Goal:** Implement actual AWS CloudWatch client wrapper

### Tasks:
1. Create `CloudWatchAlarmClient.kt` - Interface with `listAlarmsInAlarmState(): AlarmQueryResult`
2. Create `AwsCloudWatchAlarmClient.kt` - Implementation wrapping AWS SDK v2 CloudWatchClient
3. Update `CloudWatchClientConfig.kt` - Add @Produces annotation for proper CDI injection
4. Implement AWS API calls:
   - Use `DescribeAlarmsRequest.builder().stateValue(StateValue.ALARM).build()`
   - Implement pagination with `nextToken` loop
   - Extract fields: alarmName, alarmDescription, stateValue, stateReason, stateUpdatedTimestamp
   - Use AWS SDK default credential provider chain
5. Handle SDK exceptions and map to AwsError types
6. Return explicit results, never throw exceptions
7. Add proper logging for errors
8. Write unit tests for AWS client (mocked)

**Acceptance Criteria for Phase 2:**
- CloudWatch alarms are queried successfully
- Pagination works correctly
- AWS errors are caught and mapped to AwsError
- No exceptions thrown from client wrapper
- Tests verify error paths

**Estimated Complexity:** 5/10

---

## Phase 3: Alarm-to-Incident Mapping

**Goal:** Transform AWS alarm data into domain incidents

### Tasks:
1. Add `mapAlarmToIncident()` function to `CloudWatchIngestionService`:
   ```kotlin
   fun mapAlarmToIncident(alarm: AlarmDto): Incident? {
       val timestamp = alarm.stateUpdatedTimestamp ?: Instant.now()

       // Only create incidents for ALARM state
       if (alarm.stateValue != "ALARM") {
           return null
       }

       // Derive severity from threshold
       val severity = when {
           alarm.comparisonOperator == "GreaterThanThreshold" && isHighThreshold(alarm) -> Severity.HIGH
           alarm.comparisonOperator == "GreaterThanThreshold" && isMediumThreshold(alarm) -> Severity.MEDIUM
           alarm.comparisonOperator == "GreaterThanThreshold" && isLowThreshold(alarm) -> Severity.LOW
           else -> Severity.INFO
       }

       return Incident(
           id = IncidentId(0), // Will be assigned by repository
           source = "AWS:CloudWatch",
           title = alarm.alarmName ?: "Unknown Alarm",
           description = buildAlarmDescription(alarm),
           severity = severity,
           status = IncidentStatus.Open,
           createdAt = timestamp,
           updatedAt = timestamp
       )
   }
   ```
2. Add helper functions for severity derivation:
   ```kotlin
   private fun isHighThreshold(alarm: AlarmDto): Boolean {
       val threshold = alarm.threshold?.toDoubleOrNull() ?: 0.0
       return threshold >= 90.0
   }

   private fun isMediumThreshold(alarm: AlarmDto): Boolean {
       val threshold = alarm.threshold?.toDoubleOrNull() ?: 0.0
       return threshold >= 70.0
   }

   private fun isLowThreshold(alarm: AlarmDto): Boolean {
       val threshold = alarm.threshold?.toDoubleOrNull() ?: 0.0
       return threshold >= 50.0
   }
   ```
3. Add `buildAlarmDescription()` function for structured alarm details
4. Update `pollAlarms()` to call mapping and persistence
5. Handle OK/INSUFFICIENT_DATA alarm states (ignore or handle appropriately)

**Acceptance Criteria for Phase 3:**
- Alarm fields map correctly to Incident fields
- Severity is derived from thresholds
- Non-ALARM states are handled
- Unit tests verify mapping logic

**Estimated Complexity:** 3/10

---

## Phase 4: Error Handling and Resilience

**Goal:** Make ingestion robust against AWS failures

### Tasks:
1. Add explicit error logging for all AWS failures
2. Ensure @Scheduled method never crashes (always returns result)
3. Add retry logic (or use AWS SDK defaults)
4. Add circuit breaker pattern (optional, can defer)
5. Log errors with proper context
6. Test error scenarios (service unavailable, throttling, auth)

**Acceptance Criteria for Phase 4:**
- All AWS error types are covered
- Scheduled job never throws exceptions
- Errors are logged appropriately
- Error handling is tested

**Estimated Complexity:** 4/10

---

## Phase 5: Full Integration and Testing

**Goal:** Complete end-to-end flow with comprehensive tests

### Tasks:
1. Write integration tests (may mock AWS, use test framework)
2. Test alarm-to-incident mapping with real alarm data
3. Test error scenarios
4. Test scheduled job reliability
5. Test duplicate handling (if needed)
6. Verify database persistence
7. Update TICKET-006 documentation

**Acceptance Criteria for Phase 5:**
- ✅ All tests pass (302 total tests)
- ✅ End-to-end flow works
- ✅ Documentation updated
- ✅ Ready for commit

**Estimated Complexity:** 6/10

**Status:** ✅ **COMPLETE**
- Created CloudWatchIngestionIntegrationTest.kt with 21 comprehensive tests
- All 302 tests passing (281 unit + 21 integration)
- End-to-end flow verified: alarm polling → mapping → persistence → database
- Database persistence verified with real IncidentService/IncidentRepository
- All error scenarios tested (AWS errors, persistence failures)
- Scheduled job reliability confirmed

---

## File Creation Plan

### New Files to Create:
```
src/main/kotlin/com/example/incidentanalyst/aws/
├── AwsError.kt
├── AlarmDto.kt
├── AlarmQueryResult.kt
├── CloudWatchIngestionResult.kt
├── CloudWatchAlarmClient.kt
└── AwsCloudWatchAlarmClient.kt

src/test/kotlin/com/example/incidentanalyst/aws/
├── CloudWatchAlarmClientTest.kt
├── AlarmMappingTest.kt
├── CloudWatchIngestionServiceTest.kt
└── AlarmQueryResultTest.kt
```

### Files to Modify:
```
src/main/kotlin/com/example/incidentanalyst/aws/
├── CloudWatchClientConfig.kt (add @Produces)
└── CloudWatchIngestionService.kt (implement real mapping)
```

---

## Total Work Estimation

- **Phase 1:** Foundation - ~2-3 hours
- **Phase 2:** AWS Integration - ~4-6 hours
- **Phase 3:** Mapping Logic - ~2-3 hours
- **Phase 4:** Error Handling - ~2-4 hours
- **Phase 5:** Testing - ~3-5 hours

**Total:** ~13-21 hours across all phases

---

## Benefits of Phased Approach

1. **Smaller commits**: Each phase is reviewable and testable
2. **Easier debugging**: Fewer changes per commit
3. **Continuous validation**: Tests pass at each phase
4. **Flexibility**: Can adjust approach based on learning
5. **Risk mitigation**: If a phase has issues, it's isolated

---

## Next Steps

1. Review this plan
2. Approve or modify phases
3. Start with Phase 1: Foundation
4. Complete phases sequentially
5. Commit each phase separately for easier review
