package com.example.incidentanalyst.rag

import com.example.incidentanalyst.common.Either
import com.example.incidentanalyst.incident.Incident
import com.example.incidentanalyst.incident.IncidentId
import com.example.incidentanalyst.incident.IncidentStatus
import com.example.incidentanalyst.incident.Severity
import com.example.incidentanalyst.runbook.RunbookFragment
import com.example.incidentanalyst.runbook.RunbookFragmentId
import dev.langchain4j.data.document.Metadata
import dev.langchain4j.data.segment.TextSegment
import dev.langchain4j.rag.content.Content
import dev.langchain4j.rag.content.retriever.ContentRetriever
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
class RetrievalServiceTest {

    @InjectMock
    lateinit var incidentRetriever: ContentRetriever

    @Inject
    lateinit var retrievalService: RetrievalService

    private val testTimestamp = Instant.now()

    @BeforeEach
    fun setup() {
        reset(incidentRetriever)
    }

    @Test
    fun `retrieve for incident success - returns similar incidents`() {
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
        
        val segment = TextSegment.from("Past incident", Metadata.from(mapOf("source_type" to SourceType.RAW_INCIDENT.name, "incident_id" to "10")))
        val content = Content.from(segment)

        whenever(incidentRetriever.retrieve(any())).thenReturn(listOf(content))

        // Act
        val result = retrievalService.retrieve(incident)

        // Assert
        assertTrue(result is Either.Right)
        val context = (result as Either.Right).value
        assertEquals(1, context.similarIncidents.size)
        assertEquals(IncidentId(10L), context.similarIncidents[0].id)
    }

    @Test
    fun `retrieve for incident model unavailable - returns failure`() {
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

        whenever(incidentRetriever.retrieve(any())).thenThrow(RuntimeException("Model unavailable"))

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
    fun `retrieve for runbook success - returns similar runbooks`() {
        // Arrange
        val fragment = RunbookFragment(
            id = RunbookFragmentId(1L),
            title = "Database connection troubleshooting",
            content = "Check connection pool settings",
            tags = "database,troubleshooting",
            createdAt = testTimestamp
        )
        
        val segment = TextSegment.from("Procedure text", Metadata.from(mapOf("source_type" to SourceType.OFFICIAL_RUNBOOK.name, "fragment_id" to "20")))
        val content = Content.from(segment)

        whenever(incidentRetriever.retrieve(any())).thenReturn(listOf(content))

        // Act
        val result = retrievalService.retrieveForRunbook(fragment)

        // Assert
        assertTrue(result is Either.Right)
        val context = (result as Either.Right).value
        assertEquals(1, context.similarRunbooks.size)
        assertEquals(RunbookFragmentId(20L), context.similarRunbooks[0].id)
    }

    @Test
    fun `retrieve handles multiple source types correctly`() {
        // Arrange
        val incident = Incident(
            id = IncidentId(1L),
            source = "test",
            title = "Test",
            description = "Test",
            severity = Severity.HIGH,
            status = IncidentStatus.Open,
            createdAt = testTimestamp,
            updatedAt = testTimestamp
        )
        
        val segment1 = TextSegment.from("Raw", Metadata.from(mapOf("source_type" to SourceType.RAW_INCIDENT.name, "incident_id" to "10")))
        val segment2 = TextSegment.from("Verified", Metadata.from(mapOf("source_type" to SourceType.VERIFIED_DIAGNOSIS.name, "incident_id" to "20")))
        val segment3 = TextSegment.from("Resolved", Metadata.from(mapOf("source_type" to SourceType.RESOLVED_INCIDENT.name, "incident_id" to "30")))
        val segment4 = TextSegment.from("Runbook", Metadata.from(mapOf("source_type" to SourceType.OFFICIAL_RUNBOOK.name, "fragment_id" to "40")))

        whenever(incidentRetriever.retrieve(any())).thenReturn(listOf(
            Content.from(segment1),
            Content.from(segment2),
            Content.from(segment3),
            Content.from(segment4)
        ))

        // Act
        val result = retrievalService.retrieve(incident)

        // Assert
        assertTrue(result is Either.Right)
        val context = (result as Either.Right).value
        assertEquals(3, context.similarIncidents.size)
        assertEquals(1, context.similarRunbooks.size)
        
        assertTrue(context.similarIncidents.any { it.id == IncidentId(10) })
        assertTrue(context.similarIncidents.any { it.id == IncidentId(20) })
        assertTrue(context.similarIncidents.any { it.id == IncidentId(30) })
        assertEquals(RunbookFragmentId(40), context.similarRunbooks[0].id)
    }
}
