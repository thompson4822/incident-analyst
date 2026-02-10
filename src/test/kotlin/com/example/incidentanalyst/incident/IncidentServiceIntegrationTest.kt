package com.example.incidentanalyst.incident

import com.example.incidentanalyst.common.Either
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant

@QuarkusTest
class IncidentServiceIntegrationTest {

    @Inject
    lateinit var incidentService: IncidentService

    @Inject
    lateinit var incidentRepository: IncidentRepository

    @Inject
    lateinit var diagnosisRepository: com.example.incidentanalyst.diagnosis.DiagnosisRepository

    @Inject
    lateinit var incidentEmbeddingRepository: com.example.incidentanalyst.rag.IncidentEmbeddingRepository

    @Inject
    lateinit var runbookEmbeddingRepository: com.example.incidentanalyst.rag.RunbookEmbeddingRepository

    @BeforeEach
    @Transactional
    fun setup() {
        // Clear database in correct order to respect foreign keys
        incidentEmbeddingRepository.deleteAll()
        runbookEmbeddingRepository.deleteAll()
        diagnosisRepository.deleteAll()
        incidentRepository.deleteAll()
    }

    @Test
    @Transactional
    fun `create persists incident and returns with assigned ID`() {
        val timestamp = Instant.now()
        val incident = Incident(
            id = IncidentId(0), // Placeholder ID
            source = "cloudwatch",
            title = "High CPU Usage",
            description = "CPU usage exceeded threshold",
            severity = Severity.HIGH,
            status = IncidentStatus.Open,
            createdAt = timestamp,
            updatedAt = timestamp
        )

        val created = incidentService.create(incident)

        assertTrue(created.id.value > 0, "ID should be assigned by database")
        assertEquals("cloudwatch", created.source)
        assertEquals("High CPU Usage", created.title)
        assertEquals("CPU usage exceeded threshold", created.description)
        assertEquals(Severity.HIGH, created.severity)
        assertTrue(created.status is IncidentStatus.Open)
        assertNotNull(created.createdAt)
        assertNotNull(created.updatedAt)
    }

    @Test
    @Transactional
    fun `create persists incident with Acknowledged status`() {
        val timestamp = Instant.now()
        val incident = Incident(
            id = IncidentId(0),
            source = "manual",
            title = "Database Connection Issue",
            description = "Connection pool exhausted",
            severity = Severity.MEDIUM,
            status = IncidentStatus.Acknowledged,
            createdAt = timestamp,
            updatedAt = timestamp
        )

        val created = incidentService.create(incident)

        assertTrue(created.id.value > 0)
        assertTrue(created.status is IncidentStatus.Acknowledged)
    }

    @Test
    @Transactional
    fun `create persists incident with Resolved status`() {
        val timestamp = Instant.now()
        val incident = Incident(
            id = IncidentId(0),
            source = "manual",
            title = "Disk Space Warning",
            description = "Disk usage at 85%",
            severity = Severity.LOW,
            status = IncidentStatus.Resolved,
            createdAt = timestamp,
            updatedAt = timestamp
        )

        val created = incidentService.create(incident)

        assertTrue(created.id.value > 0)
        assertTrue(created.status is IncidentStatus.Resolved)
    }

    @Test
    @Transactional
    fun `create persists incident with Diagnosed status`() {
        val timestamp = Instant.now()
        val diagnosisId = 123L
        val incident = Incident(
            id = IncidentId(0),
            source = "ai",
            title = "Memory Leak Detected",
            description = "Memory usage steadily increasing",
            severity = Severity.CRITICAL,
            status = IncidentStatus.Diagnosed(diagnosisId),
            createdAt = timestamp,
            updatedAt = timestamp
        )

        val created = incidentService.create(incident)

        assertTrue(created.id.value > 0)
        assertTrue(created.status is IncidentStatus.Diagnosed)
        val diagnosedStatus = created.status as IncidentStatus.Diagnosed
        assertEquals(diagnosisId, diagnosedStatus.diagnosisId)
    }

    @Test
    @Transactional
    fun `create persists incident with INFO severity`() {
        val timestamp = Instant.now()
        val incident = Incident(
            id = IncidentId(0),
            source = "monitoring",
            title = "Info-level incident",
            description = "Informational notification",
            severity = Severity.INFO,
            status = IncidentStatus.Open,
            createdAt = timestamp,
            updatedAt = timestamp
        )

        val created = incidentService.create(incident)

        assertTrue(created.id.value > 0)
        assertEquals(Severity.INFO, created.severity)
    }

    @Test
    @Transactional
    fun `create persists multiple incidents and assigns unique IDs`() {
        val timestamp = Instant.now()
        val incident1 = Incident(
            id = IncidentId(0),
            source = "cloudwatch",
            title = "Incident 1",
            description = "First incident",
            severity = Severity.HIGH,
            status = IncidentStatus.Open,
            createdAt = timestamp,
            updatedAt = timestamp
        )
        val incident2 = Incident(
            id = IncidentId(0),
            source = "cloudwatch",
            title = "Incident 2",
            description = "Second incident",
            severity = Severity.MEDIUM,
            status = IncidentStatus.Open,
            createdAt = timestamp,
            updatedAt = timestamp
        )

        val created1 = incidentService.create(incident1)
        val created2 = incidentService.create(incident2)

        assertTrue(created1.id.value > 0)
        assertTrue(created2.id.value > 0)
        assertTrue(created1.id.value != created2.id.value, "IDs should be unique")
    }

    @Test
    @Transactional
    fun `create persists incident and can be retrieved by ID`() {
        val timestamp = Instant.now()
        val incident = Incident(
            id = IncidentId(0),
            source = "cloudwatch",
            title = "Test Incident",
            description = "Test description",
            severity = Severity.HIGH,
            status = IncidentStatus.Open,
            createdAt = timestamp,
            updatedAt = timestamp
        )

        val created = incidentService.create(incident)
        val retrieved = incidentService.getById(created.id)

        assertTrue(retrieved is Either.Right)
        val retrievedIncident = (retrieved as Either.Right).value
        assertEquals(created.id.value, retrievedIncident.id.value)
        assertEquals("Test Incident", retrievedIncident.title)
        assertEquals("Test description", retrievedIncident.description)
    }
}
