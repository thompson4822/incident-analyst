package com.example.incidentanalyst.rag

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
import org.mockito.Mockito.`when`
import org.mockito.Mockito.reset
import java.time.Instant

@QuarkusTest
class RetrievalServiceIntegrationTest {

    @Inject
    lateinit var embeddingService: EmbeddingService

    @InjectMock
    lateinit var embeddingModel: EmbeddingModel

    @Inject
    lateinit var retrievalService: RetrievalService

    @Inject
    lateinit var incidentRepository: IncidentRepository

    @Inject
    lateinit var incidentEmbeddingRepository: IncidentEmbeddingRepository

    @Inject
    lateinit var runbookFragmentRepository: RunbookFragmentRepository

    @Inject
    lateinit var runbookEmbeddingRepository: RunbookEmbeddingRepository

    @BeforeEach
    @Transactional
    fun setup() {
        reset(embeddingModel)
        `when`(embeddingModel.embed(org.mockito.Mockito.any(TextSegment::class.java)))
            .thenReturn(Response.from(Embedding.from(createMockEmbedding())))

        incidentEmbeddingRepository.deleteAll()
        runbookEmbeddingRepository.deleteAll()
        runbookFragmentRepository.deleteAll()
        incidentRepository.deleteAll()
    }

    @Test
    @Transactional
    fun `retrieve similar incidents using pgvector similarity`() {
        // Arrange - Create similar incidents
        val timestamp = Instant.now()
        val incident1 = Incident(
            id = IncidentId(0),
            source = "cloudwatch",
            title = "Database connection failed",
            description = "Connection pool exhausted due to high load",
            severity = Severity.HIGH,
            status = IncidentStatus.Open,
            createdAt = timestamp,
            updatedAt = timestamp
        )
        val incident2 = Incident(
            id = IncidentId(0),
            source = "monitoring",
            title = "Database connection timeout",
            description = "Unable to establish database connection",
            severity = Severity.HIGH,
            status = IncidentStatus.Open,
            createdAt = timestamp,
            updatedAt = timestamp
        )
        val incident3 = Incident(
            id = IncidentId(0),
            source = "manual",
            title = "Disk space warning",
            description = "Disk usage at 85%",
            severity = Severity.MEDIUM,
            status = IncidentStatus.Open,
            createdAt = timestamp,
            updatedAt = timestamp
        )

        val entity1 = incident1.toEntity()
        val entity2 = incident2.toEntity()
        val entity3 = incident3.toEntity()
        incidentRepository.persist(entity1, entity2, entity3)

        // Create embeddings
        val id1 = IncidentId(requireNotNull(entity1.id))
        val id2 = IncidentId(requireNotNull(entity2.id))
        val id3 = IncidentId(requireNotNull(entity3.id))
        embeddingService.embedIncident(id1)
        embeddingService.embedIncident(id2)
        embeddingService.embedIncident(id3)

        // Act - Retrieve similar incidents for a new similar query
        val queryIncident = Incident(
            id = IncidentId(0),
            source = "cloudwatch",
            title = "Database connection issue",
            description = "Experiencing database connection problems",
            severity = Severity.HIGH,
            status = IncidentStatus.Open,
            createdAt = timestamp,
            updatedAt = timestamp
        )

        val result = retrievalService.retrieve(queryIncident)

        // Assert
        assertTrue(result is RetrievalResult.Success)
        val context = (result as RetrievalResult.Success).context
        assertTrue(context.similarIncidents.isNotEmpty())
        assertTrue(context.similarIncidents.size <= 5) // Top-K limit of 5

        // Verify scores are above minimum threshold
        context.similarIncidents.forEach { match ->
            assertTrue(match.score.value >= 0.7, "Score ${match.score.value} should be >= 0.7")
        }
    }

    private fun createMockEmbedding(): FloatArray {
        return FloatArray(768) { it.toFloat() / 768f }
    }

    @Test
    @Transactional
    fun `retrieve similar runbooks using pgvector similarity`() {
        // Arrange - Create similar runbook fragments
        val timestamp = Instant.now()
        val fragment1 = RunbookFragmentEntity(
            id = null,
            title = "Database Connection Troubleshooting",
            content = "Check connection pool settings and verify database credentials",
            tags = "database,troubleshooting",
            createdAt = timestamp
        )
        val fragment2 = RunbookFragmentEntity(
            id = null,
            title = "Database Timeout Issues",
            content = "Increase connection timeout and check network connectivity",
            tags = "database,timeout",
            createdAt = timestamp
        )
        val fragment3 = RunbookFragmentEntity(
            id = null,
            title = "Disk Space Management",
            content = "Clean up old logs and free up disk space",
            tags = "disk,storage",
            createdAt = timestamp
        )

        runbookFragmentRepository.persist(fragment1, fragment2, fragment3)

        // Create embeddings
        val fragId1 = RunbookFragmentId(requireNotNull(fragment1.id))
        val fragId2 = RunbookFragmentId(requireNotNull(fragment2.id))
        val fragId3 = RunbookFragmentId(requireNotNull(fragment3.id))
        embeddingService.embedRunbook(fragId1)
        embeddingService.embedRunbook(fragId2)
        embeddingService.embedRunbook(fragId3)

        // Act - Retrieve similar runbooks for a new query
        val queryFragment = RunbookFragment(
            id = RunbookFragmentId(0),
            title = "Database Connection Problems",
            content = "How to fix database connection issues",
            tags = null,
            createdAt = timestamp
        )

        val result = retrievalService.retrieveForRunbook(queryFragment)

        // Assert
        assertTrue(result is RetrievalResult.Success)
        val context = (result as RetrievalResult.Success).context
        assertTrue(context.similarRunbooks.isNotEmpty())
        assertTrue(context.similarRunbooks.size <= 2) // Top-K limit of 2

        // Verify scores are above minimum threshold
        context.similarRunbooks.forEach { match ->
            assertTrue(match.score.value >= 0.7, "Score ${match.score.value} should be >= 0.7")
        }
    }

    @Test
    @Transactional
    fun `verify top-K ordering - most similar first`() {
        // Arrange - Create incidents with different similarities
        val timestamp = Instant.now()
        val incidents = listOf(
            Incident(
                id = IncidentId(0),
                source = "test",
                title = "Database connection failed",
                description = "Connection pool exhausted",
                severity = Severity.HIGH,
                status = IncidentStatus.Open,
                createdAt = timestamp,
                updatedAt = timestamp
            ),
            Incident(
                id = IncidentId(0),
                source = "test",
                title = "Database connection issue",
                description = "Experiencing database problems",
                severity = Severity.HIGH,
                status = IncidentStatus.Open,
                createdAt = timestamp,
                updatedAt = timestamp
            ),
            Incident(
                id = IncidentId(0),
                source = "test",
                title = "CPU usage high",
                description = "CPU exceeded 90% threshold",
                severity = Severity.HIGH,
                status = IncidentStatus.Open,
                createdAt = timestamp,
                updatedAt = timestamp
            )
        )

        val entityIds = incidents.map { incident ->
            val entity = incident.toEntity()
            incidentRepository.persist(entity)
            IncidentId(requireNotNull(entity.id))
        }

        // Create embeddings
        entityIds.forEach { embeddingService.embedIncident(it) }

        // Act - Retrieve with a database-related query
        val queryIncident = Incident(
            id = IncidentId(0),
            source = "test",
            title = "Database problem",
            description = "Having database issues",
            severity = Severity.HIGH,
            status = IncidentStatus.Open,
            createdAt = timestamp,
            updatedAt = timestamp
        )

        val result = retrievalService.retrieve(queryIncident)

        // Assert - Results should be ordered by similarity (highest first)
        assertTrue(result is RetrievalResult.Success)
        val context = (result as RetrievalResult.Success).context

        if (context.similarIncidents.size >= 2) {
            var previousScore = 1.0
            context.similarIncidents.forEach { match ->
                assertTrue(
                    match.score.value <= previousScore,
                    "Scores should be in descending order"
                )
                previousScore = match.score.value
            }
        }
    }

    @Test
    @Transactional
    fun `verify score filtering - results above 0_7 threshold`() {
        // Arrange - Create incidents
        val timestamp = Instant.now()
        val incident1 = Incident(
            id = IncidentId(0),
            source = "test",
            title = "Database connection failed",
            description = "Connection pool exhausted",
            severity = Severity.HIGH,
            status = IncidentStatus.Open,
            createdAt = timestamp,
            updatedAt = timestamp
        )
        val entity1 = incident1.toEntity()
        incidentRepository.persist(entity1)
        val id1 = IncidentId(requireNotNull(entity1.id))
        embeddingService.embedIncident(id1)

        // Act - Retrieve with similar query
        val queryIncident = Incident(
            id = IncidentId(0),
            source = "test",
            title = "Database connection issue",
            description = "Connection problem",
            severity = Severity.HIGH,
            status = IncidentStatus.Open,
            createdAt = timestamp,
            updatedAt = timestamp
        )

        val result = retrievalService.retrieve(queryIncident)

        // Assert - All results should have score >= 0.7
        assertTrue(result is RetrievalResult.Success)
        val context = (result as RetrievalResult.Success).context

        context.similarIncidents.forEach { match ->
            assertTrue(
                match.score.value >= 0.7,
                "Score ${match.score.value} should be >= 0.7"
            )
        }
    }

    @Test
    @Transactional
    fun `RetrievalContext construction contains both incidents and runbooks`() {
        // Arrange - Create both incidents and runbooks
        val timestamp = Instant.now()
        val incident = Incident(
            id = IncidentId(0),
            source = "test",
            title = "Database connection failed",
            description = "Connection pool exhausted",
            severity = Severity.HIGH,
            status = IncidentStatus.Open,
            createdAt = timestamp,
            updatedAt = timestamp
        )
        val fragment = RunbookFragmentEntity(
            id = null,
            title = "Database Troubleshooting",
            content = "Check connection pool settings",
            tags = "database",
            createdAt = timestamp
        )

        val incidentEntity = incident.toEntity()
        incidentRepository.persist(incidentEntity)
        runbookFragmentRepository.persist(fragment)

        val incidentId = IncidentId(requireNotNull(incidentEntity.id))
        val fragmentId = RunbookFragmentId(requireNotNull(fragment.id))

        embeddingService.embedIncident(incidentId)
        embeddingService.embedRunbook(fragmentId)

        // Act
        val queryIncident = Incident(
            id = IncidentId(0),
            source = "test",
            title = "Database issue",
            description = "Database problem",
            severity = Severity.HIGH,
            status = IncidentStatus.Open,
            createdAt = timestamp,
            updatedAt = timestamp
        )

        val result = retrievalService.retrieve(queryIncident)

        // Assert
        assertTrue(result is RetrievalResult.Success)
        val context = (result as RetrievalResult.Success).context

        // Verify context has both types of results
        assertNotNull(context.similarIncidents)
        assertNotNull(context.similarRunbooks)
        assertNotNull(context.query)
        assertTrue(context.query.contains("Database issue"))
    }

    @Test
    @Transactional
    fun `verify similar incidents and runbooks are correctly linked`() {
        // Arrange - Create incidents and runbooks
        val timestamp = Instant.now()
        val incident = Incident(
            id = IncidentId(0),
            source = "test",
            title = "CPU High Usage",
            description = "CPU exceeded 90%",
            severity = Severity.HIGH,
            status = IncidentStatus.Open,
            createdAt = timestamp,
            updatedAt = timestamp
        )
        val fragment = RunbookFragmentEntity(
            id = null,
            title = "CPU Optimization",
            content = "Reduce CPU by optimizing queries",
            tags = "cpu,performance",
            createdAt = timestamp
        )

        val incidentEntity = incident.toEntity()
        incidentRepository.persist(incidentEntity)
        runbookFragmentRepository.persist(fragment)

        val incidentId = IncidentId(requireNotNull(incidentEntity.id))
        val fragmentId = RunbookFragmentId(requireNotNull(fragment.id))

        embeddingService.embedIncident(incidentId)
        embeddingService.embedRunbook(fragmentId)

        // Act - Retrieve and verify IDs match
        val queryIncident = Incident(
            id = IncidentId(0),
            source = "test",
            title = "CPU problem",
            description = "CPU issue",
            severity = Severity.HIGH,
            status = IncidentStatus.Open,
            createdAt = timestamp,
            updatedAt = timestamp
        )

        val result = retrievalService.retrieve(queryIncident)

        // Assert
        assertTrue(result is RetrievalResult.Success)
        val context = (result as RetrievalResult.Success).context

        context.similarIncidents.forEach { match ->
            assertTrue(match.id is IncidentId)
            assertEquals(incidentId.value, match.id.value)
        }

        context.similarRunbooks.forEach { match ->
            assertTrue(match.id is RunbookFragmentId)
            assertEquals(fragmentId.value, match.id.value)
        }
    }

    @Test
    @Transactional
    fun `retrieve returns no results when no embeddings exist`() {
        // Arrange - Create query without any embeddings in database
        val timestamp = Instant.now()
        val queryIncident = Incident(
            id = IncidentId(0),
            source = "test",
            title = "Test incident",
            description = "Test description",
            severity = Severity.INFO,
            status = IncidentStatus.Open,
            createdAt = timestamp,
            updatedAt = timestamp
        )

        // Act
        val result = retrievalService.retrieve(queryIncident)

        // Assert
        assertTrue(result is RetrievalResult.Success)
        val context = (result as RetrievalResult.Success).context
        assertEquals(0, context.similarIncidents.size)
        assertEquals(0, context.similarRunbooks.size)
    }

    @Test
    @Transactional
    fun `retrieveForRunbook returns no results when no embeddings exist`() {
        // Arrange - Create query without any embeddings in database
        val timestamp = Instant.now()
        val queryFragment = RunbookFragment(
            id = RunbookFragmentId(0),
            title = "Test fragment",
            content = "Test content",
            tags = null,
            createdAt = timestamp
        )

        // Act
        val result = retrievalService.retrieveForRunbook(queryFragment)

        // Assert
        assertTrue(result is RetrievalResult.Success)
        val context = (result as RetrievalResult.Success).context
        assertEquals(0, context.similarIncidents.size)
        assertEquals(0, context.similarRunbooks.size)
    }

    @Test
    @Transactional
    fun `retrieve with invalid query returns failure`() {
        // Arrange
        val invalidIncident = Incident(
            id = IncidentId(0),
            source = "test",
            title = "   ",
            description = "   ",
            severity = Severity.INFO,
            status = IncidentStatus.Open,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        // Act
        val result = retrievalService.retrieve(invalidIncident)

        // Assert
        assertTrue(result is RetrievalResult.Failure)
        assertTrue((result as RetrievalResult.Failure).error is RetrievalError.InvalidQuery)
    }

    @Test
    @Transactional
    fun `retrieveForRunbook with invalid query returns failure`() {
        // Arrange
        val invalidFragment = RunbookFragment(
            id = RunbookFragmentId(0),
            title = "   ",
            content = "   ",
            tags = null,
            createdAt = Instant.now()
        )

        // Act
        val result = retrievalService.retrieveForRunbook(invalidFragment)

        // Assert
        assertTrue(result is RetrievalResult.Failure)
        assertTrue((result as RetrievalResult.Failure).error is RetrievalError.InvalidQuery)
    }

    @Test
    @Transactional
    fun `query includes incident severity in retrieval context`() {
        // Arrange
        val timestamp = Instant.now()
        val incident = Incident(
            id = IncidentId(0),
            source = "test",
            title = "Test",
            description = "Test description",
            severity = Severity.CRITICAL,
            status = IncidentStatus.Open,
            createdAt = timestamp,
            updatedAt = timestamp
        )
        val entity = incident.toEntity()
        incidentRepository.persist(entity)
        val incidentId = IncidentId(requireNotNull(entity.id))
        embeddingService.embedIncident(incidentId)

        // Act
        val queryIncident = Incident(
            id = IncidentId(0),
            source = "test",
            title = "Test query",
            description = "Test query description",
            severity = Severity.HIGH,
            status = IncidentStatus.Open,
            createdAt = timestamp,
            updatedAt = timestamp
        )

        val result = retrievalService.retrieve(queryIncident)

        // Assert
        assertTrue(result is RetrievalResult.Success)
        val context = (result as RetrievalResult.Success).context
        assertTrue(context.query.contains("HIGH"))
    }
}
