package com.example.incidentanalyst.rag

import com.example.incidentanalyst.common.Either
import com.example.incidentanalyst.diagnosis.DiagnosisEntity
import com.example.incidentanalyst.diagnosis.DiagnosisId
import com.example.incidentanalyst.diagnosis.DiagnosisRepository
import com.example.incidentanalyst.incident.Incident
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
import dev.langchain4j.store.embedding.EmbeddingStore
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant

@QuarkusTest
class RetrievalServiceIntegrationTest {

    @Inject
    lateinit var embeddingService: EmbeddingService

    @Inject
    lateinit var retrievalService: RetrievalService

    @Inject
    lateinit var embeddingStore: EmbeddingStore<TextSegment>

    @Inject
    lateinit var incidentRepository: IncidentRepository

    @Inject
    lateinit var runbookFragmentRepository: RunbookFragmentRepository

    @Inject
    lateinit var diagnosisRepository: DiagnosisRepository

    @BeforeEach
    @Transactional
    fun setup() {
        diagnosisRepository.deleteAll()
        runbookFragmentRepository.deleteAll()
        incidentRepository.deleteAll()
    }

    @Test
    @Transactional
    fun `retrieve similar incidents using similarity`() {
        // Arrange - Create similar incidents
        val timestamp = Instant.now()
        val incident1 = Incident(
            id = IncidentId(0),
            source = "cloudwatch",
            title = "Database connection failed " + System.currentTimeMillis(),
            description = "Connection pool exhausted due to high load",
            severity = Severity.HIGH,
            status = IncidentStatus.Open,
            createdAt = timestamp,
            updatedAt = timestamp
        )
        
        val entity1 = incident1.toEntity()
        incidentRepository.persist(entity1)
        val id1 = IncidentId(requireNotNull(entity1.id))
        
        // Act
        embeddingService.embedIncident(id1)

        // Retrieve
        val queryIncident = Incident(
            id = IncidentId(0),
            source = "cloudwatch",
            title = incident1.title,
            description = incident1.description,
            severity = Severity.HIGH,
            status = IncidentStatus.Open,
            createdAt = timestamp,
            updatedAt = timestamp
        )

        val result = retrievalService.retrieve(queryIncident)

        // Assert
        assertTrue(result is Either.Right)
        val context = (result as Either.Right).value
        assertTrue(context.similarIncidents.isNotEmpty())
    }

    @Test
    @Transactional
    fun `retrieve similar runbooks using similarity`() {
        // Arrange - Create similar runbook fragments
        val timestamp = Instant.now()
        val fragment1 = RunbookFragmentEntity(
            id = null,
            title = "Database Connection Troubleshooting " + System.currentTimeMillis(),
            content = "Check connection pool settings and verify database credentials",
            tags = "database,troubleshooting",
            createdAt = timestamp
        )

        runbookFragmentRepository.persist(fragment1)
        val fragId1 = RunbookFragmentId(requireNotNull(fragment1.id))
        
        // Act
        embeddingService.embedRunbook(fragId1)

        // Retrieve
        val queryFragment = RunbookFragment(
            id = RunbookFragmentId(0),
            title = fragment1.title,
            content = fragment1.content,
            tags = null,
            createdAt = timestamp
        )

        val result = retrievalService.retrieveForRunbook(queryFragment)

        // Assert
        assertTrue(result is Either.Right)
        val context = (result as Either.Right).value
        assertTrue(context.similarRunbooks.isNotEmpty())
    }

    @Test
    @Transactional
    fun `verified diagnosis creates embedding and can be retrieved`() {
        // Arrange - Create incident and diagnosis
        val timestamp = Instant.now()
        val incident = Incident(
            id = IncidentId(0),
            source = "cloudwatch",
            title = "Database connection failed " + System.currentTimeMillis(),
            description = "Connection pool exhausted",
            severity = Severity.HIGH,
            status = IncidentStatus.Open,
            createdAt = timestamp,
            updatedAt = timestamp
        )
        val incidentEntity = incident.toEntity()
        incidentRepository.persist(incidentEntity)

        val diagnosisEntity = DiagnosisEntity(
            id = null,
            incident = incidentEntity,
            suggestedRootCause = "Connection leak in application",
            remediationSteps = "Step 1: Restart service\nStep 2: Monitor connections",
            confidence = "HIGH",
            verification = "VERIFIED",
            createdAt = timestamp,
            verifiedAt = timestamp,
            verifiedBy = "admin"
        )
        diagnosisRepository.persist(diagnosisEntity)
        val diagnosisId = DiagnosisId(requireNotNull(diagnosisEntity.id))

        // Create verified diagnosis embedding
        embeddingService.embedVerifiedDiagnosis(diagnosisId)

        // Act - Retrieve using a similar query
        val queryIncident = Incident(
            id = IncidentId(0),
            source = "test",
            title = incident.title,
            description = incident.description,
            severity = Severity.HIGH,
            status = IncidentStatus.Open,
            createdAt = timestamp,
            updatedAt = timestamp
        )

        val result = retrievalService.retrieve(queryIncident)

        // Assert
        assertTrue(result is Either.Right)
        val context = (result as Either.Right).value
        assertTrue(context.similarIncidents.isNotEmpty())

        // Verify the retrieved match has VERIFIED_DIAGNOSIS sourceType
        val verifiedMatch = context.similarIncidents.find { it.sourceType == SourceType.VERIFIED_DIAGNOSIS }
        assertNotNull(verifiedMatch, "Should find at least one VERIFIED_DIAGNOSIS embedding")
    }
}
