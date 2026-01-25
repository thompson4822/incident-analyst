package com.example.incidentanalyst.aws

interface CloudWatchAlarmClient {
    fun listAlarmsInAlarmState(): AlarmQueryResult
}