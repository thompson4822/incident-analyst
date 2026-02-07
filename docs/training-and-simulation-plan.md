# Implementation Plan: Training Data + CloudWatch Simulation

## Overview
- **Two independent features** (no coupling)
- **Three simulation methods** (all documented and usable)
- **Clean dev-to-production switch**
- **Uses existing domain models** (minimal new code)

---

## Feature 1: Training Data Ingestion Endpoint

### Goal
REST endpoint to accept arbitrary JSON for incident creation (LLM training data)

### Data Model
```kotlin
TrainingIncidentRequest(
    title: String,
    description: String,
    severity: Severity,  // HIGH, MEDIUM, LOW, INFO
    timestamp: Instant?,
    stackTrace: String?   // Optional abbreviated stack trace
    source: String = "training"  // Default source
)
```

### Implementation
```kotlin
@Path("/training")
class TrainingIncidentResource(
    private val incidentService: IncidentService
) {
    @POST
    @Path("/incidents")
    @Consumes(MediaType.APPLICATION_JSON)
    fun createTrainingIncident(request: TrainingIncidentRequest): Response<TrainingIncidentResponse> {
        // Map request to Incident domain model
        // Call incidentService.create()
        // Log JSON to file for audit (logs/training-incidents-{date}.jsonl)
        // Return created incident
    }
}
```

### JSON File Format (JSON Lines)
```jsonl
{"title":"High CPU Usage","description":"CPU threshold exceeded 95%","severity":"HIGH","timestamp":"2026-02-07T12:00:00Z","stackTrace":"at com.example.Service.process(Service.kt:45)"}
{"title":"Memory Warning","description":"Memory usage above threshold","severity":"MEDIUM","timestamp":"2026-02-07T13:00:00Z"}
```

### Benefits
- ✅ Uses existing `Incident` domain model
- ✅ Reuses `incidentService.create()` logic
- ✅ Logs raw JSON for LLM training
- ✅ Independent from CloudWatch (no conflicts)

---

## Feature 2: Mock CloudWatch Alarm Client (Option A)

### Goal
Inject test data during dev mode without real AWS

### Implementation
```kotlin
@Alternative
@Priority(1)
class MockCloudWatchAlarmClient(
    private val testAlarmConfig: TestAlarmConfig  // Loads from config
) : CloudWatchAlarmClient {

    override fun listAlarmsInAlarmState(): AlarmQueryResult {
        when (testAlarmConfig.mode) {
            "no-alarms" -> AlarmQueryResult.Success(emptyList())
            "single-high-severity" -> AlarmQueryResult.Success(listOf(
                AlarmDto(alarmName="HighCPUAlarm", stateValue="ALARM",
                       metricName="CPUUtilization", threshold="90",
                       stateUpdatedTimestamp=Instant.now())
            ))
            "multiple-severities" -> AlarmQueryResult.Success(listOf(
                AlarmDto(alarmName="HighCPUAlarm", stateValue="ALARM",
                       metricName="CPUUtilization", threshold="90"),
                AlarmDto(alarmName="MediumDiskAlarm", stateValue="ALARM",
                       metricName="DiskUsage", threshold="70"),
                AlarmDto(alarmName="LowMemoryAlarm", stateValue="ALARM",
                       metricName="MemoryUsage", threshold="50")
            ))
            else -> AlarmQueryResult.Failure(AwsError.NetworkError("Unknown mode"))
        }
    }
}
```

### Configuration
```properties
%dev.quarkus.devservices.cloudwatch.mock.enabled=true
%dev.quarkus.devservices.cloudwatch.mock.mode=multiple-severities
# Options: no-alarms, single-high-severity, multiple-severities
```

### Benefits
- ✅ Zero code changes to `CloudWatchIngestionService`
- ✅ Easy to add new scenarios
- ✅ Supports severity testing (your main requirement)
- ✅ Toggled via configuration (dev only)

---

## Feature 3: LocalStack Simulation Guides

### Goal
Document all 3 methods for manual testing

### Method 1: LocalStack CLI (`awslocal`)
```bash
# Install LocalStack CLI
pip install localstack

# Start LocalStack
localstack start

# Create HIGH severity alarm
awslocal cloudwatch put-metric-alarm \
  --alarm-name HighCPUAlarm \
  --metric-name CPUUtilization \
  --namespace AWS/EC2 \
  --threshold 90 \
  --comparison-operator GreaterThanThreshold \
  --evaluation-periods 1

# Create MEDIUM severity alarm
awslocal cloudwatch put-metric-alarm \
  --alarm-name MediumDiskAlarm \
  --metric-name DiskUsage \
  --namespace AWS/EC2 \
  --threshold 70 \
  --comparison-operator GreaterThanThreshold

# Create LOW severity alarm
awslocal cloudwatch put-metric-alarm \
  --alarm-name LowMemoryAlarm \
  --metric-name MemoryUsage \
  --namespace AWS/EC2 \
  --threshold 50 \
  --comparison-operator GreaterThanThreshold

# List all alarms
awslocal cloudwatch describe-alarms

# Watch for state changes
watch "awslocal cloudwatch describe-alarms | jq '.MetricAlarms[] | {AlarmName, StateValue}'"
```

### Method 2: LocalStack Web UI
```
1. Navigate to http://localhost:4566
2. Go to: Resources → Management/Governance → CloudWatch Metrics
3. Click "Put Alarm"
4. Fill in form:
   - Alarm Name: HighCPUAlarm
   - Metric Name: CPUUtilization
   - Namespace: AWS/EC2
   - Threshold: 90
   - Comparison Operator: GreaterThanThreshold
   - Evaluation Periods: 1
5. Create multiple alarms with different thresholds (90, 70, 50)
6. Your app will ingest them on next scheduled poll (60s)
```

### Method 3: Programmatic (AWS CLI with LocalStack profile)
```bash
# Set up LocalStack profile
aws configure --profile localstack
AWS Access Key ID [None]: test
AWS Secret Access Key [None]: test
Default region name [None]: us-east-1

# Create alarms programmatically
aws --profile localstack cloudwatch put-metric-alarm \
  --alarm-name HighCPUAlarm \
  --metric-name CPUUtilization \
  --namespace AWS/EC2 \
  --threshold 90 \
  --comparison-operator GreaterThanThreshold

# Quick script to create all three severities
cat > create-test-alarms.sh << 'EOF'
#!/bin/bash
aws --profile localstack cloudwatch put-metric-alarm \
  --alarm-name HighCPUAlarm --metric-name CPUUtilization \
  --namespace AWS/EC2 --threshold 90 \
  --comparison-operator GreaterThanThreshold

aws --profile localstack cloudwatch put-metric-alarm \
  --alarm-name MediumDiskAlarm --metric-name DiskUsage \
  --namespace AWS/EC2 --threshold 70 \
  --comparison-operator GreaterThanThreshold

aws --profile localstack cloudwatch put-metric-alarm \
  --alarm-name LowMemoryAlarm --metric-name MemoryUsage \
  --namespace AWS/EC2 --threshold 50 \
  --comparison-operator GreaterThanThreshold
EOF

chmod +x create-test-alarms.sh
./create-test-alarms.sh
```

---

## Feature 4: Quarkus Amazon Services Integration

### Goal
Auto-start LocalStack in dev mode, use real AWS in production

### Add Dependency
```xml
<dependency>
    <groupId>io.quarkiverse.amazonservices</groupId>
    <artifactId>quarkus-amazon-cloudwatch</artifactId>
    <!-- Version managed by Quarkus BOM -->
</dependency>

<dependency>
    <groupId>software.amazon.awssdk</groupId>
    <artifactId>url-connection-client</artifactId>
</dependency>
```

### Dev Configuration (`application-dev.properties`)
```properties
# Enable Dev Services (auto-starts LocalStack)
quarkus.cloudwatch.devservices.enabled=true

# LocalStack endpoint (auto-configured by Dev Services)
# quarkus.cloudwatch.endpoint-override=http://localhost:4566

# Region
quarkus.cloudwatch.aws.region=us-east-1

# Static credentials (for LocalStack)
quarkus.cloudwatch.aws.credentials.type=static
quarkus.cloudwatch.aws.credentials.static-provider.access-key-id=test-key
quarkus.cloudwatch.aws.credentials.static-provider.secret-access-key=test-secret

# Mock client override (if using MockCloudWatchAlarmClient)
quarkus.devservices.cloudwatch.mock.enabled=true
quarkus.devservices.cloudwatch.mock.mode=multiple-severities
```

### Production Configuration (`application-prod.properties`)
```properties
# Disable Dev Services (use real AWS)
# quarkus.cloudwatch.devservices.enabled=false

# No endpoint override (auto-detects AWS)
# quarkus.cloudwatch.endpoint-override=http://localhost:4566

# Region (your AWS region)
quarkus.cloudwatch.aws.region=us-east-1

# Use default credential chain
# quarkus.cloudwatch.aws.credentials.type=default

# Credentials from:
# - AWS_ACCESS_KEY_ID / AWS_SECRET_ACCESS_KEY (env vars)
# - ~/.aws/credentials file
# - Java system properties
# - ECS instance role
# - EC2 instance profile
```

### Benefits
- ✅ Zero code changes between dev and prod
- ✅ Automatic LocalStack management in dev
- ✅ Seamless switch via environment variables
- ✅ Supports all credential providers

---

## Feature 5: Programmatic Simulation Tool (Optional Enhancement)

### Goal
Kotlin utility class for creating test alarms programmatically

### Implementation
```kotlin
@ApplicationScoped
class CloudWatchSimulationTool(
    private val cloudWatchClient: CloudWatchClient  // Direct SDK for simulation
) {
    fun createTestAlarms(severities: List<Severity>): List<String> {
        return severities.map { severity ->
            val threshold = when (severity) {
                Severity.HIGH -> 90.0
                Severity.MEDIUM -> 70.0
                Severity.LOW -> 50.0
                else -> 30.0
            }

            cloudWatchClient.putMetricAlarm {
                PutMetricAlarmRequest.builder()
                    .alarmName("${severity.name}TestAlarm")
                    .metricName("TestMetric")
                    .namespace("TestNamespace")
                    .threshold(threshold)
                    .comparisonOperator(ComparisonOperator.GREATER_THAN_THRESHOLD)
                    .evaluationPeriods(1)
                    .period(300)
                    .build()
            }

            "${severity.name}TestAlarm"
        }
    }
}
```

### REST Endpoint for Simulation
```kotlin
@Path("/test/cloudwatch")
class CloudWatchTestResource(
    private val simulationTool: CloudWatchSimulationTool
) {
    @POST
    @Path("/create-alarms")
    fun createTestAlarms(@QueryParam("severities") severityStr: String): Response<List<String>> {
        val severities = parseSeverities(severityStr)  // "HIGH,MEDIUM,LOW" -> list
        val alarmNames = simulationTool.createTestAlarms(severities)
        return Response.ok(alarmNames).build()
    }
}
```

### Usage
```bash
# Create all severity alarms via REST
curl -X POST "http://localhost:8080/test/cloudwatch/create-alarms?severities=HIGH,MEDIUM,LOW"

# Your scheduled job will ingest them on next poll (max 60s wait)
```

---

## Implementation Order (Recommended)

1. **Phase 1: Training Data Endpoint** (Feature 1)
   - Create `TrainingIncidentResource`
   - Create `TrainingIncidentRequest` DTO
   - Add JSON logging to file
   - Test with sample JSON

2. **Phase 2: Mock Client** (Feature 2)
   - Create `MockCloudWatchAlarmClient`
   - Add test config properties
   - Create test fixture files
   - Test severity scenarios

3. **Phase 3: Quarkus Amazon Services** (Feature 4)
   - Add dependencies to `pom.xml`
   - Create `application-dev.properties`
   - Create `application-prod.properties`
   - Test LocalStack auto-start

4. **Phase 4: Documentation** (Feature 3)
   - Create `docs/cloudwatch-simulation.md`
   - Add CLI examples
   - Add Web UI screenshots
   - Add shell scripts

5. **Phase 5: Programmatic Tool** (Feature 5 - Optional)
   - Create `CloudWatchSimulationTool`
   - Create REST endpoint
   - Test automation

---

## Testing Strategy

### Training Data Endpoint
```kotlin
@Test
class TrainingIncidentResourceTest {
    @Test
    fun `creates incident from valid training JSON`() {
        // POST to /training/incidents
        // Verify incident created
        // Verify JSON logged to file
    }

    @Test
    fun `handles stack trace correctly`() {
        // POST with stack trace
        // Verify stored in incident description
    }

    @Test
    fun `validates severity enum`() {
        // POST with invalid severity
        // Verify 400 error
    }
}
```

### Mock Client
```kotlin
@Test
class MockCloudWatchAlarmClientTest {
    @Test
    fun `returns multiple severities when configured`() {
        // Set mode to "multiple-severities"
        // Call listAlarmsInAlarmState()
        // Verify 3 alarms with different thresholds
    }

    @Test
    fun `returns no alarms in no-alarms mode`() {
        // Set mode to "no-alarms"
        // Verify empty list
    }
}
```

### Integration Tests
```kotlin
@QuarkusTest
class CloudWatchIngestionIntegrationTest {
    @Test
    fun `ingests alarms from LocalStack`() {
        // Create alarms via awslocal
        // Wait for scheduled job (up to 60s)
        // Verify incidents created
        // Verify severity mapping correct
    }
}
```

---

## Open Questions for Refinement

### 1. Training Data Logging
Where should JSON files be stored?
- `logs/training-incidents-{date}.jsonl` (rotated daily)?
- Single file: `logs/training-incidents.jsonl`?
- Database table: `training_incidents_audit`?

### 2. Mock Client Priority
Should be mock client always be enabled in dev, or only when explicitly configured?
- Always enabled in `%dev` profile?
- Only when `quarkus.devservices.cloudwatch.mock.enabled=true`?

### 3. Severity Test Scenarios
Besides HIGH (90), MEDIUM (70), LOW (50), do you need:
- INFO severity (default/no threshold)?
- Custom severity values?

### 4. Programmatic Tool Priority
Should Feature 5 be implemented now or deferred?

### 5. Documentation Location
Where should CloudWatch simulation guides go?
- `docs/cloudwatch-simulation.md` in repo?
- README section?
- Both?

---

## Summary

This implementation plan provides:

✅ **Training Data Ingestion**: REST endpoint for arbitrary JSON → LLM training
✅ **Mock CloudWatch Client**: Zero code changes, severity testing
✅ **Three Simulation Methods**: CLI, Web UI, Programmatic
✅ **Easy Dev-to-Prod Switch**: Configuration only, no code changes
✅ **Clean Architecture**: Uses existing domain models, independent features
✅ **Comprehensive Testing**: Unit, integration, and manual testing strategies

All features are independent and can be implemented in phases based on priority.
