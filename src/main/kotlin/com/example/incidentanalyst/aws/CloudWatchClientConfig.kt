package com.example.incidentanalyst.aws

import jakarta.enterprise.context.ApplicationScoped
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClient

@ApplicationScoped
class CloudWatchClientConfig {

    fun cloudWatchClient(): CloudWatchClient =
        CloudWatchClient.builder()
            .region(Region.US_EAST_1)
            .build()

    fun cloudWatchLogsClient(): CloudWatchLogsClient =
        CloudWatchLogsClient.builder()
            .region(Region.US_EAST_1)
            .build()
}
