# CloudWatch Simulation Guide

This guide provides three methods for simulating CloudWatch alarms during development and testing. All methods are compatible with your existing `CloudWatchIngestionService` - no code changes required.

---

## Method 1: Mock CloudWatch Alarm Client (Test Mode)

### Overview
The MockCloudWatchAlarmClient is automatically enabled in **test mode only** (`test` profile) via `@IfBuildProfile("test")` and returns predefined test alarms with HIGH, MEDIUM, and LOW severities.

### Configuration
No additional configuration needed - the mock client is only active when running tests (`mvn test`).

**Important**: The mock client uses `@IfBuildProfile("test")`, which means it's **inactive** in both dev and production modes. Dev Services CloudWatch will be used instead.

### Behavior
The mock client returns three alarms in ALARM state:
- **HighCPUAlarm**: CPU utilization > 90% (Severity: HIGH)
- **MediumDiskAlarm**: Disk usage > 70% (Severity: MEDIUM)
- **LowMemoryAlarm**: Memory usage > 50% (Severity: LOW)

### Testing
In tests, the mock client is automatically used. To verify:

```bash
mvn test
```

The tests will verify that mock alarms are returned correctly.

### Customizing Mock Data
Edit `src/main/kotlin/com/example/incidentanalyst/aws/mockCloudWatchAlarmClient.kt` to add new test scenarios.

### Using Dev Services (Automatic LocalStack)

Dev Services automatically starts LocalStack for CloudWatch when you run in dev mode:

1. Start the application in dev mode:
   ```bash
   mvn quarkus:dev
   ```

2. Quarkus will automatically start LocalStack on `localhost:4566`
3. Create alarms using Method 2 (LocalStack CLI) or Method 3 (LocalStack Web UI)
4. The application will connect to LocalStack automatically

**No manual configuration needed** - just run `mvn quarkus:dev`

The mock client is automatically disabled in dev mode (only active during `mvn test`).

---

## Method 2: LocalStack CLI (`awslocal`)

### Prerequisites
```bash
pip install awscli-local
```

### Start LocalStack
```bash
# Using Docker
docker run --rm --name local-cloudwatch \
  --publish 4566:4582 \
  -e SERVICES=cloudwatch \
  -e START_WEB=0 \
  -d localstack/localstack:3.7.2

# Or using LocalStack CLI (if installed)
pip install localstack
localstack start
```

### Configure AWS Profile
```bash
aws configure --profile localstack
AWS Access Key ID [None]: test
AWS Secret Access Key [None]: test
Default region name [None]: us-east-1
Default output format [None]: json
```

### Create Test Alarms

#### HIGH Severity Alarm
```bash
awslocal cloudwatch put-metric-alarm \
  --alarm-name HighCPUAlarm \
  --metric-name CPUUtilization \
  --namespace AWS/EC2 \
  --threshold 90 \
  --comparison-operator GreaterThanThreshold \
  --evaluation-periods 1 \
  --period 300
```

#### MEDIUM Severity Alarm
```bash
awslocal cloudwatch put-metric-alarm \
  --alarm-name MediumDiskAlarm \
  --metric-name DiskUsage \
  --namespace AWS/EC2 \
  --threshold 70 \
  --comparison-operator GreaterThanThreshold \
  --evaluation-periods 1 \
  --period 300
```

#### LOW Severity Alarm
```bash
awslocal cloudwatch put-metric-alarm \
  --alarm-name LowMemoryAlarm \
  --metric-name MemoryUsage \
  --namespace AWS/EC2 \
  --threshold 50 \
  --comparison-operator GreaterThanThreshold \
  --evaluation-periods 1 \
  --period 300
```

### List Alarms
```bash
# List all alarms
awslocal cloudwatch describe-alarms

# Filter by alarm name
awslocal cloudwatch describe-alarms --alarm-names HighCPUAlarm

# Show alarm details with jq
awslocal cloudwatch describe-alarms | jq '.MetricAlarms[] | {AlarmName, StateValue, Threshold}'
```

### Watch Alarm State Changes
```bash
watch -n 5 "awslocal cloudwatch describe-alarms | jq '.MetricAlarms[] | {AlarmName, StateValue}'"
```

### Quick Script
```bash
cat > create-test-alarms.sh << 'EOF'
#!/bin/bash
awslocal cloudwatch put-metric-alarm \
  --alarm-name HighCPUAlarm --metric-name CPUUtilization \
  --namespace AWS/EC2 --threshold 90 \
  --comparison-operator GreaterThanThreshold

awslocal cloudwatch put-metric-alarm \
  --alarm-name MediumDiskAlarm --metric-name DiskUsage \
  --namespace AWS/EC2 --threshold 70 \
  --comparison-operator GreaterThanThreshold

awslocal cloudwatch put-metric-alarm \
  --alarm-name LowMemoryAlarm --metric-name MemoryUsage \
  --namespace AWS/EC2 --threshold 50 \
  --comparison-operator GreaterThanThreshold

echo "Created 3 test alarms"
EOF

chmod +x create-test-alarms.sh
./create-test-alarms.sh
```

---

## Method 3: LocalStack Web UI

### Start LocalStack
```bash
docker run --rm --name local-cloudwatch \
  --publish 4566:4582 \
  -e SERVICES=cloudwatch \
  -d localstack/localstack:3.7.2
```

### Access Web UI
Navigate to: http://localhost:4566

### Create Alarms via Web UI

1. **Navigate to CloudWatch**
   - Go to: Resources → Management/Governance → CloudWatch Metrics

2. **Create HIGH Severity Alarm**
   - Click "Put Alarm"
   - Fill in form:
     - Alarm Name: `HighCPUAlarm`
     - Metric Name: `CPUUtilization`
     - Namespace: `AWS/EC2`
     - Threshold: `90`
     - Comparison Operator: `GreaterThanThreshold`
     - Evaluation Periods: `1`
     - Period: `300`
   - Click "Submit"

3. **Create MEDIUM Severity Alarm**
   - Repeat with threshold `70` and alarm name `MediumDiskAlarm`

4. **Create LOW Severity Alarm**
   - Repeat with threshold `50` and alarm name `LowMemoryAlarm`

5. **Verify Alarms**
   - Navigate to: CloudWatch Metrics → Alarms
   - You should see all three alarms in ALARM state

6. **Your Application Will Ingest**
   - Next scheduled job (within 60s) will pick up these alarms
   - Check application logs for ingestion success

---

## Production Deployment (Real AWS)

### Configuration Changes

When deploying to production, simply change your configuration to use real AWS credentials:

**application-prod.properties**
```properties
# Disable dev-mode scheduler (enable for prod)
quarkus.scheduler.enabled=true

# AWS Region
quarkus.cloudwatch.aws.region=us-east-1

# Use default credential chain (no static credentials needed)
# Credentials will be picked up from:
# - Environment variables: AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY
# - ~/.aws/credentials file
# - Java system properties
# - ECS instance role
# - EC2 instance profile

# No endpoint override needed (auto-detects AWS)
```

### Setting AWS Credentials

#### Option 1: Environment Variables
```bash
export AWS_ACCESS_KEY_ID=your_access_key
export AWS_SECRET_ACCESS_KEY=your_secret_key
export AWS_REGION=us-east-1
```

#### Option 2: AWS Credentials File
```bash
# ~/.aws/credentials
[default]
aws_access_key_id = your_access_key
aws_secret_access_key = your_secret_key

# ~/.aws/config
[default]
region = us-east-1
```

#### Option 3: Java System Properties
```bash
java -jar target/quarkus-app/quarkus-run.jar \
  -Daws.accessKeyId=your_access_key \
  -Daws.secretAccessKey=your_secret_key \
  -Daws.region=us-east-1
```

#### Option 4: IAM Role (ECS/EC2)
If running in ECS or EC2, ensure your instance/Task Role has:
- `cloudwatch:DescribeAlarms` permission
- `logs:DescribeLogGroups` and `logs:DescribeLogStreams` permissions

---

## Testing Your Integration

### With Mock Client (Dev Mode)
```bash
mvn quarkus:dev

# Wait up to 60s for scheduled job
# Check logs:
# "Successfully polled 3 CloudWatch alarms in ALARM state, mapped to 3 incidents"
```

### With LocalStack (Manual Testing)
```bash
# Terminal 1: Disable mock client temporarily
# Edit src/main/kotlin/.../aws/MockCloudWatchAlarmClient.kt
# Comment out @Alternative annotation

# Terminal 2: Start application - Dev Services will auto-start LocalStack
mvn quarkus:dev

# Terminal 3: Create test alarms (use regular AWS CLI pointing to LocalStack)
aws --endpoint-url http://localhost:4566 cloudwatch put-metric-alarm \
  --alarm-name HighCPUAlarm --metric-name CPUUtilization \
  --namespace AWS/EC2 --threshold 90 \
  --comparison-operator GreaterThanThreshold

# Wait up to 60s for ingestion
```

**Note**: With Dev Services enabled, you don't need to manually start LocalStack. Quarkus will automatically start a LocalStack container on `localhost:4566` when you run `mvn quarkus:dev`.

### With Real AWS (Production)
```bash
# Set AWS credentials
export AWS_ACCESS_KEY_ID=your_key
export AWS_SECRET_ACCESS_KEY=your_secret

# Run application
mvn quarkus:dev

# Verify ingestion
curl http://localhost:8080/incidents | jq '.[] | {title, severity}'
```

---

## Troubleshooting

### Mock Client Not Working
- Ensure `@Alternative` and `@Priority(1)` annotations are present
- Check that you're running in dev mode (not test or prod)
- Verify no conflicts with `AwsCloudWatchAlarmClient`

### Dev Services LocalStack Not Starting
- Check that Dev Services is enabled: `%dev.quarkus.cloudwatch.devservices.enabled=true`
- Verify port 4566 is not already in use: `lsof -i :4566` (or `netstat -tulpn | grep 4566`)
- Check Docker is running: `docker ps`
- Verify endpoint override is correct: `http://localhost:4566`

### LocalStack Connection Errors (Manual Setup)
- Ensure LocalStack is running: `docker ps | grep localstack`
- Check port 4566 is accessible: `curl http://localhost:4566/_localstack/health`
- Verify endpoint configuration in `application.properties`

### AWS Credentials Errors
- Verify credentials format (no extra spaces)
- Check IAM permissions for CloudWatch access
- Ensure region is correct: `aws sts get-caller-identity`

### No Alarms Ingested
- Check scheduler is enabled: `quarkus.scheduler.enabled=true`
- Verify CloudWatch alarm state is ALARM (not OK or INSUFFICIENT_DATA)
- Review application logs for errors
- Try manual trigger: Create an alarm via CLI and wait 60s

---

## Additional Resources

- [AWS CloudWatch Documentation](https://docs.aws.amazon.com/cloudwatch/)
- [LocalStack Documentation](https://docs.localstack.cloud/)
- [Quarkus Amazon Services Guide](https://docs.quarkiverse.io/quarkus-amazon-services/)
- [AWS CLI Reference](https://docs.aws.amazon.com/cli/)
