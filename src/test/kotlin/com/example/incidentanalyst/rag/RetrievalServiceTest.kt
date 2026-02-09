package com.example.incidentanalyst.rag

import com.example.incidentanalyst.common.Either
import com.example.incidentanalyst.incident.Incident
import com.example.incidentanalyst.incident.IncidentId
import com.example.incidentanalyst.incident.IncidentStatus
import com.example.incidentanalyst.incident.Severity
import com.example.incidentanalyst.runbook.RunbookFragment
import com.example.incidentanalyst.runbook.RunbookFragmentId
import dev.langchain4j.data.embedding.Embedding
import dev.langchain4j.data.segment.TextSegment
import dev.langchain4j.model.embedding.EmbeddingModel
import dev.langchain4j.model.output.Response
import io.quarkus.test.InjectMock
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.`when`
import org.mockito.Mockito.reset
import java.time.Instant

@QuarkusTest
class RetrievalServiceTest {

    @InjectMock
    lateinit var embeddingModel: EmbeddingModel

    @InjectMock
    lateinit var incidentEmbeddingRepository: IncidentEmbeddingRepository

    @InjectMock
    lateinit var runbookEmbeddingRepository: RunbookEmbeddingRepository

    @Inject
    lateinit var retrievalService: RetrievalService

    private val testTimestamp = Instant.now()

    @Suppress("UNCHECKED_CAST")
    private fun <T> any(): T = org.mockito.Mockito.any()

    @BeforeEach
    fun setup() {
        reset(embeddingModel, incidentEmbeddingRepository, runbookEmbeddingRepository)
    }

    @Test
    fun `retrieve for incident success - returns top 5 similar incidents`() {
        // Arrange
        val incident = Incident(
            id = IncidentId(1L),
            source = "cloudwatch",
            title = "Database connection failed",
            description = "Connection pool exhausted",
            severity = Severity.HIGH,
            status = IncidentStatus.Open,
            createdAt = testTimestamp,
            updatedAt = testTimestamp
        )
        val queryEmbedding = createMockEmbedding()
        val similarIncidents = createMockSimilarIncidents(3)
        val similarRunbooks = createMockSimilarRunbooks(2)

        `when`(embeddingModel.embed(any<TextSegment>()))
            .thenReturn(Response.from(Embedding.from(queryEmbedding)))
        `when`(
            incidentEmbeddingRepository.findSimilar(
                any<ByteArray>(),
                eq(0.7),
                eq(5)
            )
        ).thenReturn(similarIncidents)
        `when`(
            runbookEmbeddingRepository.findSimilar(
                any<ByteArray>(),
                eq(0.7),
                eq(2)
            )
        ).thenReturn(similarRunbooks)

        // Act
        val result = retrievalService.retrieve(incident)

        // Assert
        assertTrue(result is Either.Right)
        val context = (result as Either.Right).value
        assertEquals(3, context.similarIncidents.size)
        assertEquals(2, context.similarRunbooks.size)
        assertTrue(context.query.contains("Database connection failed"))
    }

    @Test
    fun `retrieve for incident model unavailable - throws exception`() {
        // Arrange
        val incident = Incident(
            id = IncidentId(1L),
            source = "cloudwatch",
            title = "Test",
            description = "Test description",
            severity = Severity.HIGH,
            status = IncidentStatus.Open,
            createdAt = testTimestamp,
            updatedAt = testTimestamp
        )

        `when`(embeddingModel.embed(any<TextSegment>()))
            .thenThrow(RuntimeException("Model unavailable"))

        // Act
        val result = retrievalService.retrieve(incident)

        // Assert
        assertTrue(result is Either.Left)
        val error = (result as Either.Left).value
        assertTrue(error is RetrievalError.ModelUnavailable)
    }

    @Test
    fun `retrieve for incident no results - empty lists returned`() {
        // Arrange
        val incident = Incident(
            id = IncidentId(1L),
            source = "cloudwatch",
            title = "Test",
            description = "Test description",
            severity = Severity.HIGH,
            status = IncidentStatus.Open,
            createdAt = testTimestamp,
            updatedAt = testTimestamp
        )
        val queryEmbedding = createMockEmbedding()

        `when`(embeddingModel.embed(any<TextSegment>()))
            .thenReturn(Response.from(Embedding.from(queryEmbedding)))
        `when`(
            incidentEmbeddingRepository.findSimilar(
                any<ByteArray>(),
                eq(0.7),
                eq(5)
            )
        ).thenReturn(emptyList())
        `when`(
            runbookEmbeddingRepository.findSimilar(
                any<ByteArray>(),
                eq(0.7),
                eq(2)
            )
        ).thenReturn(emptyList())

        // Act
        val result = retrievalService.retrieve(incident)

        // Assert
        assertTrue(result is Either.Right)
        val context = (result as Either.Right).value
        assertEquals(0, context.similarIncidents.size)
        assertEquals(0, context.similarRunbooks.size)
    }

    @Test
    fun `retrieve for incident search failed - repository throws exception`() {
        // Arrange
        val incident = Incident(
            id = IncidentId(1L),
            source = "cloudwatch",
            title = "Test",
            description = "Test description",
            severity = Severity.HIGH,
            status = IncidentStatus.Open,
            createdAt = testTimestamp,
            updatedAt = testTimestamp
        )
        val queryEmbedding = createMockEmbedding()

        `when`(embeddingModel.embed(any<TextSegment>()))
            .thenReturn(Response.from(Embedding.from(queryEmbedding)))
        `when`(
            incidentEmbeddingRepository.findSimilar(
                any<ByteArray>(),
                eq(0.7),
                eq(5)
            )
        ).thenThrow(RuntimeException("Database error"))

        // Act
        val result = retrievalService.retrieve(incident)

        // Assert
        assertTrue(result is Either.Left)
        val error = (result as Either.Left).value
        assertTrue(error is RetrievalError.SearchFailed)
    }

    @Test
    fun `retrieve for incident invalid query - blank description`() {
        // Arrange
        val incident = Incident(
            id = IncidentId(1L),
            source = "cloudwatch",
            title = "   ",
            description = "   ",
            severity = Severity.HIGH,
            status = IncidentStatus.Open,
            createdAt = testTimestamp,
            updatedAt = testTimestamp
        )

        // Act
        val result = retrievalService.retrieve(incident)

        // Assert
        assertTrue(result is Either.Left)
        val error = (result as Either.Left).value
        assertTrue(error is RetrievalError.InvalidQuery)
    }

    @Test
    fun `retrieve for runbook success - returns top 2 similar runbooks`() {
        // Arrange
        val fragment = RunbookFragment(
            id = RunbookFragmentId(1L),
            title = "Database connection troubleshooting",
            content = "Check connection pool settings",
            tags = "database,troubleshooting",
            createdAt = testTimestamp
        )
        val queryEmbedding = createMockEmbedding()
        val similarIncidents = createMockSimilarIncidents(2)
        val similarRunbooks = createMockSimilarRunbooks(2)

        `when`(embeddingModel.embed(any<TextSegment>()))
            .thenReturn(Response.from(Embedding.from(queryEmbedding)))
        `when`(
            incidentEmbeddingRepository.findSimilar(
                any<ByteArray>(),
                eq(0.7),
                eq(5)
            )
        ).thenReturn(similarIncidents)
        `when`(
            runbookEmbeddingRepository.findSimilar(
                any<ByteArray>(),
                eq(0.7),
                eq(2)
            )
        ).thenReturn(similarRunbooks)

        // Act
        val result = retrievalService.retrieveForRunbook(fragment)

        // Assert
        assertTrue(result is Either.Right)
        val context = (result as Either.Right).value
        assertEquals(2, context.similarIncidents.size)
        assertEquals(2, context.similarRunbooks.size)
        assertTrue(context.query.contains("Database connection troubleshooting"))
    }

    @Test
    fun `retrieve for runbook no results - empty lists returned`() {
        // Arrange
        val fragment = RunbookFragment(
            id = RunbookFragmentId(1L),
            title = "Test",
            content = "Test content",
            tags = null,
            createdAt = testTimestamp
        )
        val queryEmbedding = createMockEmbedding()

        `when`(embeddingModel.embed(any<TextSegment>()))
            .thenReturn(Response.from(Embedding.from(queryEmbedding)))
        `when`(
            incidentEmbeddingRepository.findSimilar(
                any<ByteArray>(),
                eq(0.7),
                eq(5)
            )
        ).thenReturn(emptyList())
        `when`(
            runbookEmbeddingRepository.findSimilar(
                any<ByteArray>(),
                eq(0.7),
                eq(2)
            )
        ).thenReturn(emptyList())

        // Act
        val result = retrievalService.retrieveForRunbook(fragment)

        // Assert
        assertTrue(result is Either.Right)
        val context = (result as Either.Right).value
        assertEquals(0, context.similarIncidents.size)
        assertEquals(0, context.similarRunbooks.size)
    }

    @Test
    fun `retrieve for runbook search failed - repository throws exception`() {
        // Arrange
        val fragment = RunbookFragment(
            id = RunbookFragmentId(1L),
            title = "Test",
            content = "Test content",
            tags = null,
            createdAt = testTimestamp
        )
        val queryEmbedding = createMockEmbedding()

        `when`(embeddingModel.embed(any<TextSegment>()))
            .thenReturn(Response.from(Embedding.from(queryEmbedding)))
        `when`(
            runbookEmbeddingRepository.findSimilar(
                any<ByteArray>(),
                eq(0.7),
                eq(2)
            )
        ).thenThrow(RuntimeException("Database error"))

        // Act
        val result = retrievalService.retrieveForRunbook(fragment)

        // Assert
        assertTrue(result is Either.Left)
        val error = (result as Either.Left).value
        assertTrue(error is RetrievalError.SearchFailed)
    }

    @Test
    fun `retrieve for runbook invalid query - blank content`() {
        // Arrange
        val fragment = RunbookFragment(
            id = RunbookFragmentId(1L),
            title = "   ",
            content = "   ",
            tags = null,
            createdAt = testTimestamp
        )

        // Act
        val result = retrievalService.retrieveForRunbook(fragment)

        // Assert
        assertTrue(result is Either.Left)
        val error = (result as Either.Left).value
        assertTrue(error is RetrievalError.InvalidQuery)
    }

    @Test
    fun `RetrievalMatch includes score and snippet`() {
        // Arrange
        val incident = Incident(
            id = IncidentId(1L),
            source = "cloudwatch",
            title = "Test",
            description = "Test description",
            severity = Severity.HIGH,
            status = IncidentStatus.Open,
            createdAt = testTimestamp,
            updatedAt = testTimestamp
        )
        val queryEmbedding = createMockEmbedding()
        val similarIncidents = createMockSimilarIncidents(1)

        `when`(embeddingModel.embed(any<TextSegment>()))
            .thenReturn(Response.from(Embedding.from(queryEmbedding)))
        `when`(
            incidentEmbeddingRepository.findSimilar(
                any<ByteArray>(),
                eq(0.7),
                eq(5)
            )
        ).thenReturn(similarIncidents)
        `when`(
            runbookEmbeddingRepository.findSimilar(
                any<ByteArray>(),
                eq(0.7),
                eq(2)
            )
        ).thenReturn(emptyList())

        // Act
        val result = retrievalService.retrieve(incident)

        // Assert
        assertTrue(result is Either.Right)
        val context = (result as Either.Right).value
        val match = context.similarIncidents[0]
        assertNotNull(match.score)
        assertNotNull(match.snippet)
        assertEquals(IncidentId(11L), match.id)
    }

    @Test
    fun `RetrievalMatch snippet truncated to 200 characters`() {
        // Arrange
        val incident = Incident(
            id = IncidentId(1L),
            source = "cloudwatch",
            title = "Test",
            description = "Test description",
            severity = Severity.HIGH,
            status = IncidentStatus.Open,
            createdAt = testTimestamp,
            updatedAt = testTimestamp
        )
        val queryEmbedding = createMockEmbedding()
        val longText = "A".repeat(300)
        val similarIncidents = listOf(
            SimilarIncidentResult(
                id = 1L,
                incidentId = 10L,
                text = longText,
                similarity = 0.9
            )
        )

        `when`(embeddingModel.embed(any<TextSegment>()))
            .thenReturn(Response.from(Embedding.from(queryEmbedding)))
        `when`(
            incidentEmbeddingRepository.findSimilar(
                any<ByteArray>(),
                eq(0.7),
                eq(5)
            )
        ).thenReturn(similarIncidents)
        `when`(
            runbookEmbeddingRepository.findSimilar(
                any<ByteArray>(),
                eq(0.7),
                eq(2)
            )
        ).thenReturn(emptyList())

        // Act
        val result = retrievalService.retrieve(incident)

        // Assert
        assertTrue(result is Either.Right)
        val context = (result as Either.Right).value
        val match = context.similarIncidents[0]
        assertTrue(match.snippet!!.length <= 200)
    }

    @Test
    fun `EmbeddingScore wraps similarity value correctly`() {
        // Arrange
        val incident = Incident(
            id = IncidentId(1L),
            source = "cloudwatch",
            title = "Test",
            description = "Test description",
            severity = Severity.HIGH,
            status = IncidentStatus.Open,
            createdAt = testTimestamp,
            updatedAt = testTimestamp
        )
        val queryEmbedding = createMockEmbedding()
        val similarity = 0.85
        val similarIncidents = listOf(
            SimilarIncidentResult(
                id = 1L,
                incidentId = 10L,
                text = "Test text",
                similarity = similarity
            )
        )

        `when`(embeddingModel.embed(any<TextSegment>()))
            .thenReturn(Response.from(Embedding.from(queryEmbedding)))
        `when`(
            incidentEmbeddingRepository.findSimilar(
                any<ByteArray>(),
                eq(0.7),
                eq(5)
            )
        ).thenReturn(similarIncidents)
        `when`(
            runbookEmbeddingRepository.findSimilar(
                any<ByteArray>(),
                eq(0.7),
                eq(2)
            )
        ).thenReturn(emptyList())

        // Act
        val result = retrievalService.retrieve(incident)

        // Assert
        assertTrue(result is Either.Right)
        val context = (result as Either.Right).value
        val match = context.similarIncidents[0]
        assertEquals(similarity, match.score.value, 0.001)
    }

    private fun createMockEmbedding(): FloatArray {
        // Create a 768-dimensional mock embedding (matching nomic-embed-text dimensions)
        return FloatArray(768) { it.toFloat() / 768f }
    }

    private fun createMockSimilarIncidents(count: Int): List<SimilarIncidentResult> {
        return (1..count).map { i ->
            SimilarIncidentResult(
                id = i.toLong(),
                incidentId = 10L + i,
                text = "Similar incident text $i",
                similarity = 0.9 - (i * 0.05)
            )
        }
    }

    private fun createMockSimilarRunbooks(count: Int): List<SimilarRunbookResult> {
        return (1..count).map { i ->
            SimilarRunbookResult(
                id = i.toLong(),
                fragmentId = 20L + i,
                text = "Similar runbook text $i",
                similarity = 0.85 - (i * 0.05)
            )
        }
    }
}
