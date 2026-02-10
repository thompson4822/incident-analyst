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

    @BeforeEach
    @Transactional
    fun setup() {
        reset(cloudWatchAlarmClient)
        // Clear database in correct order to respect foreign keys
        incidentEmbeddingRepository.deleteAll()
        runbookEmbeddingRepository.deleteAll()
        diagnosisRepository.deleteAll()
        incidentRepository.deleteAll()
    }

    @Test
    @Transactional
    fun `ingestAlarms successfully persists single alarm to database`() {
        val alarmTimestamp = Instant.parse("2024-01-15T10:30:00Z")
        val alarm = AlarmDto(
            alarmName = "HighCPUAlarm",
            alarmDescription = "CPU utilization exceeded 90%",
            stateValue = "ALARM",
            stateReason = "Threshold Crossed: 1 out of the last 1 datapoints [95.0 (15/01/24 10:30:00)] was greater than or equal to the threshold (90.0)",
            stateUpdatedTimestamp = alarmTimestamp,
            metricName = "CPUUtilization",
            namespace = "AWS/EC2",
            threshold = "90",
            comparisonOperator = "GreaterThanOrEqualToThreshold"
        )

        val queryResult = Either.Right(listOf(alarm))
        whenever(cloudWatchAlarmClient.listAlarmsInAlarmState())
            .thenReturn(queryResult)

        val result = cloudWatchIngestionService.ingestAlarms()

        assertTrue(result is Either.Right)
        val success = (result as Either.Right).value
        assertTrue(success is IngestionSuccess.NewIncidentsCreated)
        assertEquals(1, (success as IngestionSuccess.NewIncidentsCreated).count)

        // Verify database persistence
        val allIncidents = incidentRepository.listAll()
        assertEquals(1, allIncidents.size)

        val savedIncident = allIncidents[0]
        assertNotNull(savedIncident.id)
        assertTrue(savedIncident.id!! > 0)
        assertEquals("cloudwatch", savedIncident.source)
        assertEquals("HighCPUAlarm", savedIncident.title)
        assertEquals(Severity.HIGH.name, savedIncident.severity)
        assertEquals("OPEN", savedIncident.status)
        assertEquals(alarmTimestamp, savedIncident.createdAt)
        assertEquals(alarmTimestamp, savedIncident.updatedAt)

        // Verify description includes all alarm fields
        val description = savedIncident.description
        assertTrue(description.contains("Alarm Name: HighCPUAlarm"))
        assertTrue(description.contains("CPUUtilization"))
        assertTrue(description.contains("AWS/EC2"))
        assertTrue(description.contains("Threshold: 90"))
    }

    @Test
    @Transactional
    fun `ingestAlarms successfully persists multiple alarms to database`() {
        val timestamp = Instant.parse("2024-01-15T10:30:00Z")
        val alarms = listOf(
            AlarmDto(
                alarmName = "CPUAlarm1",
                alarmDescription = "High CPU on instance i-1",
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
                alarmDescription = "High memory on instance i-2",
                stateValue = "ALARM",
                stateReason = "Memory high",
                stateUpdatedTimestamp = timestamp,
                metricName = "MemoryUtilization",
                namespace = "AWS/EC2",
                threshold = "85",
                comparisonOperator = "GreaterThanThreshold"
            ),
            AlarmDto(
                alarmName = "DiskAlarm1",
                alarmDescription = "High disk usage",
                stateValue = "ALARM",
                stateReason = "Disk full",
                stateUpdatedTimestamp = timestamp,
                metricName = "DiskSpaceUtilization",
                namespace = "AWS/EC2",
                threshold = "75",
                comparisonOperator = "GreaterThanThreshold"
            )
        )

        val queryResult = Either.Right(alarms)
        whenever(cloudWatchAlarmClient.listAlarmsInAlarmState())
            .thenReturn(queryResult)

        val result = cloudWatchIngestionService.ingestAlarms()

        assertTrue(result is Either.Right)
        val success = (result as Either.Right).value
        assertTrue(success is IngestionSuccess.NewIncidentsCreated)
        assertEquals(3, (success as IngestionSuccess.NewIncidentsCreated).count)

        // Verify all incidents persisted with unique IDs
        val allIncidents = incidentRepository.listAll()
        assertEquals(3, allIncidents.size)

        val ids = allIncidents.map { it.id!! }.toSet()
        assertEquals(3, ids.size, "All incidents should have unique IDs")

        // Verify each alarm was correctly mapped
        val cpuIncident = allIncidents.find { it.title == "CPUAlarm1" }
        assertNotNull(cpuIncident)
        assertEquals(Severity.HIGH.name, cpuIncident!!.severity)

        val memoryIncident = allIncidents.find { it.title == "MemoryAlarm1" }
        assertNotNull(memoryIncident)
        assertEquals(Severity.MEDIUM.name, memoryIncident!!.severity)

        val diskIncident = allIncidents.find { it.title == "DiskAlarm1" }
        assertNotNull(diskIncident)
        assertEquals(Severity.MEDIUM.name, diskIncident!!.severity)
    }

    @Test
    @Transactional
    fun `ingestAlarms filters out non-ALARM state alarms`() {
        val timestamp = Instant.parse("2024-01-15T10:30:00Z")
        val alarms = listOf(
            AlarmDto(
                alarmName = "Alarm1",
                alarmDescription = "Alarm in ALARM state",
                stateValue = "ALARM",
                stateReason = "Threshold crossed",
                stateUpdatedTimestamp = timestamp,
                metricName = "CPUUtilization",
                namespace = "AWS/EC2",
                threshold = "90",
                comparisonOperator = "GreaterThanThreshold"
            ),
            AlarmDto(
                alarmName = "Alarm2",
                alarmDescription = "Alarm in OK state",
                stateValue = "OK",
                stateReason = "Threshold normal",
                stateUpdatedTimestamp = timestamp,
                metricName = "CPUUtilization",
                namespace = "AWS/EC2",
                threshold = "90",
                comparisonOperator = "GreaterThanThreshold"
            ),
            AlarmDto(
                alarmName = "Alarm3",
                alarmDescription = "Alarm in INSUFFICIENT_DATA state",
                stateValue = "INSUFFICIENT_DATA",
                stateReason = "No data",
                stateUpdatedTimestamp = timestamp,
                metricName = "CPUUtilization",
                namespace = "AWS/EC2",
                threshold = "90",
                comparisonOperator = "GreaterThanThreshold"
            )
        )

        val queryResult = Either.Right(alarms)
        whenever(cloudWatchAlarmClient.listAlarmsInAlarmState())
            .thenReturn(queryResult)

        val result = cloudWatchIngestionService.ingestAlarms()

        assertTrue(result is Either.Right)
        val success = (result as Either.Right).value
        assertTrue(success is IngestionSuccess.NewIncidentsCreated)
        assertEquals(1, (success as IngestionSuccess.NewIncidentsCreated).count, "Only ALARM state should create incidents")

        // Verify only ALARM state incident was persisted
        val allIncidents = incidentRepository.listAll()
        assertEquals(1, allIncidents.size)
        assertEquals("Alarm1", allIncidents[0].title)
    }

    @Test
    @Transactional
    fun `ingestAlarms handles empty alarm list`() {
        val queryResult = Either.Right(emptyList<AlarmDto>())
        whenever(cloudWatchAlarmClient.listAlarmsInAlarmState())
            .thenReturn(queryResult)

        val result = cloudWatchIngestionService.ingestAlarms()

        assertTrue(result is Either.Right)
        val success = (result as Either.Right).value
        assertTrue(success is IngestionSuccess.NoNewAlarms)

        // Verify no incidents were persisted
        val allIncidents = incidentRepository.listAll()
        assertEquals(0, allIncidents.size)
    }

    @Test
    @Transactional
    fun `ingestAlarms returns Failure for AWS Throttled error`() {
        val queryResult = Either.Left(AwsError.Throttled)
        whenever(cloudWatchAlarmClient.listAlarmsInAlarmState())
            .thenReturn(queryResult)

        val result = cloudWatchIngestionService.ingestAlarms()

        assertTrue(result is Either.Left)
        val failure = (result as Either.Left).value
        assertTrue(failure is IngestionError.AwsError)
        val awsError = (failure as IngestionError.AwsError).error
        assertTrue(awsError is AwsError.Throttled)

        // Verify no incidents were persisted
        val allIncidents = incidentRepository.listAll()
        assertEquals(0, allIncidents.size)
    }

    @Test
    @Transactional
    fun `ingestAlarms returns Failure for AWS Unauthorized error`() {
        val queryResult = Either.Left(AwsError.Unauthorized)
        whenever(cloudWatchAlarmClient.listAlarmsInAlarmState())
            .thenReturn(queryResult)

        val result = cloudWatchIngestionService.ingestAlarms()

        assertTrue(result is Either.Left)
        val failure = (result as Either.Left).value
        assertTrue(failure is IngestionError.AwsError)
        val awsError = (failure as IngestionError.AwsError).error
        assertTrue(awsError is AwsError.Unauthorized)

        val allIncidents = incidentRepository.listAll()
        assertEquals(0, allIncidents.size)
    }

    @Test
    @Transactional
    fun `ingestAlarms returns Failure for AWS NetworkError`() {
        val queryResult = Either.Left(AwsError.NetworkError)
        whenever(cloudWatchAlarmClient.listAlarmsInAlarmState())
            .thenReturn(queryResult)

        val result = cloudWatchIngestionService.ingestAlarms()

        assertTrue(result is Either.Left)
        val failure = (result as Either.Left).value
        assertTrue(failure is IngestionError.AwsError)
        val awsError = (failure as IngestionError.AwsError).error
        assertTrue(awsError is AwsError.NetworkError)

        val allIncidents = incidentRepository.listAll()
        assertEquals(0, allIncidents.size)
    }

    @Test
    @Transactional
    fun `ingestAlarms returns Failure for AWS ServiceUnavailable`() {
        val queryResult = Either.Left(AwsError.ServiceUnavailable)
        whenever(cloudWatchAlarmClient.listAlarmsInAlarmState())
            .thenReturn(queryResult)

        val result = cloudWatchIngestionService.ingestAlarms()

        assertTrue(result is Either.Left)
        val failure = (result as Either.Left).value
        assertTrue(failure is IngestionError.AwsError)
        val awsError = (failure as IngestionError.AwsError).error
        assertTrue(awsError is AwsError.ServiceUnavailable)

        val allIncidents = incidentRepository.listAll()
        assertEquals(0, allIncidents.size)
    }

    @Test
    @Transactional
    fun `ingestAlarms returns Failure for AWS Unknown error`() {
        val queryResult = Either.Left(AwsError.Unknown("Service error"))
        whenever(cloudWatchAlarmClient.listAlarmsInAlarmState())
            .thenReturn(queryResult)

        val result = cloudWatchIngestionService.ingestAlarms()

        assertTrue(result is Either.Left)
        val failure = (result as Either.Left).value
        assertTrue(failure is IngestionError.AwsError)
        val awsError = (failure as IngestionError.AwsError).error
        assertTrue(awsError is AwsError.Unknown)

        val allIncidents = incidentRepository.listAll()
        assertEquals(0, allIncidents.size)
    }

    @Test
    @Transactional
    fun `ingestAlarms handles case-insensitive ALARM state`() {
        val timestamp = Instant.parse("2024-01-15T10:30:00Z")
        val alarms = listOf(
            AlarmDto(
                alarmName = "Alarm1",
                alarmDescription = "Mixed case ALARM",
                stateValue = "alarm",
                stateReason = "Threshold crossed",
                stateUpdatedTimestamp = timestamp,
                metricName = "CPUUtilization",
                namespace = "AWS/EC2",
                threshold = "90",
                comparisonOperator = "GreaterThanThreshold"
            ),
            AlarmDto(
                alarmName = "Alarm2",
                alarmDescription = "Upper case ALARM",
                stateValue = "ALARM",
                stateReason = "Threshold crossed",
                stateUpdatedTimestamp = timestamp,
                metricName = "CPUUtilization",
                namespace = "AWS/EC2",
                threshold = "90",
                comparisonOperator = "GreaterThanThreshold"
            )
        )

        val queryResult = Either.Right(alarms)
        whenever(cloudWatchAlarmClient.listAlarmsInAlarmState())
            .thenReturn(queryResult)

        val result = cloudWatchIngestionService.ingestAlarms()

        assertTrue(result is Either.Right)
        val success = (result as Either.Right).value
        assertTrue(success is IngestionSuccess.NewIncidentsCreated)
        assertEquals(2, (success as IngestionSuccess.NewIncidentsCreated).count)

        val allIncidents = incidentRepository.listAll()
        assertEquals(2, allIncidents.size)
    }

    @Test
    @Transactional
    fun `ingestAlarms correctly derives severity from threshold values`() {
        val timestamp = Instant.parse("2024-01-15T10:30:00Z")
        val alarms = listOf(
            AlarmDto(
                alarmName = "HighSeverityAlarm",
                alarmDescription = "Very high threshold",
                stateValue = "ALARM",
                stateReason = "Threshold crossed",
                stateUpdatedTimestamp = timestamp,
                metricName = "CPUUtilization",
                namespace = "AWS/EC2",
                threshold = "95",
                comparisonOperator = "GreaterThanThreshold"
            ),
            AlarmDto(
                alarmName = "MediumSeverityAlarm",
                alarmDescription = "Medium threshold",
                stateValue = "ALARM",
                stateReason = "Threshold crossed",
                stateUpdatedTimestamp = timestamp,
                metricName = "MemoryUtilization",
                namespace = "AWS/EC2",
                threshold = "75",
                comparisonOperator = "GreaterThanThreshold"
            ),
            AlarmDto(
                alarmName = "LowSeverityAlarm",
                alarmDescription = "Low threshold",
                stateValue = "ALARM",
                stateReason = "Threshold crossed",
                stateUpdatedTimestamp = timestamp,
                metricName = "DiskUtilization",
                namespace = "AWS/EC2",
                threshold = "60",
                comparisonOperator = "GreaterThanThreshold"
            ),
            AlarmDto(
                alarmName = "InfoSeverityAlarm",
                alarmDescription = "Very low threshold",
                stateValue = "ALARM",
                stateReason = "Threshold crossed",
                stateUpdatedTimestamp = timestamp,
                metricName = "NetworkUtilization",
                namespace = "AWS/EC2",
                threshold = "45",
                comparisonOperator = "GreaterThanThreshold"
            )
        )

        val queryResult = Either.Right(alarms)
        whenever(cloudWatchAlarmClient.listAlarmsInAlarmState())
            .thenReturn(queryResult)

        val result = cloudWatchIngestionService.ingestAlarms()

        assertTrue(result is Either.Right)
        val success = (result as Either.Right).value
        assertTrue(success is IngestionSuccess.NewIncidentsCreated)
        assertEquals(4, (success as IngestionSuccess.NewIncidentsCreated).count)

        val allIncidents = incidentRepository.listAll()
        assertEquals(4, allIncidents.size)

        val highIncident = allIncidents.find { it.title == "HighSeverityAlarm" }
        assertNotNull(highIncident)
        assertEquals(Severity.HIGH.name, highIncident!!.severity)

        val mediumIncident = allIncidents.find { it.title == "MediumSeverityAlarm" }
        assertNotNull(mediumIncident)
        assertEquals(Severity.MEDIUM.name, mediumIncident!!.severity)

        val lowIncident = allIncidents.find { it.title == "LowSeverityAlarm" }
        assertNotNull(lowIncident)
        assertEquals(Severity.LOW.name, lowIncident!!.severity)

        val infoIncident = allIncidents.find { it.title == "InfoSeverityAlarm" }
        assertNotNull(infoIncident)
        assertEquals(Severity.INFO.name, infoIncident!!.severity)
    }

    @Test
    @Transactional
    fun `ingestAlarms assigns INFO severity for non-GreaterThanThreshold operators`() {
        val timestamp = Instant.parse("2024-01-15T10:30:00Z")
        val alarms = listOf(
            AlarmDto(
                alarmName = "LessThanAlarm",
                alarmDescription = "Less than threshold",
                stateValue = "ALARM",
                stateReason = "Threshold crossed",
                stateUpdatedTimestamp = timestamp,
                metricName = "CPUUtilization",
                namespace = "AWS/EC2",
                threshold = "95",
                comparisonOperator = "LessThanThreshold"
            ),
            AlarmDto(
                alarmName = "EqualToAlarm",
                alarmDescription = "Equal to threshold",
                stateValue = "ALARM",
                stateReason = "Threshold crossed",
                stateUpdatedTimestamp = timestamp,
                metricName = "MemoryUtilization",
                namespace = "AWS/EC2",
                threshold = "80",
                comparisonOperator = "EqualToThreshold"
            )
        )

        val queryResult = Either.Right(alarms)
        whenever(cloudWatchAlarmClient.listAlarmsInAlarmState())
            .thenReturn(queryResult)

        val result = cloudWatchIngestionService.ingestAlarms()

        assertTrue(result is Either.Right)
        val success = (result as Either.Right).value
        assertTrue(success is IngestionSuccess.NewIncidentsCreated)
        assertEquals(2, (success as IngestionSuccess.NewIncidentsCreated).count)

        val allIncidents = incidentRepository.listAll()
        assertEquals(2, allIncidents.size)

        assertTrue(allIncidents.all { it.severity == Severity.INFO.name })
    }

    @Test
    @Transactional
    fun `ingestAlarms uses current time when alarm timestamp is null`() {
        val beforeTest = Instant.now()
        val alarm = AlarmDto(
            alarmName = "NoTimestampAlarm",
            alarmDescription = "No timestamp",
            stateValue = "ALARM",
            stateReason = "Threshold crossed",
            stateUpdatedTimestamp = null,
            metricName = "CPUUtilization",
            namespace = "AWS/EC2",
            threshold = "90",
            comparisonOperator = "GreaterThanThreshold"
        )

        val queryResult = Either.Right(listOf(alarm))
        whenever(cloudWatchAlarmClient.listAlarmsInAlarmState())
            .thenReturn(queryResult)

        val result = cloudWatchIngestionService.ingestAlarms()

        assertTrue(result is Either.Right)
        val success = (result as Either.Right).value
        assertTrue(success is IngestionSuccess.NewIncidentsCreated)
        
        val afterTest = Instant.now()

        val allIncidents = incidentRepository.listAll()
        assertEquals(1, allIncidents.size)

        val incidentTimestamp = allIncidents[0].createdAt
        assertTrue(incidentTimestamp >= beforeTest)
        assertTrue(incidentTimestamp <= afterTest)
    }

    @Test
    @Transactional
    fun `ingestAlarms handles null alarm name with default value`() {
        val timestamp = Instant.parse("2024-01-15T10:30:00Z")
        val alarm = AlarmDto(
            alarmName = null,
            alarmDescription = "No name alarm",
            stateValue = "ALARM",
            stateReason = "Threshold crossed",
            stateUpdatedTimestamp = timestamp,
            metricName = "CPUUtilization",
            namespace = "AWS/EC2",
            threshold = "90",
            comparisonOperator = "GreaterThanThreshold"
        )

        val queryResult = Either.Right(listOf(alarm))
        whenever(cloudWatchAlarmClient.listAlarmsInAlarmState())
            .thenReturn(queryResult)

        val result = cloudWatchIngestionService.ingestAlarms()

        assertTrue(result is Either.Right)
        val success = (result as Either.Right).value
        assertTrue(success is IngestionSuccess.NewIncidentsCreated)

        val allIncidents = incidentRepository.listAll()
        assertEquals(1, allIncidents.size)
        assertEquals("Unknown Alarm", allIncidents[0].title)
    }

    @Test
    @Transactional
    fun `ingestAlarms handles decimal threshold values`() {
        val timestamp = Instant.parse("2024-01-15T10:30:00Z")
        val alarms = listOf(
            AlarmDto(
                alarmName = "DecimalHighAlarm",
                alarmDescription = "Decimal high",
                stateValue = "ALARM",
                stateReason = "Threshold crossed",
                stateUpdatedTimestamp = timestamp,
                metricName = "CPUUtilization",
                namespace = "AWS/EC2",
                threshold = "92.5",
                comparisonOperator = "GreaterThanThreshold"
            ),
            AlarmDto(
                alarmName = "DecimalLowAlarm",
                alarmDescription = "Decimal low",
                stateValue = "ALARM",
                stateReason = "Threshold crossed",
                stateUpdatedTimestamp = timestamp,
                metricName = "MemoryUtilization",
                namespace = "AWS/EC2",
                threshold = "52.3",
                comparisonOperator = "GreaterThanThreshold"
            )
        )

        val queryResult = Either.Right(alarms)
        whenever(cloudWatchAlarmClient.listAlarmsInAlarmState())
            .thenReturn(queryResult)

        val result = cloudWatchIngestionService.ingestAlarms()

        assertTrue(result is Either.Right)
        val success = (result as Either.Right).value
        assertTrue(success is IngestionSuccess.NewIncidentsCreated)
        assertEquals(2, (success as IngestionSuccess.NewIncidentsCreated).count)

        val allIncidents = incidentRepository.listAll()
        assertEquals(2, allIncidents.size)

        val highIncident = allIncidents.find { it.title == "DecimalHighAlarm" }
        assertNotNull(highIncident)
        assertEquals(Severity.HIGH.name, highIncident!!.severity)

        val lowIncident = allIncidents.find { it.title == "DecimalLowAlarm" }
        assertNotNull(lowIncident)
        assertEquals(Severity.LOW.name, lowIncident!!.severity)
    }

    @Test
    @Transactional
    fun `ingestAlarms builds complete alarm description`() {
        val timestamp = Instant.parse("2024-01-15T10:30:00Z")
        val alarm = AlarmDto(
            alarmName = "CompleteAlarm",
            alarmDescription = "Complete description",
            stateValue = "ALARM",
            stateReason = "State reason details",
            stateUpdatedTimestamp = timestamp,
            metricName = "CPUCreditBalance",
            namespace = "AWS/EC2",
            threshold = "10",
            comparisonOperator = "LessThanThreshold"
        )

        val queryResult = Either.Right(listOf(alarm))
        whenever(cloudWatchAlarmClient.listAlarmsInAlarmState())
            .thenReturn(queryResult)

        val result = cloudWatchIngestionService.ingestAlarms()

        assertTrue(result is Either.Right)
        val success = (result as Either.Right).value
        assertTrue(success is IngestionSuccess.NewIncidentsCreated)

        val allIncidents = incidentRepository.listAll()
        assertEquals(1, allIncidents.size)

        val description = allIncidents[0].description
        assertTrue(description.contains("Alarm Name: CompleteAlarm"))
        assertTrue(description.contains("Alarm Description: Complete description"))
        assertTrue(description.contains("State Reason: State reason details"))
        assertTrue(description.contains("Namespace: AWS/EC2"))
        assertTrue(description.contains("Metric Name: CPUCreditBalance"))
        assertTrue(description.contains("Threshold: 10"))
        assertTrue(description.contains("Comparison Operator: LessThanThreshold"))
        assertTrue(description.contains("State Updated Timestamp: 2024-01-15T10:30:00Z"))
    }

    @Test
    @Transactional
    fun `ingestAlarms handles null fields in alarm with UNKNOWN placeholders`() {
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

        val queryResult = Either.Right(listOf(alarm))
        whenever(cloudWatchAlarmClient.listAlarmsInAlarmState())
            .thenReturn(queryResult)

        val result = cloudWatchIngestionService.ingestAlarms()

        assertTrue(result is Either.Right)
        val success = (result as Either.Right).value
        assertTrue(success is IngestionSuccess.NewIncidentsCreated)

        val allIncidents = incidentRepository.listAll()
        assertEquals(1, allIncidents.size)

        assertEquals("Unknown Alarm", allIncidents[0].title)

        val description = allIncidents[0].description
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

        val queryResult = Either.Right(listOf(alarm))
        whenever(cloudWatchAlarmClient.listAlarmsInAlarmState())
            .thenReturn(queryResult)

        val result = cloudWatchIngestionService.ingestAlarms()

        assertTrue(result is Either.Right)
        val success = (result as Either.Right).value
        assertTrue(success is IngestionSuccess.NewIncidentsCreated)
        assertEquals(1, (success as IngestionSuccess.NewIncidentsCreated).count)

        // Get all incidents to find the ID
        val allIncidents = incidentRepository.listAll()
        assertEquals(1, allIncidents.size)

        val incidentId = IncidentId(requireNotNull(allIncidents[0].id))

        // Verify incident can be retrieved via incidentService
        val retrieved = incidentService.getById(incidentId)
        assertTrue(retrieved is Either.Right)

        val incident = (retrieved as Either.Right).value
        assertEquals("RetrievableAlarm", incident.title)
        assertEquals("cloudwatch", incident.source)
        assertEquals(Severity.HIGH, incident.severity)
        assertEquals(timestamp, incident.createdAt)
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

        val queryResult1 = Either.Right(listOf(alarm1))
        whenever(cloudWatchAlarmClient.listAlarmsInAlarmState())
            .thenReturn(queryResult1)

        // First ingestion
        val result1 = cloudWatchIngestionService.ingestAlarms()
        assertTrue(result1 is Either.Right)
        val success1 = (result1 as Either.Right).value
        assertTrue(success1 is IngestionSuccess.NewIncidentsCreated)
        assertEquals(1, (success1 as IngestionSuccess.NewIncidentsCreated).count)
        assertEquals(1, incidentRepository.count())

        // Second ingestion with different alarm
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

        val queryResult2 = Either.Right(listOf(alarm2))
        whenever(cloudWatchAlarmClient.listAlarmsInAlarmState())
            .thenReturn(queryResult2)

        // Second ingestion
        val result2 = cloudWatchIngestionService.ingestAlarms()
        assertTrue(result2 is Either.Right)
        val success2 = (result2 as Either.Right).value
        assertTrue(success2 is IngestionSuccess.NewIncidentsCreated)
        assertEquals(1, (success2 as IngestionSuccess.NewIncidentsCreated).count)

        // Verify both incidents exist
        val allIncidents = incidentRepository.listAll()
        assertEquals(2, allIncidents.size)

        val alarmTitles = allIncidents.map { it.title }.toSet()
        assertTrue(alarmTitles.contains("FirstAlarm"))
        assertTrue(alarmTitles.contains("SecondAlarm"))
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

        val queryResult = Either.Right(listOf(alarm))
        whenever(cloudWatchAlarmClient.listAlarmsInAlarmState())
            .thenReturn(queryResult)

        val result = cloudWatchIngestionService.ingestAlarms()

        assertTrue(result is Either.Right)
        val success = (result as Either.Right).value
        assertTrue(success is IngestionSuccess.NewIncidentsCreated)

        val allIncidents = incidentRepository.listAll()
        assertEquals(1, allIncidents.size)

        // Verify exact timestamp preservation
        assertEquals(alarmTimestamp, allIncidents[0].createdAt)
        assertEquals(alarmTimestamp, allIncidents[0].updatedAt)
    }

    @Test
    @Transactional
    fun `pollIncidents method calls ingestAlarms and returns result`() {
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

        val queryResult = Either.Right(listOf(alarm))
        whenever(cloudWatchAlarmClient.listAlarmsInAlarmState())
            .thenReturn(queryResult)

        // Call pollIncidents (the scheduled method)
        cloudWatchIngestionService.pollIncidents()

        // Verify alarm was ingested and persisted
        val allIncidents = incidentRepository.listAll()
        assertEquals(1, allIncidents.size)
        assertEquals("ScheduledAlarm", allIncidents[0].title)
    }
}
