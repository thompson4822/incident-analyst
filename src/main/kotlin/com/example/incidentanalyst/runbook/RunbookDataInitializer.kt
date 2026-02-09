package com.example.incidentanalyst.runbook

import io.quarkus.runtime.StartupEvent
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.event.Observes
import jakarta.inject.Inject
import org.jboss.logging.Logger

@ApplicationScoped
class RunbookDataInitializer {
    private val log = Logger.getLogger(javaClass)

    @Inject
    lateinit var runbookService: RunbookService

    fun onStart(@Observes ev: StartupEvent) {
        log.info("Initializing sample runbooks...")
        val existing = runbookService.listRecent()
        if (existing.isEmpty()) {
            runbookService.createFragment(
                "High CPU Troubleshooting",
                "1. Check top processes\n2. Identify high CPU process\n3. Check logs for errors\n4. Restart service if needed",
                "cpu,troubleshooting"
            )
            runbookService.createFragment(
                "Database Connection Pool Exhaustion",
                "1. Check active connections\n2. Identify long running queries\n3. Kill idle transactions\n4. Scale up if necessary",
                "database,postgres"
            )
            runbookService.createFragment(
                "Network Latency Investigation",
                "1. Ping gateway\n2. Trace route to target\n3. Check VPC flow logs\n4. Verify security group rules",
                "network,latency"
            )
            log.info("Sample runbooks initialized.")
        } else {
            log.info("Runbooks already exist, skipping initialization.")
        }
    }
}
