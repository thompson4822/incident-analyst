package com.example.incidentanalyst.incident

import com.example.incidentanalyst.aws.CloudWatchTestDataGeneratorService
import com.example.incidentanalyst.aws.CloudWatchTestDataRequestDto
import io.quarkus.runtime.StartupEvent
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.event.Observes
import jakarta.inject.Inject
import org.jboss.logging.Logger

/**
 * Generates dynamic test data on startup if the database is empty.
 * Static seed data is handled by Flyway in src/main/resources/db/dev-data.
 */
@ApplicationScoped
class IncidentDataInitializer {
    private val log = Logger.getLogger(javaClass)

    @Inject
    lateinit var incidentService: IncidentService

    @Inject
    lateinit var testDataGenerator: CloudWatchTestDataGeneratorService

    fun onStart(@Observes ev: StartupEvent) {
        log.info("Checking for existing incidents...")
        // We check if we have more than the static seed data (which is 3 incidents)
        val existing = incidentService.listRecent(limit = 10)
        if (existing.size <= 3) {
            log.info("Generating dynamic sample data to ensure recent timestamps...")
            
            val request = CloudWatchTestDataRequestDto(
                count = 10,
                seed = 42L // Deterministic for dev
            )
            
            testDataGenerator.generateAlarms(request)
            log.info("Dynamic sample incidents generated.")
        } else {
            log.info("Incidents already exist, skipping dynamic initialization.")
        }
    }
}
