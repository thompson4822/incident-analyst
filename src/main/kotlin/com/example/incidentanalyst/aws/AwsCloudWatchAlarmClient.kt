package com.example.incidentanalyst.aws

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import software.amazon.awssdk.core.exception.SdkClientException
import software.amazon.awssdk.core.exception.SdkException
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient
import software.amazon.awssdk.services.cloudwatch.model.CloudWatchException
import software.amazon.awssdk.services.cloudwatch.model.DescribeAlarmsRequest
import software.amazon.awssdk.services.cloudwatch.model.MetricAlarm
import software.amazon.awssdk.services.cloudwatch.model.StateValue

@ApplicationScoped
class AwsCloudWatchAlarmClient @Inject constructor(
    private val cloudWatchClient: CloudWatchClient
) : CloudWatchAlarmClient {

    override fun listAlarmsInAlarmState(): AlarmQueryResult {
        return try {
            val alarms = mutableListOf<AlarmDto>()
            var nextToken: String? = null

            do {
                val request = DescribeAlarmsRequest.builder()
                    .stateValue(StateValue.ALARM)
                    .nextToken(nextToken)
                    .build()

                val response = cloudWatchClient.describeAlarms(request)
                alarms.addAll(response.metricAlarms().map { alarm ->
                    AlarmDto(
                        alarmName = alarm.alarmName(),
                        alarmDescription = alarm.alarmDescription(),
                        stateValue = alarm.stateValueAsString(),
                        stateReason = alarm.stateReason(),
                        stateUpdatedTimestamp = alarm.stateUpdatedTimestamp(),
                        metricName = alarm.metricName(),
                        namespace = alarm.namespace(),
                        threshold = alarm.threshold()?.toString(),
                        comparisonOperator = alarm.comparisonOperatorAsString()
                    )
                })

                nextToken = response.nextToken()
            } while (nextToken != null)

            AlarmQueryResult.Success(alarms)
        } catch (e: Exception) {
            AlarmQueryResult.Failure(mapAwsExceptionToError(e))
        }
    }

    private fun mapAwsExceptionToError(e: Exception): AwsError {
        return when (e) {
            is CloudWatchException -> {
                when (e.statusCode()) {
                    429 -> AwsError.Throttled
                    401, 403 -> AwsError.Unauthorized
                    else -> AwsError.ServiceUnavailable
                }
            }
            is SdkClientException -> AwsError.NetworkError
            is SdkException -> AwsError.ServiceUnavailable
            else -> AwsError.Unknown(e.message ?: "Unknown error")
        }
    }
}