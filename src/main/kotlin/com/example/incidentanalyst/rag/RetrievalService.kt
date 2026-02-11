package com.example.incidentanalyst.rag

import com.example.incidentanalyst.common.Either
import com.example.incidentanalyst.incident.Incident
import com.example.incidentanalyst.incident.IncidentId
import com.example.incidentanalyst.runbook.RunbookFragment
import com.example.incidentanalyst.runbook.RunbookFragmentId
import dev.langchain4j.rag.content.retriever.ContentRetriever
import dev.langchain4j.rag.query.Query
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.jboss.logging.Logger

@ApplicationScoped
class RetrievalService @Inject constructor(
    private val incidentRetriever: ContentRetriever
) {
    private val log = Logger.getLogger(javaClass)

    fun retrieve(incident: Incident): Either<RetrievalError, RetrievalContext> {
        log.infof("Retrieving context for incident: %s", incident.title)
        if (incident.title.isBlank() && incident.description.isBlank()) {
            return Either.Left(RetrievalError.InvalidQuery)
        }

        val queryText = buildQueryFromIncident(incident)
        return performRetrieval(queryText)
    }

    fun retrieveForRunbook(fragment: RunbookFragment): Either<RetrievalError, RetrievalContext> {
        log.infof("Retrieving context for runbook: %s", fragment.title)
        if (fragment.title.isBlank() && fragment.content.isBlank()) {
            return Either.Left(RetrievalError.InvalidQuery)
        }

        val queryText = buildQueryFromRunbook(fragment)
        return performRetrieval(queryText)
    }

    private fun performRetrieval(queryText: String): Either<RetrievalError, RetrievalContext> {
        return try {
            val query = Query.from(queryText)
            val contents = incidentRetriever.retrieve(query)

            val similarIncidents = mutableListOf<RetrievalMatch<IncidentId>>()
            val similarRunbooks = mutableListOf<RetrievalMatch<RunbookFragmentId>>()

            contents.forEach { content ->
                val segment = content.textSegment()
                val sourceType = try { 
                    SourceType.valueOf(segment.metadata().getString("source_type") ?: SourceType.RAW_INCIDENT.name) 
                } catch (e: Exception) { 
                    SourceType.RAW_INCIDENT 
                }

                if (sourceType == SourceType.OFFICIAL_RUNBOOK) {
                    val id = segment.metadata().getString("fragment_id")?.toLongOrNull() ?: 0L
                    similarRunbooks.add(RetrievalMatch(
                        id = RunbookFragmentId(id),
                        score = EmbeddingScore(0.0), // Score not directly exposed by high-level retriever
                        snippet = segment.text(),
                        sourceType = sourceType
                    ))
                } else {
                    val id = segment.metadata().getString("incident_id")?.toLongOrNull() ?: 0L
                    similarIncidents.add(RetrievalMatch(
                        id = IncidentId(id),
                        score = EmbeddingScore(0.0),
                        snippet = segment.text(),
                        sourceType = sourceType
                    ))
                }
            }

            Either.Right(RetrievalContext(
                similarIncidents = similarIncidents.take(5),
                similarRunbooks = similarRunbooks.take(3),
                query = queryText
            ))
        } catch (e: Exception) {
            log.error("Retrieval failed", e)
            Either.Left(RetrievalError.SearchFailed)
        }
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
