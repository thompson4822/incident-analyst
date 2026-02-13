package com.example.incidentanalyst.remediation

import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@QuarkusTest
class SimulatedActionExecutorTest {

    @Inject
    lateinit var simulatedActionExecutor: SimulatedActionExecutor

    // ========== RestartService Tests ==========

    @Test
    fun `execute RestartService returns success message`() {
        val action = RemediationAction.RestartService("user-service")
        
        val result = simulatedActionExecutor.execute(action)
        
        assertEquals("Service user-service restarted successfully.", result)
    }

    @Test
    fun `execute RestartService with different service name`() {
        val action = RemediationAction.RestartService("api-gateway")
        
        val result = simulatedActionExecutor.execute(action)
        
        assertTrue(result.contains("api-gateway"))
        assertTrue(result.contains("restarted"))
    }

    // ========== ScaleCluster Tests ==========

    @Test
    fun `execute ScaleCluster returns success message`() {
        val action = RemediationAction.ScaleCluster("prod-cluster", 5)
        
        val result = simulatedActionExecutor.execute(action)
        
        assertEquals("Cluster prod-cluster scaled to 5 nodes.", result)
    }

    @Test
    fun `execute ScaleCluster with different values`() {
        val action = RemediationAction.ScaleCluster("staging", 10)
        
        val result = simulatedActionExecutor.execute(action)
        
        assertTrue(result.contains("staging"))
        assertTrue(result.contains("10"))
        assertTrue(result.contains("scaled"))
    }

    @Test
    fun `execute ScaleCluster with zero capacity`() {
        val action = RemediationAction.ScaleCluster("test-cluster", 0)
        
        val result = simulatedActionExecutor.execute(action)
        
        assertTrue(result.contains("0 nodes"))
    }

    // ========== ManualStep Tests ==========

    @Test
    fun `execute ManualStep returns recorded message`() {
        val action = RemediationAction.ManualStep("Check the database logs for errors")
        
        val result = simulatedActionExecutor.execute(action)
        
        assertEquals("Manual step recorded: Check the database logs for errors", result)
    }

    @Test
    fun `execute ManualStep preserves instructions`() {
        val instructions = "1. Check CPU usage\n2. Review memory metrics\n3. Restart if needed"
        val action = RemediationAction.ManualStep(instructions)
        
        val result = simulatedActionExecutor.execute(action)
        
        assertTrue(result.contains(instructions))
    }

    // ========== Interface Compliance ==========

    @Test
    fun `executor implements ActionExecutor interface`() {
        assertTrue(simulatedActionExecutor is ActionExecutor)
    }

    @Test
    fun `all action types return non-empty strings`() {
        val actions = listOf(
            RemediationAction.RestartService("svc"),
            RemediationAction.ScaleCluster("cluster", 1),
            RemediationAction.ManualStep("Do something")
        )
        
        actions.forEach { action ->
            val result = simulatedActionExecutor.execute(action)
            assertTrue(result.isNotEmpty(), "Result should not be empty for $action")
        }
    }
}
