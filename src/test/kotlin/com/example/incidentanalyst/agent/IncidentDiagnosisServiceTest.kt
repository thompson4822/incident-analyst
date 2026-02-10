package com.example.incidentanalyst.agent

import com.example.incidentanalyst.common.Either
import com.example.incidentanalyst.config.ApplicationProfile
import com.example.incidentanalyst.config.ProfileService
import com.example.incidentanalyst.diagnosis.DiagnosisEntity
import com.example.incidentanalyst.diagnosis.DiagnosisRepository
import com.example.incidentanalyst.diagnosis.DiagnosisSuccess
import com.example.incidentanalyst.incident.IncidentEntity
import com.example.incidentanalyst.incident.IncidentId
import com.example.incidentanalyst.incident.IncidentRepository
import com.example.incidentanalyst.rag.RetrievalContext
import com.example.incidentanalyst.rag.RetrievalService
import com.fasterxml.jackson.databind.ObjectMapper
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
class IncidentDiagnosisServiceTest {

    @InjectMock
    lateinit var incidentRepository: IncidentRepository

    @InjectMock
    lateinit var retrievalService: RetrievalService

    @InjectMock
    lateinit var aiService: IncidentAnalystAgent

    @InjectMock
    lateinit var diagnosisRepository: DiagnosisRepository

    @InjectMock
    lateinit var profileService: ProfileService

    @Inject
    lateinit var incidentDiagnosisService: IncidentDiagnosisService

    @Inject
    lateinit var objectMapper: ObjectMapper

    private val testTimestamp = Instant.now()

    @BeforeEach
    fun setup() {
        reset(incidentRepository, retrievalService, aiService, diagnosisRepository, profileService)
        
        whenever(profileService.getProfile()).thenReturn(
            ApplicationProfile("Test App", listOf("Kotlin"), listOf("DB"), "us-east-1")
        )
    }

    @Test
    fun `diagnose returns existing diagnosis if found`() {
        // Arrange
        val incidentId = IncidentId(123L)
        val incidentEntity = IncidentEntity(
            id = 123L, 
            title = "Test",
            source = "test",
            description = "desc",
            severity = "HIGH",
            status = "OPEN",
            createdAt = testTimestamp,
            updatedAt = testTimestamp
        )
        val diagnosisEntity = DiagnosisEntity(
            id = 456L, 
            incident = incidentEntity,
            suggestedRootCause = "Cause",
            remediationSteps = "Steps",
            confidence = "HIGH",
            verification = "UNVERIFIED",
            createdAt = testTimestamp
        )

        whenever(incidentRepository.findById(123L)).thenReturn(incidentEntity)
        whenever(diagnosisRepository.findByIncidentId(123L)).thenReturn(diagnosisEntity)

        // Act
        val result = incidentDiagnosisService.diagnose(incidentId)

        // Assert
        assertTrue(result is Either.Right)
        val success = (result as Either.Right).value
        assertTrue(success is DiagnosisSuccess.ExistingDiagnosisFound)
        assertEquals(456L, (success as DiagnosisSuccess.ExistingDiagnosisFound).diagnosis.id.value)
        
        verifyNoInteractions(retrievalService)
        verifyNoInteractions(aiService)
    }

    @Test
    fun `diagnose performs full RAG flow if no existing diagnosis`() {
        // Arrange
        val incidentId = IncidentId(123L)
        val incidentEntity = IncidentEntity(
            id = 123L, 
            title = "High CPU", 
            source = "test",
            description = "CPU at 99%",
            severity = "HIGH",
            status = "OPEN",
            createdAt = testTimestamp,
            updatedAt = testTimestamp
        )
        val retrievalContext = RetrievalContext(emptyList(), emptyList(), "query")
        val llmResponse = """
            {
              "rootCause": "Memory leak",
              "steps": ["Restart", "Monitor"],
              "confidence": "HIGH"
            }
        """.trimIndent()

        whenever(incidentRepository.findById(123L)).thenReturn(incidentEntity)
        whenever(diagnosisRepository.findByIncidentId(123L)).thenReturn(null)
        whenever(retrievalService.retrieve(any())).thenReturn(Either.Right(retrievalContext))
        whenever(aiService.proposeDiagnosis(any(), any(), any(), any(), any())).thenReturn(llmResponse)
        whenever(diagnosisRepository.persist(any<DiagnosisEntity>())).thenAnswer {
            (it.arguments[0] as DiagnosisEntity).id = 999L
            Unit
        }

        // Act
        val result = incidentDiagnosisService.diagnose(incidentId)

        // Assert
        assertTrue(result is Either.Right)
        val success = (result as Either.Right).value
        assertTrue(success is DiagnosisSuccess.NewDiagnosisGenerated)
        
        verify(retrievalService).retrieve(any())
        verify(aiService).proposeDiagnosis(
            appName = eq("Test App"),
            appStack = eq("Kotlin"),
            appComponents = eq("DB"),
            incident = argThat { contains("High CPU") },
            context = any()
        )
        verify(diagnosisRepository).persist(any<DiagnosisEntity>())
        assertEquals("DIAGNOSED:999", incidentEntity.status)
    }

    @Test
    fun `diagnose handles LLM response with markdown blocks`() {
        // Arrange
        val incidentId = IncidentId(123L)
        val incidentEntity = IncidentEntity(
            id = 123L, 
            title = "Test", 
            source = "test",
            description = "desc",
            severity = "HIGH",
            status = "OPEN",
            createdAt = testTimestamp,
            updatedAt = testTimestamp
        )
        val retrievalContext = RetrievalContext(emptyList(), emptyList(), "query")
        val llmResponse = """
            ```json
            {
              "rootCause": "Markdown cause",
              "steps": ["Step 1"],
              "confidence": "MEDIUM"
            }
            ```
        """.trimIndent()

        whenever(incidentRepository.findById(123L)).thenReturn(incidentEntity)
        whenever(diagnosisRepository.findByIncidentId(123L)).thenReturn(null)
        whenever(retrievalService.retrieve(any())).thenReturn(Either.Right(retrievalContext))
        whenever(aiService.proposeDiagnosis(any(), any(), any(), any(), any())).thenReturn(llmResponse)
        whenever(diagnosisRepository.persist(any<DiagnosisEntity>())).thenAnswer {
            (it.arguments[0] as DiagnosisEntity).id = 888L
            Unit
        }

        // Act
        val result = incidentDiagnosisService.diagnose(incidentId)

        // Assert
        assertTrue(result is Either.Right)
        val success = (result as Either.Right).value
        assertEquals("Markdown cause", (success as DiagnosisSuccess.NewDiagnosisGenerated).diagnosis.rootCause)
    }
}
