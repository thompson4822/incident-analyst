package com.example.incidentanalyst.rag

import com.example.incidentanalyst.common.Either
import com.example.incidentanalyst.common.onRight
import com.example.incidentanalyst.diagnosis.DiagnosisId
import com.example.incidentanalyst.diagnosis.DiagnosisRepository
import com.example.incidentanalyst.incident.IncidentId
import com.example.incidentanalyst.incident.IncidentRepository
import com.example.incidentanalyst.runbook.RunbookFragmentId
import com.example.incidentanalyst.runbook.RunbookFragmentRepository
import dev.langchain4j.data.document.Metadata
import dev.langchain4j.data.segment.TextSegment
import dev.langchain4j.model.embedding.EmbeddingModel
import dev.langchain4j.store.embedding.EmbeddingStore
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import org.jboss.logging.Logger

@ApplicationScoped
class EmbeddingService(
    private val embeddingModel: EmbeddingModel,
    private val embeddingStore: EmbeddingStore<TextSegment>,
    private val incidentRepository: IncidentRepository,
    private val runbookFragmentRepository: RunbookFragmentRepository,
    private val diagnosisRepository: DiagnosisRepository
) {
    private val log = Logger.getLogger(javaClass)

    @Transactional
    fun embedIncident(incidentId: IncidentId): Either<EmbeddingError, Int> {
        val incident = incidentRepository.findById(incidentId.value)
            ?: return Either.Left(EmbeddingError.Unexpected)

        if (incident.title.isBlank() && incident.description.isBlank()) {
            return Either.Left(EmbeddingError.InvalidText)
        }

        val text = "Title: ${incident.title}\nDescription: ${incident.description}"
        val metadata = Metadata.from(mapOf(
            "source_type" to SourceType.RAW_INCIDENT.name,
            "incident_id" to incident.id.toString()
        ))

        return try {
            val segment = TextSegment.from(text, metadata)
            val embedding = embeddingModel.embed(segment).content()
            embeddingStore.add(embedding, segment)
            Either.Right(1)
        } catch (e: Exception) {
            log.error("Failed to embed incident", e)
            Either.Left(EmbeddingError.EmbeddingFailed)
        }
    }

    @Transactional
    fun embedRunbook(fragmentId: RunbookFragmentId): Either<EmbeddingError, Int> {
        val fragment = runbookFragmentRepository.findById(fragmentId.value)
            ?: return Either.Left(EmbeddingError.Unexpected)

        if (fragment.title.isBlank() && fragment.content.isBlank()) {
            return Either.Left(EmbeddingError.InvalidText)
        }

        val text = "Title: ${fragment.title}\nContent: ${fragment.content}"
        val metadata = Metadata.from(mapOf(
            "source_type" to SourceType.OFFICIAL_RUNBOOK.name,
            "fragment_id" to fragment.id.toString()
        ))

        return try {
            val segment = TextSegment.from(text, metadata)
            val embedding = embeddingModel.embed(segment).content()
            embeddingStore.add(embedding, segment)
            Either.Right(1)
        } catch (e: Exception) {
            log.error("Failed to embed runbook", e)
            Either.Left(EmbeddingError.EmbeddingFailed)
        }
    }

    @Transactional
    fun embedBatch(
        incidentIds: List<IncidentId>,
        fragmentIds: List<RunbookFragmentId>
    ): Either<EmbeddingError, Int> {
        var count = 0
        incidentIds.forEach { id ->
            embedIncident(id).onRight { count += it }
        }
        fragmentIds.forEach { id ->
            embedRunbook(id).onRight { count += it }
        }
        return Either.Right(count)
    }

    @Transactional
    fun embedVerifiedDiagnosis(diagnosisId: DiagnosisId): Either<EmbeddingError, Int> {
        val diagnosis = diagnosisRepository.findById(diagnosisId.value)
            ?: return Either.Left(EmbeddingError.Unexpected)

        val incident = diagnosis.incident
            ?: return Either.Left(EmbeddingError.Unexpected)

        val text = """
            Title: ${incident.title}
            Description: ${incident.description}
            Root Cause: ${diagnosis.suggestedRootCause}
            Remediation Steps: ${diagnosis.remediationSteps}
        """.trimIndent()

        val metadata = Metadata.from(mapOf(
            "source_type" to SourceType.VERIFIED_DIAGNOSIS.name,
            "incident_id" to incident.id.toString(),
            "diagnosis_id" to diagnosis.id.toString()
        ))

        return try {
            val segment = TextSegment.from(text, metadata)
            val embedding = embeddingModel.embed(segment).content()
            embeddingStore.add(embedding, segment)
            Either.Right(1)
        } catch (e: Exception) {
            log.error("Failed to embed verified diagnosis", e)
            Either.Left(EmbeddingError.EmbeddingFailed)
        }
    }

    @Transactional
    fun embedResolution(incidentId: IncidentId): Either<EmbeddingError, Int> {
        val incident = incidentRepository.findById(incidentId.value)
            ?: return Either.Left(EmbeddingError.Unexpected)

        val resolutionText = incident.resolutionText
            ?: return Either.Left(EmbeddingError.InvalidText)

        val text = """
            Title: ${incident.title}
            Description: ${incident.description}
            Resolution: $resolutionText
        """.trimIndent()

        val metadata = Metadata.from(mapOf(
            "source_type" to SourceType.RESOLVED_INCIDENT.name,
            "incident_id" to incident.id.toString()
        ))

        return try {
            val segment = TextSegment.from(text, metadata)
            val embedding = embeddingModel.embed(segment).content()
            embeddingStore.add(embedding, segment)
            Either.Right(1)
        } catch (e: Exception) {
            log.error("Failed to embed resolution", e)
            Either.Left(EmbeddingError.EmbeddingFailed)
        }
    }
}
