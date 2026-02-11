package com.example.incidentanalyst.rag

import com.example.incidentanalyst.common.Either
import com.example.incidentanalyst.diagnosis.DiagnosisEntity
import com.example.incidentanalyst.diagnosis.DiagnosisId
import com.example.incidentanalyst.incident.IncidentEntity
import com.example.incidentanalyst.incident.IncidentId
import com.example.incidentanalyst.incident.IncidentRepository
import com.example.incidentanalyst.runbook.RunbookFragmentEntity
import com.example.incidentanalyst.runbook.RunbookFragmentId
import com.example.incidentanalyst.runbook.RunbookFragmentRepository
import dev.langchain4j.data.segment.TextSegment
import dev.langchain4j.model.embedding.EmbeddingModel
import dev.langchain4j.model.output.Response
import dev.langchain4j.store.embedding.EmbeddingStore
import io.quarkus.test.InjectMock
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import java.time.Instant

@QuarkusTest
class EmbeddingServiceTest {

    @InjectMock
    lateinit var embeddingModel: EmbeddingModel

    @InjectMock
    lateinit var embeddingStore: EmbeddingStore<TextSegment>

    @InjectMock
    lateinit var incidentRepository: IncidentRepository

    @InjectMock
    lateinit var runbookFragmentRepository: RunbookFragmentRepository

    @InjectMock
    lateinit var diagnosisRepository: com.example.incidentanalyst.diagnosis.DiagnosisRepository

    @Inject
    lateinit var embeddingService: EmbeddingService

    private val testTimestamp = Instant.now()

    @BeforeEach
    fun setup() {
        reset(
            embeddingModel,
            embeddingStore,
            incidentRepository,
            runbookFragmentRepository,
            diagnosisRepository
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
        val mockEmbedding = dev.langchain4j.data.embedding.Embedding.from(FloatArray(768))

        whenever(incidentRepository.findById(incidentId.value)).thenReturn(incidentEntity)
        whenever(embeddingModel.embed(any<TextSegment>()))
            .thenReturn(Response.from(mockEmbedding))

        // Act
        val result = embeddingService.embedIncident(incidentId)

        // Assert
        assertTrue(result is Either.Right)
        assertEquals(1, (result as Either.Right).value)
        verify(embeddingStore).add(eq(mockEmbedding), any<TextSegment>())
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

        whenever(incidentRepository.findById(incidentId.value)).thenReturn(incidentEntity)
        whenever(embeddingModel.embed(any<TextSegment>()))
            .thenThrow(RuntimeException("Model unavailable"))

        // Act
        val result = embeddingService.embedIncident(incidentId)

        // Assert
        assertTrue(result is Either.Left)
        val error = (result as Either.Left).value
        assertTrue(error is EmbeddingError.EmbeddingFailed)
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

        whenever(incidentRepository.findById(incidentId.value)).thenReturn(incidentEntity)

        // Act
        val result = embeddingService.embedIncident(incidentId)

        // Assert
        assertTrue(result is Either.Left)
        val error = (result as Either.Left).value
        assertTrue(error is EmbeddingError.InvalidText)
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
        val mockEmbedding = dev.langchain4j.data.embedding.Embedding.from(FloatArray(768))

        whenever(runbookFragmentRepository.findById(fragmentId.value)).thenReturn(fragmentEntity)
        whenever(embeddingModel.embed(any<TextSegment>()))
            .thenReturn(Response.from(mockEmbedding))

        // Act
        val result = embeddingService.embedRunbook(fragmentId)

        // Assert
        assertTrue(result is Either.Right)
        assertEquals(1, (result as Either.Right).value)
        verify(embeddingStore).add(eq(mockEmbedding), any<TextSegment>())
    }

    @Test
    fun `embedVerifiedDiagnosis success - embedding generated and persisted`() {
        // Arrange
        val diagnosisId = DiagnosisId(123L)
        val baseTime = Instant.now()
        val incidentEntity = IncidentEntity(
            id = 1L,
            source = "cloudwatch",
            title = "High CPU Usage",
            description = "CPU usage exceeded 90% threshold",
            severity = "HIGH",
            status = "OPEN",
            createdAt = baseTime,
            updatedAt = baseTime
        )
        val diagnosisEntity = DiagnosisEntity(
            id = diagnosisId.value,
            incident = incidentEntity,
            suggestedRootCause = "Memory leak in application",
            remediationSteps = "Step 1: Restart service\nStep 2: Monitor memory usage",
            confidence = "HIGH",
            verification = "VERIFIED",
            createdAt = baseTime
        )
        val mockEmbedding = dev.langchain4j.data.embedding.Embedding.from(FloatArray(768))

        whenever(diagnosisRepository.findById(diagnosisId.value)).thenReturn(diagnosisEntity)
        whenever(embeddingModel.embed(any<TextSegment>()))
            .thenReturn(Response.from(mockEmbedding))

        // Act
        val result = embeddingService.embedVerifiedDiagnosis(diagnosisId)

        // Assert
        assertTrue(result is Either.Right)
        assertEquals(1, (result as Either.Right).value)
        
        val captor = argumentCaptor<TextSegment>()
        verify(embeddingStore).add(eq(mockEmbedding), captor.capture())
        
        val segment = captor.firstValue
        assertEquals(SourceType.VERIFIED_DIAGNOSIS.name, segment.metadata().getString("source_type"))
        assertEquals("1", segment.metadata().getString("incident_id"))
    }

    @Test
    fun `embedResolution success - embedding generated and persisted`() {
        // Arrange
        val testId = 456L
        val resolutionText = "Fixed via restart"
        val incidentEntity = IncidentEntity(
            id = testId,
            source = "test",
            title = "Test",
            description = "Test",
            severity = "HIGH",
            status = "RESOLVED",
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
            resolutionText = resolutionText
        )
        val mockEmbedding = dev.langchain4j.data.embedding.Embedding.from(FloatArray(768))

        whenever(incidentRepository.findById(testId)).thenReturn(incidentEntity)
        whenever(embeddingModel.embed(any<TextSegment>()))
            .thenReturn(Response.from(mockEmbedding))

        // Act
        val result = embeddingService.embedResolution(IncidentId(testId))

        // Assert
        assertTrue(result is Either.Right)
        assertEquals(1, (result as Either.Right).value)
        
        val captor = argumentCaptor<TextSegment>()
        verify(embeddingStore).add(eq(mockEmbedding), captor.capture())
        
        val segment = captor.firstValue
        assertEquals(SourceType.RESOLVED_INCIDENT.name, segment.metadata().getString("source_type"))
        assertTrue(segment.text().contains("Resolution: Fixed via restart"))
    }

    @Test
    fun `embedBatch handles partial failures correctly`() {
        // Arrange
        val incidentId1 = IncidentId(1L)
        val incidentId2 = IncidentId(2L)
        
        val incidentEntity1 = IncidentEntity(id = 1L, title = "T1", description = "D1", source = "S1", severity = "HIGH", status = "OPEN", createdAt = testTimestamp, updatedAt = testTimestamp)
        
        whenever(incidentRepository.findById(1L)).thenReturn(incidentEntity1)
        whenever(incidentRepository.findById(2L)).thenReturn(null) // Failure
        
        val mockEmbedding = dev.langchain4j.data.embedding.Embedding.from(FloatArray(768))
        whenever(embeddingModel.embed(any<TextSegment>())).thenReturn(Response.from(mockEmbedding))

        // Act
        val result = embeddingService.embedBatch(listOf(incidentId1, incidentId2), emptyList())

        // Assert
        assertTrue(result is Either.Right)
        assertEquals(1, (result as Either.Right).value)
        verify(embeddingStore, times(1)).add(any<dev.langchain4j.data.embedding.Embedding>(), any<TextSegment>())
    }
}
