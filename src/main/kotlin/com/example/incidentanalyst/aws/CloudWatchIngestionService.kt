package com.example.incidentanalyst.aws

import com.example.incidentanalyst.incident.Incident
import com.example.incidentanalyst.incident.IncidentService
import com.example.incidentanalyst.incident.IncidentStatus
import com.example.incidentanalyst.incident.Severity
import io.quarkus.scheduler.Scheduled
import jakarta.enterprise.context.ApplicationScoped
import org.jboss.logging.Logger
import java.time.Instant

@ApplicationScoped
class CloudWatchIngestionService(
    private val cloudWatchAlarmClient: CloudWatchAlarmClient,
    private val incidentService: IncidentService
) {

    private val log = Logger.getLogger(javaClass)

    @Scheduled(every = "60s")
    fun pollIncidents() {
        val result = ingestAlarms()

        when (result) {
            is CloudWatchIngestionResult.Success -> {
                log.infof(
                    "CloudWatch ingestion completed successfully. Incidents created: %d",
                    result.count
                )
            }
            is CloudWatchIngestionResult.Failure -> {
                // Result already logged, no action needed
            }
        }
    }

    fun ingestAlarms(): CloudWatchIngestionResult {
        val pollTimestamp = Instant.now()
        
        val queryResult = try {
            cloudWatchAlarmClient.listAlarmsInAlarmState()
        } catch (e: Exception) {
            log.errorf(
                e,
                "Failed to call CloudWatch client. errorType=Unknown, operation=ListAlarms, pollTimestamp=%s, error=%s",
                pollTimestamp,
                e.message ?: "Unexpected exception"
            )
            return CloudWatchIngestionResult.Failure(
                IngestionError.AwsError(AwsError.Unknown(e.message ?: "Unexpected exception"))
            )
        }
        
        return when (queryResult) {
            is AlarmQueryResult.Success -> {
                val alarms = queryResult.alarms
                val incidents = alarms.mapNotNull { mapAlarmToIncident(it) }
                
                log.infof(
                    "Successfully polled %d CloudWatch alarms in ALARM state, mapped to %d incidents",
                    alarms.size,
                    incidents.size
                )

                var persistedCount = 0
                incidents.forEach { incident ->
                    try {
                        incidentService.create(incident)
                        persistedCount++
                    } catch (e: Exception) {
                        log.errorf(
                            e,
                            "Failed to persist incident. errorType=PersistenceError, operation=ListAlarms, " +
                                "alarmCount=%d, incidentCount=%d, pollTimestamp=%s, title=%s",
                            alarms.size,
                            incidents.size,
                            pollTimestamp,
                            incident.title
                        )
                        return CloudWatchIngestionResult.Failure(
                            IngestionError.PersistenceError("Failed to persist incident: ${incident.title}")
                        )
                    }
                }

                CloudWatchIngestionResult.Success(persistedCount)
            }
            is AlarmQueryResult.Failure -> {
                val errorType = when (queryResult.error) {
                    is AwsError.Throttled -> "Throttled"
                    is AwsError.Unauthorized -> "Unauthorized"
                    is AwsError.NetworkError -> "NetworkError"
                    is AwsError.ServiceUnavailable -> "ServiceUnavailable"
                    is AwsError.Unknown -> "Unknown"
                }
                
                log.errorf(
                    "Failed to poll CloudWatch alarms. errorType=%s, operation=ListAlarms, alarmCount=0, incidentCount=0, pollTimestamp=%s, error=%s",
                    errorType,
                    pollTimestamp,
                    queryResult.error
                )
                
                CloudWatchIngestionResult.Failure(
                    IngestionError.AwsError(queryResult.error)
                )
            }
        }
    }

    fun mapAlarmToIncident(alarm: AlarmDto): Incident? {
        val stateValue = alarm.stateValue?.trim()

        if (stateValue == null || !stateValue.equals("ALARM", ignoreCase = true)) {
            return null
        }

        val timestamp = alarm.stateUpdatedTimestamp ?: Instant.now()
        val severity = deriveSeverity(alarm)
        val alarmName = alarm.alarmName?.takeIf { it.isNotBlank() } ?: "Unknown Alarm"

        return Incident(
            id = com.example.incidentanalyst.incident.IncidentId(0), // Placeholder ID, will be assigned on persist
            source = "cloudwatch",
            title = alarmName,
            description = buildAlarmDescription(alarm),
            severity = severity,
            status = IncidentStatus.Open,
            createdAt = timestamp,
            updatedAt = timestamp
        )
    }

    fun buildAlarmDescription(alarm: AlarmDto): String {
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

    fun deriveSeverity(alarm: AlarmDto): Severity {
        val threshold = alarm.threshold?.toDoubleOrNull() ?: 0.0
        val operator = alarm.comparisonOperator?.trim()

        val isGreaterThanOperator = operator.equals("GreaterThanThreshold", ignoreCase = true) ||
                               operator.equals("GreaterThanOrEqualToThreshold", ignoreCase = true)

        return when {
            isGreaterThanOperator && threshold >= 90.0 -> Severity.HIGH
            isGreaterThanOperator && threshold >= 70.0 -> Severity.MEDIUM
            isGreaterThanOperator && threshold >= 50.0 -> Severity.LOW
            else -> Severity.INFO
        }
    }
}
