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
import dev.langchain4j.data.embedding.Embedding
import dev.langchain4j.data.segment.TextSegment
import dev.langchain4j.model.embedding.EmbeddingModel
import dev.langchain4j.model.output.Response
import io.quarkus.test.InjectMock
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import java.time.Instant

@QuarkusTest
class EmbeddingServiceIntegrationTest {

    @Inject
    lateinit var embeddingService: EmbeddingService

    @InjectMock
    lateinit var embeddingModel: EmbeddingModel

    @Inject
    lateinit var incidentRepository: IncidentRepository

    @Inject
    lateinit var incidentEmbeddingRepository: IncidentEmbeddingRepository

    @Inject
    lateinit var runbookFragmentRepository: RunbookFragmentRepository

    @Inject
    lateinit var runbookEmbeddingRepository: RunbookEmbeddingRepository

    @Inject
    lateinit var diagnosisRepository: com.example.incidentanalyst.diagnosis.DiagnosisRepository

    @BeforeEach
    @Transactional
    fun setup() {
        reset(embeddingModel)
        whenever(embeddingModel.embed(any<TextSegment>()))
            .thenReturn(Response.from(Embedding.from(createMockEmbedding())))

        // Clear database in correct order to respect foreign keys
        incidentEmbeddingRepository.deleteAll()
        runbookEmbeddingRepository.deleteAll()
        diagnosisRepository.deleteAll()
        runbookFragmentRepository.deleteAll()
        incidentRepository.deleteAll()
    }

    @Test
    @Transactional
    fun `end-to-end embedding creation for incident`() {
        // Arrange
        val timestamp = Instant.now()
        val incident = Incident(
            id = IncidentId(0),
            source = "cloudwatch",
            title = "Database connection failed",
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

        // Verify embedding persisted
        val embeddings = incidentEmbeddingRepository.findAll().list()
        assertEquals(1, embeddings.size)
        val embedding = embeddings[0]
        assertEquals(incidentId.value, embedding.incident?.id)
        assertNotNull(embedding.text)
        assertTrue(embedding.embedding.isNotEmpty())
    }

    @Test
    @Transactional
    fun `end-to-end embedding creation for runbook fragment`() {
        // Arrange
        val timestamp = Instant.now()
        val fragment = RunbookFragmentEntity(
            id = null,
            title = "Database Connection Troubleshooting",
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

        // Verify embedding persisted
        val embeddings = runbookEmbeddingRepository.findAll().list()
        assertEquals(1, embeddings.size)
        val embedding = embeddings[0]
        assertEquals(fragmentId.value, embedding.fragment?.id)
        assertNotNull(embedding.text)
        assertTrue(embedding.embedding.isNotEmpty())
    }

    @Test
    @Transactional
    fun `verify embedding persisted in database with correct fields`() {
        // Arrange
        val timestamp = Instant.now()
        val incident = Incident(
            id = IncidentId(0),
            source = "manual",
            title = "Memory leak detected",
            description = "Memory usage steadily increasing over time",
            severity = Severity.CRITICAL,
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

        val embedding = incidentEmbeddingRepository.findAll().firstResult()
        assertNotNull(embedding)
        val storedEmbedding = requireNotNull(embedding)
        assertEquals(incidentId.value, storedEmbedding.incident?.id)
        assertTrue(storedEmbedding.text.contains("Memory leak detected"))
        assertTrue(storedEmbedding.text.contains("Memory usage steadily increasing"))
        // Check that it's a valid vector string
        assertTrue(storedEmbedding.embedding.startsWith("["))
        assertTrue(storedEmbedding.embedding.endsWith("]"))
        assertNotNull(storedEmbedding.createdAt)
    }

    @Test
    @Transactional
    fun `query embedding via repository and verify vector stored`() {
        // Arrange
        val timestamp = Instant.now()
        val incident = Incident(
            id = IncidentId(0),
            source = "monitoring",
            title = "High CPU usage",
            description = "CPU utilization exceeded 90% threshold",
            severity = Severity.HIGH,
            status = IncidentStatus.Open,
            createdAt = timestamp,
            updatedAt = timestamp
        )
        val entity = incident.toEntity()
        incidentRepository.persist(entity)
        val incidentId = IncidentId(requireNotNull(entity.id))

        // Act
        embeddingService.embedIncident(incidentId)

        // Assert
        val embedding = incidentEmbeddingRepository.findAll().firstResult()
        assertNotNull(embedding)
        val storedVectorString = requireNotNull(embedding).embedding

        // Verify the vector can be converted back to floats
        val floatArray = vectorStringToFloatArray(storedVectorString)
        assertEquals(768, floatArray.size)
        assertTrue(floatArray.any { it > 0f }) // Ensure we have non-zero values
    }

    @Test
    @Transactional
    fun `create multiple embeddings for different incidents`() {
        // Arrange
        val timestamp = Instant.now()
        val incidents = listOf(
            Incident(
                id = IncidentId(0),
                source = "cloudwatch",
                title = "Database connection failed",
                description = "Connection pool exhausted",
                severity = Severity.HIGH,
                status = IncidentStatus.Open,
                createdAt = timestamp,
                updatedAt = timestamp
            ),
            Incident(
                id = IncidentId(0),
                source = "monitoring",
                title = "Memory leak detected",
                description = "Memory usage increasing",
                severity = Severity.CRITICAL,
                status = IncidentStatus.Open,
                createdAt = timestamp,
                updatedAt = timestamp
            ),
            Incident(
                id = IncidentId(0),
                source = "manual",
                title = "Disk space warning",
                description = "Disk usage at 85%",
                severity = Severity.MEDIUM,
                status = IncidentStatus.Open,
                createdAt = timestamp,
                updatedAt = timestamp
            )
        )

        val incidentIds = incidents.map { incident ->
            val entity = incident.toEntity()
            incidentRepository.persist(entity)
            IncidentId(requireNotNull(entity.id))
        }

        // Act
        var successCount = 0
        incidentIds.forEach { id ->
            val result = embeddingService.embedIncident(id)
            if (result is Either.Right) {
                successCount += result.value
            }
        }

        // Assert
        assertEquals(3, successCount)
        val embeddings = incidentEmbeddingRepository.findAll().list()
        assertEquals(3, embeddings.size)
    }

    @Test
    @Transactional
    fun `create multiple embeddings for runbook fragments`() {
        // Arrange
        val timestamp = Instant.now()
        val fragments = listOf(
            RunbookFragmentEntity(
                id = null,
                title = "Database Troubleshooting",
                content = "Check connection pool and verify credentials",
                tags = "database,troubleshooting",
                createdAt = timestamp
            ),
            RunbookFragmentEntity(
                id = null,
                title = "Memory Issues",
                content = "Analyze heap dump and check for memory leaks",
                tags = "memory,performance",
                createdAt = timestamp
            )
        )

        fragments.forEach { runbookFragmentRepository.persist(it) }
        val fragmentIds = fragments.map { RunbookFragmentId(requireNotNull(it.id)) }

        // Act
        var successCount = 0
        fragmentIds.forEach { id ->
            val result = embeddingService.embedRunbook(id)
            if (result is Either.Right) {
                successCount += result.value
            }
        }

        // Assert
        assertEquals(2, successCount)
        val embeddings = runbookEmbeddingRepository.findAll().list()
        assertEquals(2, embeddings.size)
    }

    @Test
    @Transactional
    fun `embedBatch creates embeddings for both incidents and runbooks`() {
        // Arrange
        val timestamp = Instant.now()
        val incident = Incident(
            id = IncidentId(0),
            source = "cloudwatch",
            title = "CPU high",
            description = "CPU exceeded threshold",
            severity = Severity.HIGH,
            status = IncidentStatus.Open,
            createdAt = timestamp,
            updatedAt = timestamp
        )
        val fragment = RunbookFragmentEntity(
            id = null,
            title = "CPU Optimization",
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
        assertEquals(1, incidentEmbeddingRepository.count())
        assertEquals(1, runbookEmbeddingRepository.count())
    }

    @Test
    @Transactional
    fun `embedIncident fails for non-existent incident`() {
        // Act
        val result = embeddingService.embedIncident(IncidentId(999L))

        // Assert
        assertTrue(result is Either.Left)
        assertTrue((result as Either.Left).value is EmbeddingError.Unexpected)
    }

    @Test
    @Transactional
    fun `embedRunbook fails for non-existent fragment`() {
        // Act
        val result = embeddingService.embedRunbook(RunbookFragmentId(999L))

        // Assert
        assertTrue(result is Either.Left)
        assertTrue((result as Either.Left).value is EmbeddingError.Unexpected)
    }

    @Test
    @Transactional
    fun `embedding text includes both title and description`() {
        // Arrange
        val timestamp = Instant.now()
        val incident = Incident(
            id = IncidentId(0),
            source = "test",
            title = "Test Title ABC123",
            description = "Test Description XYZ789",
            severity = Severity.INFO,
            status = IncidentStatus.Open,
            createdAt = timestamp,
            updatedAt = timestamp
        )
        val entity = incident.toEntity()
        incidentRepository.persist(entity)
        val incidentId = IncidentId(requireNotNull(entity.id))

        // Act
        embeddingService.embedIncident(incidentId)

        // Assert
        val embedding = incidentEmbeddingRepository.findAll().firstResult()
        assertNotNull(embedding)
        val storedEmbedding = requireNotNull(embedding)
        assertTrue(storedEmbedding.text.contains("Test Title ABC123"))
        assertTrue(storedEmbedding.text.contains("Test Description XYZ789"))
    }

    private fun vectorStringToFloatArray(vector: String): FloatArray {
        if (vector.isBlank()) return FloatArray(0)
        return vector.removePrefix("[").removeSuffix("]")
            .split(",")
            .map { it.trim().toFloat() }
            .toFloatArray()
    }

    private fun createMockEmbedding(): FloatArray {
        return FloatArray(768) { it.toFloat() / 768f }
    }
}
