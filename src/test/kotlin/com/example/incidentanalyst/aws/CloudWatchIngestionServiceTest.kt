package com.example.incidentanalyst.aws

import com.example.incidentanalyst.common.Either
import com.example.incidentanalyst.incident.Incident
import com.example.incidentanalyst.incident.IncidentService
import com.example.incidentanalyst.incident.Severity
import io.quarkus.test.InjectMock
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.Mockito.reset
import java.time.Instant

@QuarkusTest
class CloudWatchIngestionServiceTest {

    @InjectMock
    lateinit var cloudWatchAlarmClient: CloudWatchAlarmClient

    @InjectMock
    lateinit var incidentService: IncidentService

    @Inject
    lateinit var cloudWatchIngestionService: CloudWatchIngestionService

    @BeforeEach
    fun setup() {
        reset(cloudWatchAlarmClient)
        reset(incidentService)
    }

    @Test
    fun `mapAlarmToIncident returns null for null stateValue`() {
        val alarm = AlarmDto(
            alarmName = "TestAlarm",
            alarmDescription = "Test description",
            stateValue = null,
            stateReason = "Test reason",
            stateUpdatedTimestamp = Instant.now(),
            metricName = "CPUUtilization",
            namespace = "AWS/EC2",
            threshold = "90",
            comparisonOperator = "GreaterThanThreshold"
        )

        val result = cloudWatchIngestionService.mapAlarmToIncident(alarm)

        assertNull(result)
    }

    @Test
    fun `mapAlarmToIncident returns null for OK state`() {
        val alarm = AlarmDto(
            alarmName = "TestAlarm",
            alarmDescription = "Test description",
            stateValue = "OK",
            stateReason = "Test reason",
            stateUpdatedTimestamp = Instant.now(),
            metricName = "CPUUtilization",
            namespace = "AWS/EC2",
            threshold = "90",
            comparisonOperator = "GreaterThanThreshold"
        )

        val result = cloudWatchIngestionService.mapAlarmToIncident(alarm)

        assertNull(result)
    }

    @Test
    fun `mapAlarmToIncident returns null for INSUFFICIENT_DATA state`() {
        val alarm = AlarmDto(
            alarmName = "TestAlarm",
            alarmDescription = "Test description",
            stateValue = "INSUFFICIENT_DATA",
            stateReason = "Test reason",
            stateUpdatedTimestamp = Instant.now(),
            metricName = "CPUUtilization",
            namespace = "AWS/EC2",
            threshold = "90",
            comparisonOperator = "GreaterThanThreshold"
        )

        val result = cloudWatchIngestionService.mapAlarmToIncident(alarm)

        assertNull(result)
    }

    @Test
    fun `mapAlarmToIncident maps ALARM state to incident`() {
        val alarm = AlarmDto(
            alarmName = "TestAlarm",
            alarmDescription = "Test description",
            stateValue = "ALARM",
            stateReason = "Crossed threshold",
            stateUpdatedTimestamp = Instant.parse("2024-01-15T10:30:00Z"),
            metricName = "CPUUtilization",
            namespace = "AWS/EC2",
            threshold = "90",
            comparisonOperator = "GreaterThanThreshold"
        )

        val result = cloudWatchIngestionService.mapAlarmToIncident(alarm)

        assertNotNull(result)
        assertEquals("cloudwatch", result!!.source)
        assertEquals("TestAlarm", result.title)
    }

    @Test
    fun `mapAlarmToIncident handles case-insensitive ALARM state`() {
        val alarm = AlarmDto(
            alarmName = "TestAlarm",
            alarmDescription = "Test description",
            stateValue = "alarm",
            stateReason = "Test reason",
            stateUpdatedTimestamp = Instant.now(),
            metricName = "CPUUtilization",
            namespace = "AWS/EC2",
            threshold = "90",
            comparisonOperator = "GreaterThanThreshold"
        )

        val result = cloudWatchIngestionService.mapAlarmToIncident(alarm)

        assertNotNull(result)
    }

    @Test
    fun `mapAlarmToIncident uses alarm timestamp when available`() {
        val alarmTimestamp = Instant.parse("2024-01-15T10:30:00Z")
        val alarm = AlarmDto(
            alarmName = "TestAlarm",
            alarmDescription = "Test description",
            stateValue = "ALARM",
            stateReason = "Crossed threshold",
            stateUpdatedTimestamp = alarmTimestamp,
            metricName = "CPUUtilization",
            namespace = "AWS/EC2",
            threshold = "90",
            comparisonOperator = "GreaterThanThreshold"
        )

        val result = cloudWatchIngestionService.mapAlarmToIncident(alarm)

        assertNotNull(result)
        assertEquals(alarmTimestamp, result!!.createdAt)
        assertEquals(alarmTimestamp, result.updatedAt)
    }

    @Test
    fun `mapAlarmToIncident uses current time when timestamp is null`() {
        val alarm = AlarmDto(
            alarmName = "TestAlarm",
            alarmDescription = "Test description",
            stateValue = "ALARM",
            stateReason = "Test reason",
            stateUpdatedTimestamp = null,
            metricName = "CPUUtilization",
            namespace = "AWS/EC2",
            threshold = "90",
            comparisonOperator = "GreaterThanThreshold"
        )

        val result = cloudWatchIngestionService.mapAlarmToIncident(alarm)

        assertNotNull(result)
        assertNotNull(result!!.createdAt)
        assertNotNull(result.updatedAt)
        // Timestamps should be recent (within last 5 seconds)
        val fiveSecondsAgo = Instant.now().minusSeconds(5)
        assertTrue(result.createdAt >= fiveSecondsAgo)
    }

    @Test
    fun `mapAlarmToIncident uses Unknown Alarm when alarmName is null`() {
        val alarm = AlarmDto(
            alarmName = null,
            alarmDescription = "Test description",
            stateValue = "ALARM",
            stateReason = "Test reason",
            stateUpdatedTimestamp = Instant.now(),
            metricName = "CPUUtilization",
            namespace = "AWS/EC2",
            threshold = "90",
            comparisonOperator = "GreaterThanThreshold"
        )

        val result = cloudWatchIngestionService.mapAlarmToIncident(alarm)

        assertNotNull(result)
        assertEquals("Unknown Alarm", result!!.title)
    }

    @Test
    fun `deriveSeverity returns HIGH for threshold greater than or equal 90`() {
        val alarm = AlarmDto(
            alarmName = "TestAlarm",
            alarmDescription = "Test description",
            stateValue = "ALARM",
            stateReason = "Test reason",
            stateUpdatedTimestamp = Instant.now(),
            metricName = "CPUUtilization",
            namespace = "AWS/EC2",
            threshold = "90",
            comparisonOperator = "GreaterThanThreshold"
        )

        val result = cloudWatchIngestionService.mapAlarmToIncident(alarm)

        assertNotNull(result)
        assertEquals(Severity.HIGH, result!!.severity)
    }

    @Test
    fun `deriveSeverity returns HIGH for threshold greater than 90`() {
        val alarm = AlarmDto(
            alarmName = "TestAlarm",
            alarmDescription = "Test description",
            stateValue = "ALARM",
            stateReason = "Test reason",
            stateUpdatedTimestamp = Instant.now(),
            metricName = "CPUUtilization",
            namespace = "AWS/EC2",
            threshold = "95",
            comparisonOperator = "GreaterThanThreshold"
        )

        val result = cloudWatchIngestionService.mapAlarmToIncident(alarm)

        assertNotNull(result)
        assertEquals(Severity.HIGH, result!!.severity)
    }

    @Test
    fun `deriveSeverity returns MEDIUM for threshold between 70 and 89`() {
        val alarm = AlarmDto(
            alarmName = "TestAlarm",
            alarmDescription = "Test description",
            stateValue = "ALARM",
            stateReason = "Test reason",
            stateUpdatedTimestamp = Instant.now(),
            metricName = "CPUUtilization",
            namespace = "AWS/EC2",
            threshold = "75",
            comparisonOperator = "GreaterThanThreshold"
        )

        val result = cloudWatchIngestionService.mapAlarmToIncident(alarm)

        assertNotNull(result)
        assertEquals(Severity.MEDIUM, result!!.severity)
    }

    @Test
    fun `deriveSeverity returns MEDIUM for threshold exactly 70`() {
        val alarm = AlarmDto(
            alarmName = "TestAlarm",
            alarmDescription = "Test description",
            stateValue = "ALARM",
            stateReason = "Test reason",
            stateUpdatedTimestamp = Instant.now(),
            metricName = "CPUUtilization",
            namespace = "AWS/EC2",
            threshold = "70",
            comparisonOperator = "GreaterThanThreshold"
        )

        val result = cloudWatchIngestionService.mapAlarmToIncident(alarm)

        assertNotNull(result)
        assertEquals(Severity.MEDIUM, result!!.severity)
    }

    @Test
    fun `deriveSeverity returns LOW for threshold between 50 and 69`() {
        val alarm = AlarmDto(
            alarmName = "TestAlarm",
            alarmDescription = "Test description",
            stateValue = "ALARM",
            stateReason = "Test reason",
            stateUpdatedTimestamp = Instant.now(),
            metricName = "CPUUtilization",
            namespace = "AWS/EC2",
            threshold = "60",
            comparisonOperator = "GreaterThanThreshold"
        )

        val result = cloudWatchIngestionService.mapAlarmToIncident(alarm)

        assertNotNull(result)
        assertEquals(Severity.LOW, result!!.severity)
    }

    @Test
    fun `deriveSeverity returns LOW for threshold exactly 50`() {
        val alarm = AlarmDto(
            alarmName = "TestAlarm",
            alarmDescription = "Test description",
            stateValue = "ALARM",
            stateReason = "Test reason",
            stateUpdatedTimestamp = Instant.now(),
            metricName = "CPUUtilization",
            namespace = "AWS/EC2",
            threshold = "50",
            comparisonOperator = "GreaterThanThreshold"
        )

        val result = cloudWatchIngestionService.mapAlarmToIncident(alarm)

        assertNotNull(result)
        assertEquals(Severity.LOW, result!!.severity)
    }

    @Test
    fun `deriveSeverity returns INFO for threshold below 50`() {
        val alarm = AlarmDto(
            alarmName = "TestAlarm",
            alarmDescription = "Test description",
            stateValue = "ALARM",
            stateReason = "Test reason",
            stateUpdatedTimestamp = Instant.now(),
            metricName = "CPUUtilization",
            namespace = "AWS/EC2",
            threshold = "45",
            comparisonOperator = "GreaterThanThreshold"
        )

        val result = cloudWatchIngestionService.mapAlarmToIncident(alarm)

        assertNotNull(result)
        assertEquals(Severity.INFO, result!!.severity)
    }

    @Test
    fun `deriveSeverity returns INFO for non-GreaterThanThreshold operator`() {
        val alarm = AlarmDto(
            alarmName = "TestAlarm",
            alarmDescription = "Test description",
            stateValue = "ALARM",
            stateReason = "Test reason",
            stateUpdatedTimestamp = Instant.now(),
            metricName = "CPUUtilization",
            namespace = "AWS/EC2",
            threshold = "90",
            comparisonOperator = "LessThanThreshold"
        )

        val result = cloudWatchIngestionService.mapAlarmToIncident(alarm)

        assertNotNull(result)
        assertEquals(Severity.INFO, result!!.severity)
    }

    @Test
    fun `deriveSeverity returns INFO for null comparisonOperator`() {
        val alarm = AlarmDto(
            alarmName = "TestAlarm",
            alarmDescription = "Test description",
            stateValue = "ALARM",
            stateReason = "Test reason",
            stateUpdatedTimestamp = Instant.now(),
            metricName = "CPUUtilization",
            namespace = "AWS/EC2",
            threshold = "90",
            comparisonOperator = null
        )

        val result = cloudWatchIngestionService.mapAlarmToIncident(alarm)

        assertNotNull(result)
        assertEquals(Severity.INFO, result!!.severity)
    }

    @Test
    fun `deriveSeverity returns INFO for invalid threshold`() {
        val alarm = AlarmDto(
            alarmName = "TestAlarm",
            alarmDescription = "Test description",
            stateValue = "ALARM",
            stateReason = "Test reason",
            stateUpdatedTimestamp = Instant.now(),
            metricName = "CPUUtilization",
            namespace = "AWS/EC2",
            threshold = "invalid",
            comparisonOperator = "GreaterThanThreshold"
        )

        val result = cloudWatchIngestionService.mapAlarmToIncident(alarm)

        assertNotNull(result)
        assertEquals(Severity.INFO, result!!.severity)
    }

    @Test
    fun `deriveSeverity returns INFO for null threshold`() {
        val alarm = AlarmDto(
            alarmName = "TestAlarm",
            alarmDescription = "Test description",
            stateValue = "ALARM",
            stateReason = "Test reason",
            stateUpdatedTimestamp = Instant.now(),
            metricName = "CPUUtilization",
            namespace = "AWS/EC2",
            threshold = null,
            comparisonOperator = "GreaterThanThreshold"
        )

        val result = cloudWatchIngestionService.mapAlarmToIncident(alarm)

        assertNotNull(result)
        assertEquals(Severity.INFO, result!!.severity)
    }

    @Test
    fun `buildAlarmDescription includes all required fields`() {
        val alarm = AlarmDto(
            alarmName = "CPUAlarm",
            alarmDescription = "High CPU usage",
            stateValue = "ALARM",
            stateReason = "Threshold crossed",
            stateUpdatedTimestamp = Instant.parse("2024-01-15T10:30:00Z"),
            metricName = "CPUUtilization",
            namespace = "AWS/EC2",
            threshold = "90",
            comparisonOperator = "GreaterThanThreshold"
        )

        val result = cloudWatchIngestionService.mapAlarmToIncident(alarm)

        assertNotNull(result)
        val description = result!!.description
        assertTrue(description.contains("Alarm Name: CPUAlarm"))
        assertTrue(description.contains("Alarm Description: High CPU usage"))
        assertTrue(description.contains("State Reason: Threshold crossed"))
        assertTrue(description.contains("Namespace: AWS/EC2"))
        assertTrue(description.contains("Metric Name: CPUUtilization"))
        assertTrue(description.contains("Threshold: 90"))
        assertTrue(description.contains("Comparison Operator: GreaterThanThreshold"))
        assertTrue(description.contains("State Updated Timestamp: 2024-01-15T10:30:00Z"))
    }

    @Test
    fun `buildAlarmDescription handles null fields with UNKNOWN`() {
        val alarm = AlarmDto(
            alarmName = null,
            alarmDescription = null,
            stateValue = "ALARM",
            stateReason = null,
            stateUpdatedTimestamp = null,
            metricName = null,
            namespace = null,
            threshold = null,
            comparisonOperator = null
        )

        val result = cloudWatchIngestionService.mapAlarmToIncident(alarm)

        assertNotNull(result)
        val description = result!!.description
        assertTrue(description.contains("Alarm Name: UNKNOWN"))
        assertTrue(description.contains("Alarm Description: UNKNOWN"))
        assertTrue(description.contains("State Reason: UNKNOWN"))
        assertTrue(description.contains("Namespace: UNKNOWN"))
        assertTrue(description.contains("Metric Name: UNKNOWN"))
        assertTrue(description.contains("Threshold: UNKNOWN"))
        assertTrue(description.contains("Comparison Operator: UNKNOWN"))
        assertTrue(description.contains("State Updated Timestamp: UNKNOWN"))
    }

    @Test
    fun `deriveSeverity handles decimal thresholds correctly`() {
        val alarm = AlarmDto(
            alarmName = "TestAlarm",
            alarmDescription = "Test description",
            stateValue = "ALARM",
            stateReason = "Test reason",
            stateUpdatedTimestamp = Instant.now(),
            metricName = "CPUUtilization",
            namespace = "AWS/EC2",
            threshold = "92.5",
            comparisonOperator = "GreaterThanThreshold"
        )

        val result = cloudWatchIngestionService.mapAlarmToIncident(alarm)

        assertNotNull(result)
        assertEquals(Severity.HIGH, result!!.severity)
    }

    @Test
    fun `deriveSeverity is case-insensitive for comparisonOperator`() {
        val alarm = AlarmDto(
            alarmName = "TestAlarm",
            alarmDescription = "Test description",
            stateValue = "ALARM",
            stateReason = "Test reason",
            stateUpdatedTimestamp = Instant.now(),
            metricName = "CPUUtilization",
            namespace = "AWS/EC2",
            threshold = "90",
            comparisonOperator = "greaterthanthreshold"
        )

        val result = cloudWatchIngestionService.mapAlarmToIncident(alarm)

        assertNotNull(result)
        assertEquals(Severity.HIGH, result!!.severity)
    }

    @Test
    fun `ingestAlarms returns Success when no alarms`() {
        val queryResult = Either.Right(emptyList<AlarmDto>())
        org.mockito.Mockito.`when`(cloudWatchAlarmClient.listAlarmsInAlarmState())
            .thenReturn(queryResult)

        val result = cloudWatchIngestionService.ingestAlarms()

        assertTrue(result is Either.Right)
        val success = (result as Either.Right).value
        assertTrue(success is IngestionSuccess.NoNewAlarms)
    }

    @Test
    fun `ingestAlarms returns Failure on AWS Throttled error`() {
        val queryResult = Either.Left(AwsError.Throttled)
        org.mockito.Mockito.`when`(cloudWatchAlarmClient.listAlarmsInAlarmState())
            .thenReturn(queryResult)

        val result = cloudWatchIngestionService.ingestAlarms()

        assertTrue(result is Either.Left)
        val failure = (result as Either.Left).value
        assertTrue(failure is IngestionError.AwsError)
        val awsError = (failure as IngestionError.AwsError).error
        assertTrue(awsError is AwsError.Throttled)
    }

    @Test
    fun `ingestAlarms returns Failure on AWS Unauthorized error`() {
        val queryResult = Either.Left(AwsError.Unauthorized)
        org.mockito.Mockito.`when`(cloudWatchAlarmClient.listAlarmsInAlarmState())
            .thenReturn(queryResult)

        val result = cloudWatchIngestionService.ingestAlarms()

        assertTrue(result is Either.Left)
        val failure = (result as Either.Left).value
        assertTrue(failure is IngestionError.AwsError)
        val awsError = (failure as IngestionError.AwsError).error
        assertTrue(awsError is AwsError.Unauthorized)
    }

    @Test
    fun `ingestAlarms returns Failure on AWS NetworkError`() {
        val queryResult = Either.Left(AwsError.NetworkError)
        org.mockito.Mockito.`when`(cloudWatchAlarmClient.listAlarmsInAlarmState())
            .thenReturn(queryResult)

        val result = cloudWatchIngestionService.ingestAlarms()

        assertTrue(result is Either.Left)
        val failure = (result as Either.Left).value
        assertTrue(failure is IngestionError.AwsError)
        val awsError = (failure as IngestionError.AwsError).error
        assertTrue(awsError is AwsError.NetworkError)
    }

    @Test
    fun `ingestAlarms returns Failure on AWS ServiceUnavailable`() {
        val queryResult = Either.Left(AwsError.ServiceUnavailable)
        org.mockito.Mockito.`when`(cloudWatchAlarmClient.listAlarmsInAlarmState())
            .thenReturn(queryResult)

        val result = cloudWatchIngestionService.ingestAlarms()

        assertTrue(result is Either.Left)
        val failure = (result as Either.Left).value
        assertTrue(failure is IngestionError.AwsError)
        val awsError = (failure as IngestionError.AwsError).error
        assertTrue(awsError is AwsError.ServiceUnavailable)
    }

    @Test
    fun `ingestAlarms returns Failure on AWS Unknown error`() {
        val queryResult = Either.Left(AwsError.Unknown("Some error"))
        org.mockito.Mockito.`when`(cloudWatchAlarmClient.listAlarmsInAlarmState())
            .thenReturn(queryResult)

        val result = cloudWatchIngestionService.ingestAlarms()

        assertTrue(result is Either.Left)
        val failure = (result as Either.Left).value
        assertTrue(failure is IngestionError.AwsError)
        val awsError = (failure as IngestionError.AwsError).error
        assertTrue(awsError is AwsError.Unknown)
    }

    @Test
    fun `ingestAlarms returns PersistenceError when incidentService create throws`() {
        val alarm = AlarmDto(
            alarmName = "TestAlarm",
            alarmDescription = "Test description",
            stateValue = "ALARM",
            stateReason = "Test reason",
            stateUpdatedTimestamp = Instant.now(),
            metricName = "CPUUtilization",
            namespace = "AWS/EC2",
            threshold = "90",
            comparisonOperator = "GreaterThanThreshold"
        )
        val queryResult = Either.Right(listOf(alarm))
        org.mockito.Mockito.`when`(cloudWatchAlarmClient.listAlarmsInAlarmState())
            .thenReturn(queryResult)

        val expectedIncident = cloudWatchIngestionService.mapAlarmToIncident(alarm)
        assertNotNull(expectedIncident)

        org.mockito.Mockito.`when`(incidentService.create(expectedIncident!!))
            .thenThrow(RuntimeException("Database error"))

        val result = cloudWatchIngestionService.ingestAlarms()

        assertTrue(result is Either.Left)
        val failure = (result as Either.Left).value
        assertTrue(failure is IngestionError.PersistenceError)
        assertTrue(failure.toString().contains("Failed to persist incident"))
    }

    @Test
    fun `ingestAlarms handles multiple alarms with some null stateValue`() {
        val alarm1 = AlarmDto(
            alarmName = "Alarm1",
            alarmDescription = "Test description",
            stateValue = "ALARM",
            stateReason = "Test reason",
            stateUpdatedTimestamp = Instant.now(),
            metricName = "CPUUtilization",
            namespace = "AWS/EC2",
            threshold = "90",
            comparisonOperator = "GreaterThanThreshold"
        )
        val alarm2 = AlarmDto(
            alarmName = "Alarm2",
            alarmDescription = "Test description",
            stateValue = "OK",
            stateReason = "Test reason",
            stateUpdatedTimestamp = Instant.now(),
            metricName = "CPUUtilization",
            namespace = "AWS/EC2",
            threshold = "90",
            comparisonOperator = "GreaterThanThreshold"
        )
        val queryResult = Either.Right(listOf(alarm1, alarm2))
        org.mockito.Mockito.`when`(cloudWatchAlarmClient.listAlarmsInAlarmState())
            .thenReturn(queryResult)

        val result = cloudWatchIngestionService.ingestAlarms()

        assertTrue(result is Either.Right)
        val success = (result as Either.Right).value
        assertTrue(success is IngestionSuccess.NewIncidentsCreated)
        assertEquals(1, (success as IngestionSuccess.NewIncidentsCreated).count)
    }

    @Test
    fun `ingestAlarms handles multiple alarms successfully`() {
        val alarm1 = AlarmDto(
            alarmName = "Alarm1",
            alarmDescription = "Test description",
            stateValue = "ALARM",
            stateReason = "Test reason",
            stateUpdatedTimestamp = Instant.now(),
            metricName = "CPUUtilization",
            namespace = "AWS/EC2",
            threshold = "90",
            comparisonOperator = "GreaterThanThreshold"
        )
        val alarm2 = AlarmDto(
            alarmName = "Alarm2",
            alarmDescription = "Test description",
            stateValue = "ALARM",
            stateReason = "Test reason",
            stateUpdatedTimestamp = Instant.now(),
            metricName = "CPUUtilization",
            namespace = "AWS/EC2",
            threshold = "90",
            comparisonOperator = "GreaterThanThreshold"
        )
        val queryResult = Either.Right(listOf(alarm1, alarm2))
        org.mockito.Mockito.`when`(cloudWatchAlarmClient.listAlarmsInAlarmState())
            .thenReturn(queryResult)

        val result = cloudWatchIngestionService.ingestAlarms()

        assertTrue(result is Either.Right)
        val success = (result as Either.Right).value
        assertTrue(success is IngestionSuccess.NewIncidentsCreated)
        assertEquals(2, (success as IngestionSuccess.NewIncidentsCreated).count)
    }
}
