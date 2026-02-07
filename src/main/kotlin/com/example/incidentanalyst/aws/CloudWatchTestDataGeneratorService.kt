package com.example.incidentanalyst.aws

import com.example.incidentanalyst.incident.IncidentService
import com.example.incidentanalyst.incident.Severity
import jakarta.enterprise.context.ApplicationScoped
import org.jboss.logging.Logger
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

@ApplicationScoped
class CloudWatchTestDataGeneratorService(
    private val incidentService: IncidentService
) {

    private val log = Logger.getLogger(javaClass)

    companion object {
        private val DEFAULT_NAMESPACES = listOf(
            "AWS/EC2",
            "AWS/RDS",
            "AWS/ECS",
            "AWS/ELB"
        )

        private val NAMESPACE_METRICS = mapOf(
            "AWS/EC2" to listOf("CPUUtilization", "DiskUsage", "MemoryUsage"),
            "AWS/RDS" to listOf("CPUUtilization", "DatabaseConnections", "FreeStorageSpace"),
            "AWS/ECS" to listOf("Errors", "Duration", "Throttles"),
            "AWS/ELB" to listOf("HTTPCode_5XX_Count", "Latency")
        )

        private val SEVERITY_THRESHOLDS = mapOf(
            Severity.CRITICAL to listOf(95, 99),
            Severity.HIGH to listOf(90, 94),
            Severity.MEDIUM to listOf(70, 89),
            Severity.LOW to listOf(50, 69),
            Severity.INFO to listOf(0, 49)
        )

        private val STATE_REASONS = listOf(
            "Threshold Crossed",
            "Metric exceeded threshold",
            "Alert condition met",
            "Violation detected",
            "Anomaly detected"
        )

        private val ALARM_DESCRIPTIONS = mapOf(
            "CPUUtilization" to "CPU utilization has exceeded the configured threshold",
            "DiskUsage" to "Disk usage has reached critical levels",
            "MemoryUsage" to "Memory utilization is above threshold",
            "DatabaseConnections" to "Database connections have exceeded limit",
            "FreeStorageSpace" to "Free storage space is running low",
            "Errors" to "Error rate has increased beyond threshold",
            "Duration" to "Operation duration exceeded threshold",
            "Throttles" to "Throttling events detected",
            "HTTPCode_5XX_Count" to "HTTP 5XX error count exceeded threshold",
            "Latency" to "Request latency exceeded threshold"
        )
    }

    fun generateAlarms(request: CloudWatchTestDataRequestDto): CloudWatchTestDataGenerationResult {
        // Validate request
        if (request.count != null && request.count < 0) {
            return CloudWatchTestDataGenerationResult.ValidationError(
                "Count must be non-negative"
            )
        }

        if (request.count != null && request.count > 1000) {
            return CloudWatchTestDataGenerationResult.ValidationError(
                "Count cannot exceed 1000"
            )
        }

        if (request.minSeverity != null && request.maxSeverity != null) {
            val severityOrder = listOf(
                Severity.INFO,
                Severity.LOW,
                Severity.MEDIUM,
                Severity.HIGH,
                Severity.CRITICAL
            )
            val minIndex = severityOrder.indexOf(request.minSeverity)
            val maxIndex = severityOrder.indexOf(request.maxSeverity)
            if (minIndex > maxIndex) {
                return CloudWatchTestDataGenerationResult.ValidationError(
                    "minSeverity cannot be greater than maxSeverity"
                )
            }
        }

        val count = request.count ?: 10
        if (count == 0) {
            return CloudWatchTestDataGenerationResult.Success(
                generatedCount = 0,
                severityBreakdown = emptyMap(),
                createdIncidentIds = emptyList(),
                seedUsed = request.seed
            )
        }

        // Set up random with seed if provided
        val random = if (request.seed != null) Random(request.seed) else Random
        val seedUsed = request.seed

        // Determine namespaces to use
        val namespaces = request.namespaces?.ifEmpty { DEFAULT_NAMESPACES }
            ?: DEFAULT_NAMESPACES

        // Determine severity range
        val severityOrder = listOf(
            Severity.INFO,
            Severity.LOW,
            Severity.MEDIUM,
            Severity.HIGH,
            Severity.CRITICAL
        )

        val minSeverity = request.minSeverity ?: Severity.INFO
        val maxSeverity = request.maxSeverity ?: Severity.CRITICAL
        val minIndex = severityOrder.indexOf(minSeverity)
        val maxIndex = severityOrder.indexOf(maxSeverity)
        val availableSeverities = severityOrder.subList(minIndex, maxIndex + 1)

        val filteredSeverities = request.severities?.filter { it in availableSeverities }
            ?: availableSeverities

        if (filteredSeverities.isEmpty()) {
            return CloudWatchTestDataGenerationResult.ValidationError(
                "No valid severities available given the constraints"
            )
        }

        // Determine time range
        val endTime = request.endTime ?: Instant.now()
        val startTime = request.startTime ?: endTime.minus(24, ChronoUnit.HOURS)

        if (startTime.isAfter(endTime)) {
            return CloudWatchTestDataGenerationResult.ValidationError(
                "startTime cannot be after endTime"
            )
        }

        // Generate alarms
        val usedAlarmNames = mutableSetOf<String>()
        val createdIncidentIds = mutableListOf<Long>()
        val severityCounts = mutableMapOf<Severity, Int>()

        repeat(count) { index ->
            val alarm = generateAlarm(
                random = random,
                namespaces = namespaces,
                severities = filteredSeverities,
                startTime = startTime,
                endTime = endTime,
                usedAlarmNames = usedAlarmNames,
                index = index
            )

            // Convert alarm to incident
            val incident = mapAlarmToIncident(alarm)

            if (incident != null) {
                val createdIncident = incidentService.create(incident)
                createdIncidentIds.add(createdIncident.id.value)
                severityCounts[createdIncident.severity] =
                    (severityCounts[createdIncident.severity] ?: 0) + 1
            }
        }

        val severityBreakdown = severityCounts.mapKeys { it.key.name }

        return CloudWatchTestDataGenerationResult.Success(
            generatedCount = createdIncidentIds.size,
            severityBreakdown = severityBreakdown,
            createdIncidentIds = createdIncidentIds,
            seedUsed = seedUsed
        )
    }

    fun mapAlarmToIncident(alarm: AlarmDto): com.example.incidentanalyst.incident.Incident? {
        val stateValue = alarm.stateValue?.trim()

        if (stateValue == null || !stateValue.equals("ALARM", ignoreCase = true)) {
            return null
        }

        val timestamp = alarm.stateUpdatedTimestamp ?: Instant.now()
        val severity = deriveSeverity(alarm)
        val alarmName = alarm.alarmName?.takeIf { it.isNotBlank() } ?: "Unknown Alarm"

        return com.example.incidentanalyst.incident.Incident(
            id = com.example.incidentanalyst.incident.IncidentId(0),
            source = "cloudwatch",
            title = alarmName,
            description = buildAlarmDescription(alarm),
            severity = severity,
            status = com.example.incidentanalyst.incident.IncidentStatus.Open,
            createdAt = timestamp,
            updatedAt = timestamp
        )
    }

    private fun buildAlarmDescription(alarm: AlarmDto): String {
        val name = alarm.alarmName ?: "UNKNOWN"
        val description = alarm.alarmDescription ?: "UNKNOWN"
        val reason = alarm.stateReason ?: "UNKNOWN"
        val namespace = alarm.namespace ?: "UNKNOWN"
        val metricName = alarm.metricName ?: "UNKNOWN"
        val threshold = alarm.threshold ?: "UNKNOWN"
        val operator = alarm.comparisonOperator ?: "UNKNOWN"
        val timestamp = alarm.stateUpdatedTimestamp?.toString() ?: "UNKNOWN"

        return """Alarm Name: $name
Alarm Description: $description
State Reason: $reason
Namespace: $namespace
Metric Name: $metricName
Threshold: $threshold
Comparison Operator: $operator
State Updated Timestamp: $timestamp""".trimIndent()
    }

    private fun deriveSeverity(alarm: AlarmDto): Severity {
        val threshold = alarm.threshold?.toDoubleOrNull() ?: 0.0
        val operator = alarm.comparisonOperator?.trim()

        val isGreaterThanOperator = operator.equals("GreaterThanThreshold", ignoreCase = true) ||
            operator.equals("GreaterThanOrEqualToThreshold", ignoreCase = true)

        return when {
            isGreaterThanOperator && threshold >= 95.0 -> Severity.CRITICAL
            isGreaterThanOperator && threshold >= 90.0 -> Severity.HIGH
            isGreaterThanOperator && threshold >= 70.0 -> Severity.MEDIUM
            isGreaterThanOperator && threshold >= 50.0 -> Severity.LOW
            else -> Severity.INFO
        }
    }

    private fun generateAlarm(
        random: Random,
        namespaces: List<String>,
        severities: List<Severity>,
        startTime: Instant,
        endTime: Instant,
        usedAlarmNames: MutableSet<String>,
        index: Int
    ): AlarmDto {
        // Select random namespace
        val namespace = namespaces[random.nextInt(namespaces.size)]

        // Select random metric for namespace
        val metrics = NAMESPACE_METRICS[namespace] ?: listOf("CustomMetric")
        val metricName = metrics[random.nextInt(metrics.size)]

        // Select random severity
        val severity = severities[random.nextInt(severities.size)]

        // Generate threshold based on severity
        val (minThreshold, maxThreshold) = SEVERITY_THRESHOLDS[severity] ?: listOf(0, 100)
        val threshold = random.nextInt(minThreshold, maxThreshold + 1)

        // Generate timestamp within range
        val startSeconds = startTime.epochSecond
        val endSeconds = endTime.epochSecond
        val timestampSeconds = startSeconds + random.nextLong(0, endSeconds - startSeconds + 1)
        val timestamp = Instant.ofEpochSecond(timestampSeconds)

        // Generate unique alarm name
        val alarmName = generateUniqueAlarmName(
            namespace = namespace,
            metricName = metricName,
            threshold = threshold,
            usedAlarmNames = usedAlarmNames
        )

        // Select random state reason
        val stateReason = STATE_REASONS[random.nextInt(STATE_REASONS.size)]

        // Get description for metric
        val alarmDescription = ALARM_DESCRIPTIONS[metricName]
            ?: "Alert for $metricName"

        return AlarmDto(
            alarmName = alarmName,
            alarmDescription = alarmDescription,
            stateValue = "ALARM",
            stateReason = stateReason,
            stateUpdatedTimestamp = timestamp,
            metricName = metricName,
            namespace = namespace,
            threshold = threshold.toString(),
            comparisonOperator = "GreaterThanThreshold"
        )
    }

    private fun generateUniqueAlarmName(
        namespace: String,
        metricName: String,
        threshold: Int,
        usedAlarmNames: MutableSet<String>
    ): String {
        val baseName = "$namespace-$metricName"
        var name = "$baseName-$threshold"

        var counter = 1
        while (name in usedAlarmNames) {
            name = "$baseName-$threshold-$counter"
            counter++
        }

        usedAlarmNames.add(name)
        return name
    }
}

sealed interface CloudWatchTestDataGenerationResult {
    data class Success(
        val generatedCount: Int,
        val severityBreakdown: Map<String, Int>,
        val createdIncidentIds: List<Long>,
        val seedUsed: Long?
    ) : CloudWatchTestDataGenerationResult

    data class ValidationError(
        val message: String
    ) : CloudWatchTestDataGenerationResult
}
