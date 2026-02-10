package com.example.incidentanalyst.aws

import com.example.incidentanalyst.incident.Incident
import com.example.incidentanalyst.incident.IncidentId
import com.example.incidentanalyst.incident.IncidentService
import com.example.incidentanalyst.incident.IncidentStatus
import com.example.incidentanalyst.incident.Severity
import io.quarkus.test.InjectMock
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import java.time.Instant

@QuarkusTest
class CloudWatchTestDataGeneratorServiceTest {

    @InjectMock
    lateinit var incidentService: IncidentService

    @Inject
    lateinit var generatorService: CloudWatchTestDataGeneratorService

    @BeforeEach
    fun setup() {
        reset(incidentService)
    }

    @Test
    fun `generateAlarms returns ValidationError when count is negative`() {
        val request = CloudWatchTestDataRequestDto(count = -1)

        val result = generatorService.generateAlarms(request)

        assertTrue(result is CloudWatchTestDataGenerationResult.ValidationError)
        val error = result as CloudWatchTestDataGenerationResult.ValidationError
        assertTrue(error.message.contains("non-negative"))
    }

    @Test
    fun `generateAlarms returns ValidationError when count exceeds 1000`() {
        val request = CloudWatchTestDataRequestDto(count = 1001)

        val result = generatorService.generateAlarms(request)

        assertTrue(result is CloudWatchTestDataGenerationResult.ValidationError)
        val error = result as CloudWatchTestDataGenerationResult.ValidationError
        assertTrue(error.message.contains("exceed 1000"))
    }

    @Test
    fun `generateAlarms returns ValidationError when startTime is after endTime`() {
        val request = CloudWatchTestDataRequestDto(
            count = 10,
            startTime = Instant.parse("2024-01-02T00:00:00Z"),
            endTime = Instant.parse("2024-01-01T00:00:00Z")
        )

        val result = generatorService.generateAlarms(request)

        assertTrue(result is CloudWatchTestDataGenerationResult.ValidationError)
        val error = result as CloudWatchTestDataGenerationResult.ValidationError
        assertTrue(error.message.contains("startTime cannot be after endTime"))
    }

    @Test
    fun `generateAlarms returns ValidationError when minSeverity is greater than maxSeverity`() {
        val request = CloudWatchTestDataRequestDto(
            count = 10,
            minSeverity = Severity.HIGH,
            maxSeverity = Severity.LOW
        )

        val result = generatorService.generateAlarms(request)

        assertTrue(result is CloudWatchTestDataGenerationResult.ValidationError)
        val error = result as CloudWatchTestDataGenerationResult.ValidationError
        assertTrue(error.message.contains("minSeverity cannot be greater than maxSeverity"))
    }

    @Test
    fun `generateAlarms returns ValidationError when no valid severities available`() {
        val request = CloudWatchTestDataRequestDto(
            count = 10,
            severities = listOf(Severity.CRITICAL),
            maxSeverity = Severity.LOW
        )

        val result = generatorService.generateAlarms(request)

        assertTrue(result is CloudWatchTestDataGenerationResult.ValidationError)
        val error = result as CloudWatchTestDataGenerationResult.ValidationError
        assertTrue(error.message.contains("No valid severities available"))
    }

    @Test
    fun `generateAlarms returns Success with zero count`() {
        val request = CloudWatchTestDataRequestDto(count = 0)

        val result = generatorService.generateAlarms(request)

        assertTrue(result is CloudWatchTestDataGenerationResult.Success)
        val success = result as CloudWatchTestDataGenerationResult.Success
        assertEquals(0, success.generatedCount)
        assertEquals(emptyMap<String, Int>(), success.severityBreakdown)
        assertEquals(emptyList<Long>(), success.createdIncidentIds)
    }

    @Test
    fun `generateAlarms returns createdIncidentIds`() {
        val timestamp = Instant.parse("2024-01-01T00:00:00Z")
        val incident1 = baseIncident(timestamp, IncidentId(100))
        val incident2 = baseIncident(timestamp, IncidentId(101))
        whenever(incidentService.create(any<Incident>()))
            .thenReturn(incident1, incident2)

        val request = CloudWatchTestDataRequestDto(count = 2)

        val result = generatorService.generateAlarms(request)

        assertTrue(result is CloudWatchTestDataGenerationResult.Success)
        val success = result as CloudWatchTestDataGenerationResult.Success
        assertEquals(2, success.createdIncidentIds.size)
        assertTrue(success.createdIncidentIds.containsAll(listOf(100, 101)))
    }

    @Test
    fun `generateAlarms returns severity breakdown for created incidents`() {
        whenever(incidentService.create(any<Incident>()))
            .thenAnswer { invocation -> invocation.getArgument(0) as Incident }

        val request = CloudWatchTestDataRequestDto(
            count = 5,
            seed = 12345L
        )

        val result = generatorService.generateAlarms(request)

        assertTrue(result is CloudWatchTestDataGenerationResult.Success)
        val success = result as CloudWatchTestDataGenerationResult.Success
        assertEquals(5, success.severityBreakdown.values.sum())
    }

    @Test
    fun `generateAlarms includes seedUsed when seed is provided`() {
        whenever(incidentService.create(any<Incident>()))
            .thenAnswer { invocation -> invocation.getArgument(0) as Incident }

        val request = CloudWatchTestDataRequestDto(
            count = 3,
            seed = 777L
        )

        val result = generatorService.generateAlarms(request)

        assertTrue(result is CloudWatchTestDataGenerationResult.Success)
        val success = result as CloudWatchTestDataGenerationResult.Success
        assertEquals(777L, success.seedUsed)
    }

    @Test
    fun `generateAlarms has seedUsed null when seed is not provided`() {
        whenever(incidentService.create(any<Incident>()))
            .thenAnswer { invocation -> invocation.getArgument(0) as Incident }

        val request = CloudWatchTestDataRequestDto(count = 1)

        val result = generatorService.generateAlarms(request)

        assertTrue(result is CloudWatchTestDataGenerationResult.Success)
        val success = result as CloudWatchTestDataGenerationResult.Success
        assertEquals(null, success.seedUsed)
    }

    @Test
    fun `generateAlarms is deterministic with same seed`() {
        whenever(incidentService.create(any<Incident>()))
            .thenAnswer { invocation -> invocation.getArgument(0) as Incident }

        val request = CloudWatchTestDataRequestDto(
            count = 5,
            seed = 12345L
        )

        val result1 = generatorService.generateAlarms(request)
        val result2 = generatorService.generateAlarms(request)

        assertTrue(result1 is CloudWatchTestDataGenerationResult.Success)
        assertTrue(result2 is CloudWatchTestDataGenerationResult.Success)
        val success1 = result1 as CloudWatchTestDataGenerationResult.Success
        val success2 = result2 as CloudWatchTestDataGenerationResult.Success

        assertEquals(success1.severityBreakdown, success2.severityBreakdown)
        assertEquals(success1.generatedCount, success2.generatedCount)
    }

    @Test
    fun `mapAlarmToIncident maps ALARM state to incident`() {
        val alarm = AlarmDto(
            alarmName = "CPUAlarm",
            alarmDescription = "High CPU",
            stateValue = "ALARM",
            stateReason = "Threshold crossed",
            stateUpdatedTimestamp = Instant.parse("2024-01-01T00:00:00Z"),
            metricName = "CPUUtilization",
            namespace = "AWS/EC2",
            threshold = "90",
            comparisonOperator = "GreaterThanThreshold"
        )

        val result = generatorService.mapAlarmToIncident(alarm)

        assertTrue(result != null)
        assertEquals("cloudwatch", result!!.source)
        assertEquals("CPUAlarm", result.title)
        assertEquals(Severity.HIGH, result.severity)
    }

    @Test
    fun `mapAlarmToIncident returns null for null stateValue`() {
        val alarm = AlarmDto(
            alarmName = "CPUAlarm",
            alarmDescription = "High CPU",
            stateValue = null,
            stateReason = "Threshold crossed",
            stateUpdatedTimestamp = Instant.parse("2024-01-01T00:00:00Z"),
            metricName = "CPUUtilization",
            namespace = "AWS/EC2",
            threshold = "90",
            comparisonOperator = "GreaterThanThreshold"
        )

        val result = generatorService.mapAlarmToIncident(alarm)

        assertEquals(null, result)
    }

    @Test
    fun `mapAlarmToIncident returns null for non-ALARM state`() {
        val alarm = AlarmDto(
            alarmName = "CPUAlarm",
            alarmDescription = "High CPU",
            stateValue = "OK",
            stateReason = "Normal",
            stateUpdatedTimestamp = Instant.parse("2024-01-01T00:00:00Z"),
            metricName = "CPUUtilization",
            namespace = "AWS/EC2",
            threshold = "90",
            comparisonOperator = "GreaterThanThreshold"
        )

        val result = generatorService.mapAlarmToIncident(alarm)

        assertEquals(null, result)
    }

    private fun baseIncident(timestamp: Instant, id: IncidentId): Incident = Incident(
        id = id,
        source = "cloudwatch",
        title = "Test Alarm",
        description = "Test description",
        severity = Severity.HIGH,
        status = IncidentStatus.Open,
        createdAt = timestamp,
        updatedAt = timestamp
    )
}
