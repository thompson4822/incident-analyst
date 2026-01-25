package com.example.incidentanalyst.aws

import java.time.Instant

data class AlarmDto(
    val alarmName: String?,
    val alarmDescription: String?,
    val stateValue: String?,
    val stateReason: String?,
    val stateUpdatedTimestamp: Instant?,
    val metricName: String?,
    val namespace: String?,
    val threshold: String?,
    val comparisonOperator: String?
)