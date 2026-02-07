package com.example.incidentanalyst.aws

import io.quarkus.arc.profile.IfBuildProfile
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.inject.Alternative
import jakarta.annotation.Priority
import java.time.Instant

@Alternative
@IfBuildProfile("test")
@Priority(1)
@ApplicationScoped
class MockCloudWatchAlarmClient : CloudWatchAlarmClient {

    override fun listAlarmsInAlarmState(): AlarmQueryResult {
        val timestamp = Instant.now()

        val alarms = listOf(
            AlarmDto(
                alarmName = "HighCPUAlarm",
                alarmDescription = "CPU utilization exceeded 90% threshold",
                stateValue = "ALARM",
                stateReason = "Crossing alarm threshold",
                stateUpdatedTimestamp = timestamp,
                metricName = "CPUUtilization",
                namespace = "AWS/EC2",
                threshold = "90",
                comparisonOperator = "GreaterThanThreshold"
            ),
            AlarmDto(
                alarmName = "MediumDiskAlarm",
                alarmDescription = "Disk usage exceeded 70% threshold",
                stateValue = "ALARM",
                stateReason = "Crossing alarm threshold",
                stateUpdatedTimestamp = timestamp,
                metricName = "DiskUsage",
                namespace = "AWS/EC2",
                threshold = "70",
                comparisonOperator = "GreaterThanThreshold"
            ),
            AlarmDto(
                alarmName = "LowMemoryAlarm",
                alarmDescription = "Memory usage exceeded 50% threshold",
                stateValue = "ALARM",
                stateReason = "Crossing alarm threshold",
                stateUpdatedTimestamp = timestamp,
                metricName = "MemoryUsage",
                namespace = "AWS/EC2",
                threshold = "50",
                comparisonOperator = "GreaterThanThreshold"
            )
        )

        return AlarmQueryResult.Success(alarms)
    }
}
