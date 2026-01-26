package com.example.incidentanalyst.rag

import com.example.incidentanalyst.incident.Incident
import com.example.incidentanalyst.incident.IncidentEntity
import com.example.incidentanalyst.incident.IncidentId
import com.example.incidentanalyst.incident.IncidentStatus
import com.example.incidentanalyst.incident.Severity
import com.example.incidentanalyst.incident.IncidentRepository
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
import org.hibernate.exception.ConstraintViolationException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.reset
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.time.Instant

@QuarkusTest
class EmbeddingServiceTest {

    @InjectMock
    lateinit var embeddingModel: EmbeddingModel

    @InjectMock
    lateinit var incidentRepository: IncidentRepository

    @InjectMock
    lateinit var incidentEmbeddingRepository: IncidentEmbeddingRepository

    @InjectMock
    lateinit var runbookFragmentRepository: RunbookFragmentRepository

    @InjectMock
    lateinit var runbookEmbeddingRepository: RunbookEmbeddingRepository

    @Inject
    lateinit var embeddingService: EmbeddingService

    private val testTimestamp = Instant.now()

    @BeforeEach
    fun setup() {
        reset(
            embeddingModel,
            incidentRepository,
            incidentEmbeddingRepository,
            runbookFragmentRepository,
            runbookEmbeddingRepository
        )
    }

    @Test
    fun `embedIncident success - embedding generated and persisted`() {
        // Arrange
        val incidentId = IncidentId(123L)
        val incidentEntity = IncidentEntity(
            id = incidentId.value,
            source = "cloudwatch",
            title = "High CPU Usage",
            description = "CPU usage exceeded 90% threshold",
            severity = "HIGH",
            status = "OPEN",
            createdAt = testTimestamp,
            updatedAt = testTimestamp
        )
        val mockEmbedding = createMockEmbedding()

        `when`(incidentRepository.findById(incidentId.value)).thenReturn(incidentEntity)
        `when`(embeddingModel.embed(any<TextSegment>()))
            .thenReturn(Response.from(Embedding.from(mockEmbedding)))

        // Act
        val result = embeddingService.embedIncident(incidentId)

        // Assert
        assertTrue(result is EmbeddingResult.Success)
        assertEquals(1, (result as EmbeddingResult.Success).count)
        verify(incidentEmbeddingRepository).persist(any<IncidentEmbeddingEntity>())
    }

    @Test
    fun `embedIncident model unavailable - throws exception`() {
        // Arrange
        val incidentId = IncidentId(123L)
        val incidentEntity = IncidentEntity(
            id = incidentId.value,
            source = "cloudwatch",
            title = "Test",
            description = "Test description",
            severity = "HIGH",
            status = "OPEN",
            createdAt = testTimestamp,
            updatedAt = testTimestamp
        )

        `when`(incidentRepository.findById(incidentId.value)).thenReturn(incidentEntity)
        `when`(embeddingModel.embed(any<TextSegment>()))
            .thenThrow(RuntimeException("Model unavailable"))

        // Act
        val result = embeddingService.embedIncident(incidentId)

        // Assert
        assertTrue(result is EmbeddingResult.Failure)
        val error = (result as EmbeddingResult.Failure).error
        assertTrue(error is EmbeddingError.EmbeddingFailed)
    }

    @Test
    fun `embedIncident persistence error - repository throws exception`() {
        // Arrange
        val incidentId = IncidentId(123L)
        val incidentEntity = IncidentEntity(
            id = incidentId.value,
            source = "cloudwatch",
            title = "Test",
            description = "Test description",
            severity = "HIGH",
            status = "OPEN",
            createdAt = testTimestamp,
            updatedAt = testTimestamp
        )
        val mockEmbedding = createMockEmbedding()

        `when`(incidentRepository.findById(incidentId.value)).thenReturn(incidentEntity)
        `when`(embeddingModel.embed(any<TextSegment>()))
            .thenReturn(Response.from(Embedding.from(mockEmbedding)))
        doThrow(ConstraintViolationException("Unique constraint violation", null, "uk_test"))
            .`when`(incidentEmbeddingRepository).persist(any<IncidentEmbeddingEntity>())

        // Act
        val result = embeddingService.embedIncident(incidentId)

        // Assert
        assertTrue(result is EmbeddingResult.Failure)
        val error = (result as EmbeddingResult.Failure).error
        assertTrue(error is EmbeddingError.PersistenceError)
    }

    @Test
    fun `embedIncident invalid text - empty description`() {
        // Arrange
        val incidentId = IncidentId(123L)
        val incidentEntity = IncidentEntity(
            id = incidentId.value,
            source = "cloudwatch",
            title = "   ",
            description = "   ",
            severity = "HIGH",
            status = "OPEN",
            createdAt = testTimestamp,
            updatedAt = testTimestamp
        )

        `when`(incidentRepository.findById(incidentId.value)).thenReturn(incidentEntity)

        // Act
        val result = embeddingService.embedIncident(incidentId)

        // Assert
        assertTrue(result is EmbeddingResult.Failure)
        val error = (result as EmbeddingResult.Failure).error
        assertTrue(error is EmbeddingError.InvalidText)
    }

    @Test
    fun `embedIncident embedding failed - model throws exception`() {
        // Arrange
        val incidentId = IncidentId(123L)
        val incidentEntity = IncidentEntity(
            id = incidentId.value,
            source = "cloudwatch",
            title = "Test",
            description = "Test description",
            severity = "HIGH",
            status = "OPEN",
            createdAt = testTimestamp,
            updatedAt = testTimestamp
        )

        `when`(incidentRepository.findById(incidentId.value)).thenReturn(incidentEntity)
        `when`(embeddingModel.embed(any<TextSegment>()))
            .thenThrow(IllegalStateException("Embedding failed"))

        // Act
        val result = embeddingService.embedIncident(incidentId)

        // Assert
        assertTrue(result is EmbeddingResult.Failure)
        val error = (result as EmbeddingResult.Failure).error
        assertTrue(error is EmbeddingError.EmbeddingFailed)
    }

    @Test
    fun `embedIncident incident not found`() {
        // Arrange
        val incidentId = IncidentId(999L)
        `when`(incidentRepository.findById(incidentId.value)).thenReturn(null)

        // Act
        val result = embeddingService.embedIncident(incidentId)

        // Assert
        assertTrue(result is EmbeddingResult.Failure)
        val error = (result as EmbeddingResult.Failure).error
        assertTrue(error is EmbeddingError.Unexpected)
    }

    @Test
    fun `embedRunbook success - embedding generated and persisted`() {
        // Arrange
        val fragmentId = RunbookFragmentId(456L)
        val fragmentEntity = RunbookFragmentEntity(
            id = fragmentId.value,
            title = "Database Connection Troubleshooting",
            content = "Check connection pool settings",
            tags = "database,troubleshooting",
            createdAt = testTimestamp
        )
        val mockEmbedding = createMockEmbedding()

        `when`(runbookFragmentRepository.findById(fragmentId.value)).thenReturn(fragmentEntity)
        `when`(embeddingModel.embed(any<TextSegment>()))
            .thenReturn(Response.from(Embedding.from(mockEmbedding)))

        // Act
        val result = embeddingService.embedRunbook(fragmentId)

        // Assert
        assertTrue(result is EmbeddingResult.Success)
        assertEquals(1, (result as EmbeddingResult.Success).count)
        verify(runbookEmbeddingRepository).persist(any<RunbookEmbeddingEntity>())
    }

    @Test
    fun `embedRunbook model unavailable - throws exception`() {
        // Arrange
        val fragmentId = RunbookFragmentId(456L)
        val fragmentEntity = RunbookFragmentEntity(
            id = fragmentId.value,
            title = "Test",
            content = "Test content",
            tags = null,
            createdAt = testTimestamp
        )

        `when`(runbookFragmentRepository.findById(fragmentId.value)).thenReturn(fragmentEntity)
        `when`(embeddingModel.embed(any<TextSegment>()))
            .thenThrow(RuntimeException("Model unavailable"))

        // Act
        val result = embeddingService.embedRunbook(fragmentId)

        // Assert
        assertTrue(result is EmbeddingResult.Failure)
        val error = (result as EmbeddingResult.Failure).error
        assertTrue(error is EmbeddingError.EmbeddingFailed)
    }

    @Test
    fun `embedRunbook persistence error - repository throws exception`() {
        // Arrange
        val fragmentId = RunbookFragmentId(456L)
        val fragmentEntity = RunbookFragmentEntity(
            id = fragmentId.value,
            title = "Test",
            content = "Test content",
            tags = null,
            createdAt = testTimestamp
        )
        val mockEmbedding = createMockEmbedding()

        `when`(runbookFragmentRepository.findById(fragmentId.value)).thenReturn(fragmentEntity)
        `when`(embeddingModel.embed(any<TextSegment>()))
            .thenReturn(Response.from(Embedding.from(mockEmbedding)))
        doThrow(ConstraintViolationException("Unique constraint violation", null, "uk_test"))
            .`when`(runbookEmbeddingRepository).persist(any<RunbookEmbeddingEntity>())

        // Act
        val result = embeddingService.embedRunbook(fragmentId)

        // Assert
        assertTrue(result is EmbeddingResult.Failure)
        val error = (result as EmbeddingResult.Failure).error
        assertTrue(error is EmbeddingError.PersistenceError)
    }

    @Test
    fun `embedRunbook invalid text - empty content`() {
        // Arrange
        val fragmentId = RunbookFragmentId(456L)
        val fragmentEntity = RunbookFragmentEntity(
            id = fragmentId.value,
            title = "   ",
            content = "   ",
            tags = null,
            createdAt = testTimestamp
        )

        `when`(runbookFragmentRepository.findById(fragmentId.value)).thenReturn(fragmentEntity)

        // Act
        val result = embeddingService.embedRunbook(fragmentId)

        // Assert
        assertTrue(result is EmbeddingResult.Failure)
        val error = (result as EmbeddingResult.Failure).error
        assertTrue(error is EmbeddingError.InvalidText)
    }

    @Test
    fun `embedRunbook fragment not found`() {
        // Arrange
        val fragmentId = RunbookFragmentId(999L)
        `when`(runbookFragmentRepository.findById(fragmentId.value)).thenReturn(null)

        // Act
        val result = embeddingService.embedRunbook(fragmentId)

        // Assert
        assertTrue(result is EmbeddingResult.Failure)
        val error = (result as EmbeddingResult.Failure).error
        assertTrue(error is EmbeddingError.Unexpected)
    }

    @Test
    fun `embedBatch success - multiple embeddings generated and persisted`() {
        // Arrange
        val incidentId1 = IncidentId(1L)
        val incidentId2 = IncidentId(2L)
        val fragmentId1 = RunbookFragmentId(3L)
        val fragmentId2 = RunbookFragmentId(4L)

        val incidentEntity1 = IncidentEntity(
            id = incidentId1.value,
            source = "cloudwatch",
            title = "Incident 1",
            description = "Description 1",
            severity = "HIGH",
            status = "OPEN",
            createdAt = testTimestamp,
            updatedAt = testTimestamp
        )
        val incidentEntity2 = IncidentEntity(
            id = incidentId2.value,
            source = "cloudwatch",
            title = "Incident 2",
            description = "Description 2",
            severity = "MEDIUM",
            status = "OPEN",
            createdAt = testTimestamp,
            updatedAt = testTimestamp
        )

        val fragmentEntity1 = RunbookFragmentEntity(
            id = fragmentId1.value,
            title = "Fragment 1",
            content = "Content 1",
            tags = null,
            createdAt = testTimestamp
        )
        val fragmentEntity2 = RunbookFragmentEntity(
            id = fragmentId2.value,
            title = "Fragment 2",
            content = "Content 2",
            tags = null,
            createdAt = testTimestamp
        )

        val mockEmbedding = createMockEmbedding()

        `when`(incidentRepository.findById(incidentId1.value)).thenReturn(incidentEntity1)
        `when`(incidentRepository.findById(incidentId2.value)).thenReturn(incidentEntity2)
        `when`(runbookFragmentRepository.findById(fragmentId1.value)).thenReturn(fragmentEntity1)
        `when`(runbookFragmentRepository.findById(fragmentId2.value)).thenReturn(fragmentEntity2)
        `when`(embeddingModel.embed(any<TextSegment>()))
            .thenReturn(Response.from(Embedding.from(mockEmbedding)))

        // Act
        val result = embeddingService.embedBatch(
            incidentIds = listOf(incidentId1, incidentId2),
            fragmentIds = listOf(fragmentId1, fragmentId2)
        )

        // Assert
        assertTrue(result is EmbeddingResult.Success)
        assertEquals(4, (result as EmbeddingResult.Success).count)
    }

    @Test
    fun `embedBatch partial failure - some embeddings succeed`() {
        // Arrange
        val incidentId1 = IncidentId(1L)
        val incidentId2 = IncidentId(999L) // Not found
        val fragmentId1 = RunbookFragmentId(3L)

        val incidentEntity1 = IncidentEntity(
            id = incidentId1.value,
            source = "cloudwatch",
            title = "Incident 1",
            description = "Description 1",
            severity = "HIGH",
            status = "OPEN",
            createdAt = testTimestamp,
            updatedAt = testTimestamp
        )

        val fragmentEntity1 = RunbookFragmentEntity(
            id = fragmentId1.value,
            title = "Fragment 1",
            content = "Content 1",
            tags = null,
            createdAt = testTimestamp
        )

        val mockEmbedding = createMockEmbedding()

        `when`(incidentRepository.findById(incidentId1.value)).thenReturn(incidentEntity1)
        `when`(incidentRepository.findById(incidentId2.value)).thenReturn(null)
        `when`(runbookFragmentRepository.findById(fragmentId1.value)).thenReturn(fragmentEntity1)
        `when`(embeddingModel.embed(any<TextSegment>()))
            .thenReturn(Response.from(Embedding.from(mockEmbedding)))

        // Act
        val result = embeddingService.embedBatch(
            incidentIds = listOf(incidentId1, incidentId2),
            fragmentIds = listOf(fragmentId1)
        )

        // Assert
        assertTrue(result is EmbeddingResult.Success)
        assertEquals(2, (result as EmbeddingResult.Success).count)
    }

    @Test
    fun `embedBatch empty lists - returns success with count 0`() {
        // Act
        val result = embeddingService.embedBatch(
            incidentIds = emptyList(),
            fragmentIds = emptyList()
        )

        // Assert
        assertTrue(result is EmbeddingResult.Success)
        assertEquals(0, (result as EmbeddingResult.Success).count)
    }

    private fun createMockEmbedding(): FloatArray {
        // Create a 768-dimensional mock embedding (matching nomic-embed-text dimensions)
        return FloatArray(768) { it.toFloat() / 768f }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> any(): T = org.mockito.Mockito.any()
}
