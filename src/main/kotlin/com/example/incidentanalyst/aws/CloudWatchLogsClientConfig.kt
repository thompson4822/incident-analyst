package com.example.incidentanalyst.aws

import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.inject.Produces
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClient

@ApplicationScoped
class CloudWatchLogsClientConfig {

    @Produces
    @ApplicationScoped
    fun cloudWatchLogsClient(): CloudWatchLogsClient =
        CloudWatchLogsClient.builder()
            .region(Region.US_EAST_1)
            .build()
}