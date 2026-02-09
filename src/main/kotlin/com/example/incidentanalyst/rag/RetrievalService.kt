package com.example.incidentanalyst.rag

import com.example.incidentanalyst.common.Either
import com.example.incidentanalyst.incident.Incident
import com.example.incidentanalyst.incident.IncidentId
import com.example.incidentanalyst.runbook.RunbookFragment
import com.example.incidentanalyst.runbook.RunbookFragmentId
import dev.langchain4j.data.segment.TextSegment
import dev.langchain4j.model.embedding.EmbeddingModel
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import java.nio.ByteBuffer

private fun floatArrayToByteArray(floatArray: FloatArray): ByteArray {
    val buffer = ByteBuffer.allocate(floatArray.size * java.lang.Float.BYTES)
    floatArray.forEach { buffer.putFloat(it) }
    return buffer.array()
}

@ApplicationScoped
class RetrievalService @Inject constructor(
    private val embeddingModel: EmbeddingModel,
    private val incidentEmbeddingRepository: IncidentEmbeddingRepository,
    private val runbookEmbeddingRepository: RunbookEmbeddingRepository
) {

    fun retrieve(incident: Incident): Either<RetrievalError, RetrievalContext> {
        if (incident.title.isBlank() && incident.description.isBlank()) {
            return Either.Left(RetrievalError.InvalidQuery)
        }

        val query = buildQueryFromIncident(incident)

        if (query.isBlank()) {
            return Either.Left(RetrievalError.InvalidQuery)
        }

        val queryEmbedding = try {
            embeddingModel.embed(TextSegment.from(query)).content().vector()
        } catch (e: Exception) {
            return Either.Left(RetrievalError.ModelUnavailable)
        }

        val queryEmbeddingBytes = floatArrayToByteArray(queryEmbedding)

        val similarIncidents = try {
            incidentEmbeddingRepository.findSimilar(
                queryEmbedding = queryEmbeddingBytes,
                minScore = 0.7,
                limit = 5
            )
        } catch (e: Exception) {
            return Either.Left(RetrievalError.SearchFailed)
        }

        val similarRunbooks = try {
            runbookEmbeddingRepository.findSimilar(
                queryEmbedding = queryEmbeddingBytes,
                minScore = 0.7,
                limit = 2
            )
        } catch (e: Exception) {
            return Either.Left(RetrievalError.SearchFailed)
        }

        val context = RetrievalContext(
            similarIncidents = similarIncidents.map {
                RetrievalMatch(
                    id = IncidentId(it.incidentId),
                    score = EmbeddingScore(it.similarity),
                    snippet = it.text?.take(200)
                )
            },
            similarRunbooks = similarRunbooks.map {
                RetrievalMatch(
                    id = RunbookFragmentId(it.fragmentId),
                    score = EmbeddingScore(it.similarity),
                    snippet = it.text?.take(200)
                )
            },
            query = query
        )

        return Either.Right(context)
    }

    fun retrieveForRunbook(fragment: RunbookFragment): Either<RetrievalError, RetrievalContext> {
        if (fragment.title.isBlank() && fragment.content.isBlank()) {
            return Either.Left(RetrievalError.InvalidQuery)
        }

        val query = buildQueryFromRunbook(fragment)

        if (query.isBlank()) {
            return Either.Left(RetrievalError.InvalidQuery)
        }

        val queryEmbedding = try {
            embeddingModel.embed(TextSegment.from(query)).content().vector()
        } catch (e: Exception) {
            return Either.Left(RetrievalError.ModelUnavailable)
        }

        val queryEmbeddingBytes = floatArrayToByteArray(queryEmbedding)

        val similarIncidents = try {
            incidentEmbeddingRepository.findSimilar(
                queryEmbedding = queryEmbeddingBytes,
                minScore = 0.7,
                limit = 5
            )
        } catch (e: Exception) {
            return Either.Left(RetrievalError.SearchFailed)
        }

        val similarRunbooks = try {
            runbookEmbeddingRepository.findSimilar(
                queryEmbedding = queryEmbeddingBytes,
                minScore = 0.7,
                limit = 2
            )
        } catch (e: Exception) {
            return Either.Left(RetrievalError.SearchFailed)
        }

        val context = RetrievalContext(
            similarIncidents = similarIncidents.map {
                RetrievalMatch(
                    id = IncidentId(it.incidentId),
                    score = EmbeddingScore(it.similarity),
                    snippet = it.text?.take(200)
                )
            },
            similarRunbooks = similarRunbooks.map {
                RetrievalMatch(
                    id = RunbookFragmentId(it.fragmentId),
                    score = EmbeddingScore(it.similarity),
                    snippet = it.text?.take(200)
                )
            },
            query = query
        )

        return Either.Right(context)
    }

    private fun buildQueryFromIncident(incident: Incident): String = """
        Incident: ${incident.title}
        Description: ${incident.description}
        Severity: ${incident.severity}
        Status: ${incident.status}
        """.trimIndent().trim()

    private fun buildQueryFromRunbook(fragment: RunbookFragment): String = """
        Runbook: ${fragment.title}
        Content: ${fragment.content}
        ${if (!fragment.tags.isNullOrBlank()) "Tags: ${fragment.tags}" else ""}
        """.trimIndent().trim()
}
