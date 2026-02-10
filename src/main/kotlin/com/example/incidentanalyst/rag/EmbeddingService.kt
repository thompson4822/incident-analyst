package com.example.incidentanalyst.rag

import com.example.incidentanalyst.common.Either
import com.example.incidentanalyst.diagnosis.DiagnosisEntity
import com.example.incidentanalyst.diagnosis.DiagnosisId
import com.example.incidentanalyst.diagnosis.DiagnosisRepository
import com.example.incidentanalyst.incident.IncidentId
import com.example.incidentanalyst.incident.IncidentRepository
import com.example.incidentanalyst.runbook.RunbookFragmentId
import com.example.incidentanalyst.runbook.RunbookFragmentRepository
import dev.langchain4j.data.segment.TextSegment
import dev.langchain4j.model.embedding.EmbeddingModel
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import org.hibernate.exception.ConstraintViolationException
import java.nio.ByteBuffer

@ApplicationScoped
class EmbeddingService(
    private val embeddingModel: EmbeddingModel,
    private val incidentRepository: IncidentRepository,
    private val incidentEmbeddingRepository: IncidentEmbeddingRepository,
    private val runbookFragmentRepository: RunbookFragmentRepository,
    private val runbookEmbeddingRepository: RunbookEmbeddingRepository,
    private val diagnosisRepository: DiagnosisRepository
) {

    @Transactional
    fun embedIncident(incidentId: IncidentId): Either<EmbeddingError, Int> {
        val incident = incidentRepository.findById(incidentId.value)
            ?: return Either.Left(EmbeddingError.Unexpected)

        if (incident.title.isBlank() && incident.description.isBlank()) {
            return Either.Left(EmbeddingError.InvalidText)
        }

        val text = buildString {
            append("Title: ").append(incident.title).append("\n")
            append("Description: ").append(incident.description)
        }.trim()

        return try {
            val embedding = embeddingModel.embed(TextSegment.from(text)).content().vector()
            val embeddingBytes = floatArrayToByteArray(embedding)

            val entity = IncidentEmbeddingEntity(
                incident = incident,
                text = text,
                embedding = embeddingBytes
            )

            incidentEmbeddingRepository.persist(entity)
            Either.Right(1)
        } catch (e: ConstraintViolationException) {
            Either.Left(
                EmbeddingError.PersistenceError(e.message ?: "Constraint violation")
            )
        } catch (e: Exception) {
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

        val text = buildString {
            append("Title: ").append(fragment.title).append("\n")
            append("Content: ").append(fragment.content)
        }.trim()

        return try {
            val embedding = embeddingModel.embed(TextSegment.from(text)).content().vector()
            val embeddingBytes = floatArrayToByteArray(embedding)

            val entity = RunbookEmbeddingEntity(
                fragment = fragment,
                text = text,
                embedding = embeddingBytes
            )

            runbookEmbeddingRepository.persist(entity)
            Either.Right(1)
        } catch (e: ConstraintViolationException) {
            Either.Left(
                EmbeddingError.PersistenceError(e.message ?: "Constraint violation")
            )
        } catch (e: Exception) {
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
            embedIncident(id).fold(
                ifLeft = { error ->
                    if (error !is EmbeddingError.Unexpected) {
                        return Either.Left(error)
                    }
                },
                ifRight = { count += it }
            )
        }

        fragmentIds.forEach { id ->
            embedRunbook(id).fold(
                ifLeft = { error ->
                    if (error !is EmbeddingError.Unexpected) {
                        return Either.Left(error)
                    }
                },
                ifRight = { count += it }
            )
        }

        return Either.Right(count)
    }

    @Transactional
    fun embedVerifiedDiagnosis(diagnosisId: DiagnosisId): Either<EmbeddingError, Int> {
        val diagnosis = diagnosisRepository.findById(diagnosisId.value)
            ?: return Either.Left(EmbeddingError.Unexpected)

        val incident = diagnosis.incident
            ?: return Either.Left(EmbeddingError.Unexpected)

        val text = buildString {
            append("Title: ").append(incident.title).append("\n")
            append("Description: ").append(incident.description).append("\n")
            append("Root Cause: ").append(diagnosis.suggestedRootCause).append("\n")
            append("Remediation Steps: ").append(diagnosis.remediationSteps)
        }.trim()

        return try {
            val embedding = embeddingModel.embed(TextSegment.from(text)).content().vector()
            val embeddingBytes = floatArrayToByteArray(embedding)

            val entity = IncidentEmbeddingEntity(
                incident = incident,
                text = text,
                embedding = embeddingBytes,
                sourceType = "VERIFIED_DIAGNOSIS",
                diagnosisId = diagnosis.id
            )

            incidentEmbeddingRepository.persist(entity)
            Either.Right(1)
        } catch (e: ConstraintViolationException) {
            Either.Left(
                EmbeddingError.PersistenceError(e.message ?: "Constraint violation")
            )
        } catch (e: Exception) {
            Either.Left(EmbeddingError.EmbeddingFailed)
        }
    }

    @Transactional
    fun embedResolution(incidentId: IncidentId): Either<EmbeddingError, Int> {
        val incident = incidentRepository.findById(incidentId.value)
            ?: return Either.Left(EmbeddingError.Unexpected)

        val resolutionText = incident.resolutionText
            ?: return Either.Left(EmbeddingError.InvalidText)

        val text = buildString {
            append("Title: ").append(incident.title).append("\n")
            append("Description: ").append(incident.description).append("\n")
            append("Resolution: ").append(resolutionText)
        }.trim()

        return try {
            val embedding = embeddingModel.embed(TextSegment.from(text)).content().vector()
            val embeddingBytes = floatArrayToByteArray(embedding)

            val entity = IncidentEmbeddingEntity(
                incident = incident,
                text = text,
                embedding = embeddingBytes,
                sourceType = "RESOLVED_INCIDENT"
            )

            incidentEmbeddingRepository.persist(entity)
            Either.Right(1)
        } catch (e: ConstraintViolationException) {
            Either.Left(
                EmbeddingError.PersistenceError(e.message ?: "Constraint violation")
            )
        } catch (e: Exception) {
            Either.Left(EmbeddingError.EmbeddingFailed)
        }
    }
}

private fun floatArrayToByteArray(floatArray: FloatArray): ByteArray {
    val buffer = ByteBuffer.allocate(floatArray.size * java.lang.Float.BYTES)
    floatArray.forEach { buffer.putFloat(it) }
    return buffer.array()
}
