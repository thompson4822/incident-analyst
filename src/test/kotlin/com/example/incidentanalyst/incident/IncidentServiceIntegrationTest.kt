package com.example.incidentanalyst.incident

import com.example.incidentanalyst.common.Either
import com.example.incidentanalyst.incident.Incident
import com.example.incidentanalyst.incident.IncidentEntity
import com.example.incidentanalyst.incident.IncidentId
import com.example.incidentanalyst.incident.IncidentRepository
import com.example.incidentanalyst.incident.IncidentSource
import com.example.incidentanalyst.incident.IncidentStatus
import com.example.incidentanalyst.incident.Severity
import com.example.incidentanalyst.incident.toEntity
import dev.langchain4j.data.segment.TextSegment
import dev.langchain4j.store.embedding.EmbeddingStore
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
    lateinit var embeddingStore: EmbeddingStore<TextSegment>

    @Inject
    lateinit var runbookFragmentRepository: com.example.incidentanalyst.runbook.RunbookFragmentRepository

    @BeforeEach
    @Transactional
    fun setup() {
        // Clear database in correct order to respect foreign keys
        diagnosisRepository.deleteAll()
        incidentRepository.deleteAll()
        runbookFragmentRepository.deleteAll()
    }

    @Test
    @Transactional
    fun `create and retrieve incident`() {
        val timestamp = Instant.now()
        val incident = Incident(
            id = IncidentId(0),
            source = IncidentSource.Webhook("test"),
            title = "Test Incident",
            description = "Test Description",
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
    }

    @Test
    @Transactional
    fun `update incident status`() {
        val timestamp = Instant.now()
        val incident = Incident(
            id = IncidentId(0),
            source = IncidentSource.Webhook("test"),
            title = "Test Incident",
            description = "Test Description",
            severity = Severity.HIGH,
            status = IncidentStatus.Open,
            createdAt = timestamp,
            updatedAt = timestamp
        )

        val created = incidentService.create(incident)
        val updated = incidentService.updateStatus(created.id, IncidentStatus.Acknowledged)

        assertTrue(updated is Either.Right)
        val updatedIncident = (updated as Either.Right).value
        assertTrue(updatedIncident.status is IncidentStatus.Acknowledged)
    }
}
