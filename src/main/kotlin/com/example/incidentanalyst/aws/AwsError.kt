package com.example.incidentanalyst.aws

sealed interface AwsError {
    data object ServiceUnavailable : AwsError
    data object Throttled : AwsError
    data object Unauthorized : AwsError
    data object NetworkError : AwsError
    data class Unknown(val message: String) : AwsError
}