package com.example.incidentanalyst.rag

import com.example.incidentanalyst.common.Either
import com.example.incidentanalyst.incident.Incident
import com.example.incidentanalyst.incident.IncidentEntity
import com.example.incidentanalyst.incident.IncidentId
import com.example.incidentanalyst.incident.IncidentRepository
import com.example.incidentanalyst.incident.IncidentStatus
import com.example.incidentanalyst.incident.Severity
import com.example.incidentanalyst.incident.toEntity
import com.example.incidentanalyst.runbook.RunbookFragment
import com.example.incidentanalyst.runbook.RunbookFragmentEntity
import com.example.incidentanalyst.runbook.RunbookFragmentId
import com.example.incidentanalyst.runbook.RunbookFragmentRepository
import dev.langchain4j.data.segment.TextSegment
import dev.langchain4j.model.embedding.EmbeddingModel
import dev.langchain4j.model.output.Response
import dev.langchain4j.store.embedding.EmbeddingStore
import dev.langchain4j.store.embedding.EmbeddingSearchRequest
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
class EmbeddingServiceIntegrationTest {

    @Inject
    lateinit var embeddingService: EmbeddingService

    @Inject
    lateinit var embeddingStore: EmbeddingStore<TextSegment>

    @Inject
    lateinit var incidentRepository: IncidentRepository

    @Inject
    lateinit var runbookFragmentRepository: RunbookFragmentRepository

    @Inject
    lateinit var diagnosisRepository: com.example.incidentanalyst.diagnosis.DiagnosisRepository

    @BeforeEach
    @Transactional
    fun setup() {
        // Clear database
        diagnosisRepository.deleteAll()
        runbookFragmentRepository.deleteAll()
        incidentRepository.deleteAll()
        
        // Clear embedding store if possible (InMemoryEmbeddingStore can be cleared by re-instantiating or if it's a bean, we might need a clear method)
        // For InMemoryEmbeddingStore, we can't easily clear it if it's injected as a singleton.
        // But since it's an integration test, we can just check for presence.
    }

    @Test
    @Transactional
    fun `end-to-end embedding creation for incident`() {
        // Arrange
        val timestamp = Instant.now()
        val incident = Incident(
            id = IncidentId(0),
            source = "cloudwatch",
            title = "Database connection failed " + System.currentTimeMillis(),
            description = "Connection pool exhausted due to high load",
            severity = Severity.HIGH,
            status = IncidentStatus.Open,
            createdAt = timestamp,
            updatedAt = timestamp
        )
        val entity = incident.toEntity()
        incidentRepository.persist(entity)
        val incidentId = IncidentId(requireNotNull(entity.id))

        // Act
        val result = embeddingService.embedIncident(incidentId)

        // Assert
        assertTrue(result is Either.Right)
        assertEquals(1, (result as Either.Right).value)
    }

    @Test
    @Transactional
    fun `end-to-end embedding creation for runbook fragment`() {
        // Arrange
        val timestamp = Instant.now()
        val fragment = RunbookFragmentEntity(
            id = null,
            title = "Database Connection Troubleshooting " + System.currentTimeMillis(),
            content = "Check connection pool settings and increase pool size if needed",
            tags = "database,troubleshooting,connection",
            createdAt = timestamp
        )
        runbookFragmentRepository.persist(fragment)
        val fragmentId = RunbookFragmentId(requireNotNull(fragment.id))

        // Act
        val result = embeddingService.embedRunbook(fragmentId)

        // Assert
        assertTrue(result is Either.Right)
        assertEquals(1, (result as Either.Right).value)
    }

    @Test
    @Transactional
    fun `embedBatch creates embeddings for both incidents and runbooks`() {
        // Arrange
        val timestamp = Instant.now()
        val incident = Incident(
            id = IncidentId(0),
            source = "cloudwatch",
            title = "CPU high " + System.currentTimeMillis(),
            description = "CPU exceeded threshold",
            severity = Severity.HIGH,
            status = IncidentStatus.Open,
            createdAt = timestamp,
            updatedAt = timestamp
        )
        val fragment = RunbookFragmentEntity(
            id = null,
            title = "CPU Optimization " + System.currentTimeMillis(),
            content = "Reduce CPU usage by optimizing queries",
            tags = "cpu,optimization",
            createdAt = timestamp
        )

        val incidentEntity = incident.toEntity()
        incidentRepository.persist(incidentEntity)
        runbookFragmentRepository.persist(fragment)
        val incidentId = IncidentId(requireNotNull(incidentEntity.id))
        val fragmentId = RunbookFragmentId(requireNotNull(fragment.id))

        // Act
        val result = embeddingService.embedBatch(
            incidentIds = listOf(incidentId),
            fragmentIds = listOf(fragmentId)
        )

        // Assert
        assertTrue(result is Either.Right)
        assertEquals(2, (result as Either.Right).value)
    }

    @Test
    @Transactional
    fun `embedIncident fails for non-existent incident`() {
        // Act
        val result = embeddingService.embedIncident(IncidentId(999999L))

        // Assert
        assertTrue(result is Either.Left)
        assertTrue((result as Either.Left).value is EmbeddingError.Unexpected)
    }

    @Test
    @Transactional
    fun `embedRunbook fails for non-existent fragment`() {
        // Act
        val result = embeddingService.embedRunbook(RunbookFragmentId(999999L))

        // Assert
        assertTrue(result is Either.Left)
        assertTrue((result as Either.Left).value is EmbeddingError.Unexpected)
    }
}
