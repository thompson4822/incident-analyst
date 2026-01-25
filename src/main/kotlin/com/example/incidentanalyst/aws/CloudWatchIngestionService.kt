package com.example.incidentanalyst.aws

import com.example.incidentanalyst.incident.IncidentEntity
import com.example.incidentanalyst.incident.IncidentRepository
import io.quarkus.scheduler.Scheduled
import jakarta.enterprise.context.ApplicationScoped
import java.time.Instant

@ApplicationScoped
class CloudWatchIngestionService(
    private val clientConfig: CloudWatchClientConfig,
    private val incidentRepository: IncidentRepository
) {

    @Scheduled(every = "60s")
    fun pollIncidents() {
        val dummy = IncidentEntity(
            source = "CloudWatch",
            title = "Dummy incident",
            description = "Placeholder",
            severity = "INFO",
            status = "OPEN",
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
    }
}
