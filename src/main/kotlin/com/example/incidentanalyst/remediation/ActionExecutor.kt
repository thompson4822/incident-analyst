package com.example.incidentanalyst.remediation

import jakarta.enterprise.context.ApplicationScoped
import org.jboss.logging.Logger

interface ActionExecutor {
    fun execute(action: RemediationAction): String
}

@ApplicationScoped
class SimulatedActionExecutor : ActionExecutor {
    private val log = Logger.getLogger(javaClass)

    override fun execute(action: RemediationAction): String {
        log.infof("Simulating execution of action: %s", action)
        
        return when (action) {
            is RemediationAction.RestartService -> {
                "Service ${action.serviceName} restarted successfully."
            }
            is RemediationAction.ScaleCluster -> {
                "Cluster ${action.clusterId} scaled to ${action.desiredCapacity} nodes."
            }
            is RemediationAction.ManualStep -> {
                "Manual step recorded: ${action.instructions}"
            }
        }
    }
}
