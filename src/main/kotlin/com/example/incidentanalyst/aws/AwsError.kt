package com.example.incidentanalyst.aws

sealed interface AwsError {
    data object ServiceUnavailable : AwsError
    data object Throttled : AwsError
    data object Unauthorized : AwsError
    data object NetworkError : AwsError
    data class Unknown(val message: String) : AwsError
}

sealed interface IngestionError {
    data class AwsError(val error: com.example.incidentanalyst.aws.AwsError) : IngestionError
    data class PersistenceError(val message: String) : IngestionError
}

sealed interface IngestionSuccess {
    data class NewIncidentsCreated(val count: Int) : IngestionSuccess
    data object NoNewAlarms : IngestionSuccess
}
