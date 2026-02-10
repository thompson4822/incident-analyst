package com.example.incidentanalyst.aws

import com.example.incidentanalyst.common.Either
import com.example.incidentanalyst.incident.Incident
import com.example.incidentanalyst.incident.IncidentId
import com.example.incidentanalyst.incident.IncidentRepository
import com.example.incidentanalyst.incident.IncidentService
import com.example.incidentanalyst.incident.Severity
import io.quarkus.test.InjectMock
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import java.time.Instant

@QuarkusTest
class CloudWatchIngestionIntegrationTest {

    @InjectMock
    lateinit var cloudWatchAlarmClient: CloudWatchAlarmClient

    @Inject
    lateinit var cloudWatchIngestionService: CloudWatchIngestionService

    @Inject
    lateinit var incidentService: IncidentService

    @Inject
    lateinit var incidentRepository: IncidentRepository

    @Inject
    lateinit var diagnosisRepository: com.example.incidentanalyst.diagnosis.DiagnosisRepository

    @Inject
    lateinit var incidentEmbeddingRepository: com.example.incidentanalyst.rag.IncidentEmbeddingRepository

    @Inject
    lateinit var runbookEmbeddingRepository: com.example.incidentanalyst.rag.RunbookEmbeddingRepository

    @Inject
    lateinit var runbookFragmentRepository: com.example.incidentanalyst.runbook.RunbookFragmentRepository

    @BeforeEach
    @Transactional
    fun setup() {
        reset(cloudWatchAlarmClient)
        // Clear database in correct order to respect foreign keys
        incidentEmbeddingRepository.deleteAll()
        runbookEmbeddingRepository.deleteAll()
        diagnosisRepository.deleteAll()
        incidentRepository.deleteAll()
        runbookFragmentRepository.deleteAll()
        
        incidentRepository.flush()
    }

    @Test
    @Transactional
    fun `ingestAlarms successfully persists single alarm to database`() {
        val alarmTimestamp = Instant.parse("2024-01-15T10:30:00Z")
        val alarm = AlarmDto(
            alarmName = "HighCPUAlarm",
            alarmDescription = "CPU utilization exceeded 90%",
            stateValue = "ALARM",
            stateReason = "Threshold Crossed",
            stateUpdatedTimestamp = alarmTimestamp,
            metricName = "CPUUtilization",
            namespace = "AWS/EC2",
            threshold = "90",
            comparisonOperator = "GreaterThanOrEqualToThreshold"
        )

        whenever(cloudWatchAlarmClient.listAlarmsInAlarmState())
            .thenReturn(Either.Right(listOf(alarm)))

        val result = cloudWatchIngestionService.ingestAlarms()

        assertTrue(result is Either.Right)
        
        // Verify database persistence
        val allIncidents = incidentRepository.listAll()
        assertEquals(1, allIncidents.size)

        val savedIncident = allIncidents[0]
        assertEquals("HighCPUAlarm", savedIncident.title)
        assertEquals("OPEN", savedIncident.status)
        assertEquals(alarmTimestamp, savedIncident.createdAt)
    }

    @Test
    @Transactional
    fun `ingestAlarms successfully persists multiple alarms to database`() {
        val timestamp = Instant.parse("2024-01-15T10:30:00Z")
        val alarms = listOf(
            AlarmDto(
                alarmName = "CPUAlarm1",
                alarmDescription = "High CPU",
                stateValue = "ALARM",
                stateReason = "CPU high",
                stateUpdatedTimestamp = timestamp,
                metricName = "CPUUtilization",
                namespace = "AWS/EC2",
                threshold = "95",
                comparisonOperator = "GreaterThanThreshold"
            ),
            AlarmDto(
                alarmName = "MemoryAlarm1",
                alarmDescription = "High memory",
                stateValue = "ALARM",
                stateReason = "Memory high",
                stateUpdatedTimestamp = timestamp,
                metricName = "MemoryUtilization",
                namespace = "AWS/EC2",
                threshold = "85",
                comparisonOperator = "GreaterThanThreshold"
            )
        )

        whenever(cloudWatchAlarmClient.listAlarmsInAlarmState())
            .thenReturn(Either.Right(alarms))

        val result = cloudWatchIngestionService.ingestAlarms()

        assertTrue(result is Either.Right)
        
        val allIncidents = incidentRepository.listAll()
        assertEquals(2, allIncidents.size)
    }

    @Test
    @Transactional
    fun `ingestAlarms persists incidents that can be retrieved by incidentService`() {
        val timestamp = Instant.parse("2024-01-15T10:30:00Z")
        val alarm = AlarmDto(
            alarmName = "RetrievableAlarm",
            alarmDescription = "Should be retrievable",
            stateValue = "ALARM",
            stateReason = "Threshold crossed",
            stateUpdatedTimestamp = timestamp,
            metricName = "CPUUtilization",
            namespace = "AWS/EC2",
            threshold = "90",
            comparisonOperator = "GreaterThanThreshold"
        )

        whenever(cloudWatchAlarmClient.listAlarmsInAlarmState())
            .thenReturn(Either.Right(listOf(alarm)))

        cloudWatchIngestionService.ingestAlarms()

        val allIncidents = incidentRepository.listAll()
        val incidentId = IncidentId(requireNotNull(allIncidents[0].id))

        val retrieved = incidentService.getById(incidentId)
        assertTrue(retrieved is Either.Right)

        val incident = (retrieved as Either.Right).value
        assertEquals("RetrievableAlarm", incident.title)
    }

    @Test
    @Transactional
    fun `ingestAlarms handles multiple sequential ingestions correctly`() {
        val timestamp1 = Instant.parse("2024-01-15T10:30:00Z")
        val alarm1 = AlarmDto(
            alarmName = "FirstAlarm",
            alarmDescription = "First ingestion",
            stateValue = "ALARM",
            stateReason = "Threshold crossed",
            stateUpdatedTimestamp = timestamp1,
            metricName = "CPUUtilization",
            namespace = "AWS/EC2",
            threshold = "90",
            comparisonOperator = "GreaterThanThreshold"
        )

        whenever(cloudWatchAlarmClient.listAlarmsInAlarmState())
            .thenReturn(Either.Right(listOf(alarm1)))

        cloudWatchIngestionService.ingestAlarms()
        assertEquals(1, incidentRepository.count())

        val timestamp2 = Instant.parse("2024-01-15T11:30:00Z")
        val alarm2 = AlarmDto(
            alarmName = "SecondAlarm",
            alarmDescription = "Second ingestion",
            stateValue = "ALARM",
            stateReason = "Threshold crossed",
            stateUpdatedTimestamp = timestamp2,
            metricName = "MemoryUtilization",
            namespace = "AWS/EC2",
            threshold = "80",
            comparisonOperator = "GreaterThanThreshold"
        )

        whenever(cloudWatchAlarmClient.listAlarmsInAlarmState())
            .thenReturn(Either.Right(listOf(alarm2)))

        cloudWatchIngestionService.ingestAlarms()

        assertEquals(2, incidentRepository.count())
    }

    @Test
    @Transactional
    fun `ingestAlarms persists with exact timestamp from alarm`() {
        val alarmTimestamp = Instant.parse("2024-12-25T15:45:30Z")
        val alarm = AlarmDto(
            alarmName = "TimestampAlarm",
            alarmDescription = "Test timestamp precision",
            stateValue = "ALARM",
            stateReason = "Threshold crossed",
            stateUpdatedTimestamp = alarmTimestamp,
            metricName = "CPUUtilization",
            namespace = "AWS/EC2",
            threshold = "90",
            comparisonOperator = "GreaterThanThreshold"
        )

        whenever(cloudWatchAlarmClient.listAlarmsInAlarmState())
            .thenReturn(Either.Right(listOf(alarm)))

        cloudWatchIngestionService.ingestAlarms()

        val allIncidents = incidentRepository.listAll()
        assertEquals(1, allIncidents.size)
        assertEquals(alarmTimestamp, allIncidents[0].createdAt)
    }

    @Test
    @Transactional
    fun `pollIncidents method calls ingestAlarms and persists data`() {
        val timestamp = Instant.parse("2024-01-15T10:30:00Z")
        val alarm = AlarmDto(
            alarmName = "ScheduledAlarm",
            alarmDescription = "From scheduled job",
            stateValue = "ALARM",
            stateReason = "Threshold crossed",
            stateUpdatedTimestamp = timestamp,
            metricName = "CPUUtilization",
            namespace = "AWS/EC2",
            threshold = "90",
            comparisonOperator = "GreaterThanThreshold"
        )

        whenever(cloudWatchAlarmClient.listAlarmsInAlarmState())
            .thenReturn(Either.Right(listOf(alarm)))

        cloudWatchIngestionService.pollIncidents()

        assertEquals(1, incidentRepository.count())
        assertEquals("ScheduledAlarm", incidentRepository.listAll()[0].title)
    }
}
