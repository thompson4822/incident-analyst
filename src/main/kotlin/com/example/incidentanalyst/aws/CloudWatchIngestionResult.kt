package com.example.incidentanalyst.aws

sealed interface CloudWatchIngestionResult {
    data class Success(val count: Int) : CloudWatchIngestionResult
    data class Failure(val error: IngestionError) : CloudWatchIngestionResult
}

sealed interface IngestionError {
    data class AwsError(val error: com.example.incidentanalyst.aws.AwsError) : IngestionError
    data class PersistenceError(val message: String) : IngestionError
}