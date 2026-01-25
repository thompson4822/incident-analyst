package com.example.incidentanalyst.aws

sealed interface AlarmQueryResult {
    data class Success(val alarms: List<AlarmDto>) : AlarmQueryResult
    data class Failure(val error: AwsError) : AlarmQueryResult
}