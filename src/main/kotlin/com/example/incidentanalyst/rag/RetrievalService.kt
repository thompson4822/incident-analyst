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
import org.jboss.logging.Logger

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
    private val log = Logger.getLogger(javaClass)

    fun retrieve(incident: Incident): Either<RetrievalError, RetrievalContext> {
        log.infof("Retrieving context for incident: %s", incident.title)
        if (incident.title.isBlank() && incident.description.isBlank()) {
            return Either.Left(RetrievalError.InvalidQuery)
        }

        val query = buildQueryFromIncident(incident)
        return performRetrieval(query)
    }

    fun retrieveForRunbook(fragment: RunbookFragment): Either<RetrievalError, RetrievalContext> {
        log.infof("Retrieving context for runbook: %s", fragment.title)
        if (fragment.title.isBlank() && fragment.content.isBlank()) {
            return Either.Left(RetrievalError.InvalidQuery)
        }

        val query = buildQueryFromRunbook(fragment)
        return performRetrieval(query)
    }

    private fun performRetrieval(query: String): Either<RetrievalError, RetrievalContext> {
        log.infof("Performing retrieval with query: %s", query)
        if (query.isBlank()) {
            return Either.Left(RetrievalError.InvalidQuery)
        }

        val queryEmbedding = try {
            log.info("Generating embedding for query...")
            embeddingModel.embed(TextSegment.from(query)).content().vector()
        } catch (e: Exception) {
            log.error("Failed to generate embedding", e)
            return Either.Left(RetrievalError.ModelUnavailable)
        }

        val queryEmbeddingBytes = floatArrayToByteArray(queryEmbedding)

        val similarIncidents = try {
            log.info("Searching for similar incidents in DB...")
            incidentEmbeddingRepository.findSimilar(
                queryEmbedding = queryEmbeddingBytes,
                minScore = 0.6,
                limit = 15
            )
        } catch (e: Exception) {
            log.error("Database search for incidents failed", e)
            return Either.Left(RetrievalError.SearchFailed)
        }

        val similarRunbooks = try {
            log.info("Searching for similar runbooks in DB...")
            runbookEmbeddingRepository.findSimilar(
                queryEmbedding = queryEmbeddingBytes,
                minScore = 0.6,
                limit = 5
            )
        } catch (e: Exception) {
            log.error("Database search for runbooks failed", e)
            return Either.Left(RetrievalError.SearchFailed)
        }

        log.infof("Found %d similar incidents and %d similar runbooks", similarIncidents.size, similarRunbooks.size)

        val context = RetrievalContext(
            similarIncidents = similarIncidents.map {
                val sourceType = try { SourceType.valueOf(it.sourceType) } catch (e: Exception) { SourceType.RAW_INCIDENT }
                val boost = when (sourceType) {
                    SourceType.RESOLVED_INCIDENT -> 1.2
                    SourceType.VERIFIED_DIAGNOSIS -> 1.1
                    SourceType.RAW_INCIDENT -> 1.0
                    SourceType.OFFICIAL_RUNBOOK -> 1.0
                }
                RetrievalMatch(
                    id = IncidentId(it.incidentId),
                    score = EmbeddingScore(it.similarity * boost),
                    snippet = it.text?.take(400),
                    sourceType = sourceType
                )
            }.sortedByDescending { it.score.value }.take(5),
            similarRunbooks = similarRunbooks.map {
                RetrievalMatch(
                    id = RunbookFragmentId(it.fragmentId),
                    score = EmbeddingScore(it.similarity),
                    snippet = it.text?.take(400),
                    sourceType = SourceType.OFFICIAL_RUNBOOK
                )
            }.sortedByDescending { it.score.value }.take(3),
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
