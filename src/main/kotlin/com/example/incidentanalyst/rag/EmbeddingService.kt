package com.example.incidentanalyst.rag

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
    private val runbookEmbeddingRepository: RunbookEmbeddingRepository
) {

    @Transactional
    fun embedIncident(incidentId: IncidentId): EmbeddingResult {
        val incident = incidentRepository.findById(incidentId.value)
            ?: return EmbeddingResult.Failure(EmbeddingError.Unexpected)

        if (incident.title.isBlank() && incident.description.isBlank()) {
            return EmbeddingResult.Failure(EmbeddingError.InvalidText)
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
            EmbeddingResult.Success(1)
        } catch (e: ConstraintViolationException) {
            EmbeddingResult.Failure(
                EmbeddingError.PersistenceError(e.message ?: "Constraint violation")
            )
        } catch (e: Exception) {
            EmbeddingResult.Failure(EmbeddingError.EmbeddingFailed)
        }
    }

    @Transactional
    fun embedRunbook(fragmentId: RunbookFragmentId): EmbeddingResult {
        val fragment = runbookFragmentRepository.findById(fragmentId.value)
            ?: return EmbeddingResult.Failure(EmbeddingError.Unexpected)

        if (fragment.title.isBlank() && fragment.content.isBlank()) {
            return EmbeddingResult.Failure(EmbeddingError.InvalidText)
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
            EmbeddingResult.Success(1)
        } catch (e: ConstraintViolationException) {
            EmbeddingResult.Failure(
                EmbeddingError.PersistenceError(e.message ?: "Constraint violation")
            )
        } catch (e: Exception) {
            EmbeddingResult.Failure(EmbeddingError.EmbeddingFailed)
        }
    }

    @Transactional
    fun embedBatch(
        incidentIds: List<IncidentId>,
        fragmentIds: List<RunbookFragmentId>
    ): EmbeddingResult {
        var count = 0

        incidentIds.forEach { id ->
            when (val result = embedIncident(id)) {
                is EmbeddingResult.Success -> count += result.count
                is EmbeddingResult.Failure -> {
                    if (result.error !is EmbeddingError.Unexpected) {
                        return result
                    }
                }
            }
        }

        fragmentIds.forEach { id ->
            when (val result = embedRunbook(id)) {
                is EmbeddingResult.Success -> count += result.count
                is EmbeddingResult.Failure -> {
                    if (result.error !is EmbeddingError.Unexpected) {
                        return result
                    }
                }
            }
        }

        return EmbeddingResult.Success(count)
    }
}

private fun floatArrayToByteArray(floatArray: FloatArray): ByteArray {
    val buffer = ByteBuffer.allocate(floatArray.size * java.lang.Float.BYTES)
    floatArray.forEach { buffer.putFloat(it) }
    return buffer.array()
}
