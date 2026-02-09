package com.example.incidentanalyst.aws

import com.example.incidentanalyst.common.Either

interface CloudWatchAlarmClient {
    fun listAlarmsInAlarmState(): Either<AwsError, List<AlarmDto>>
}
