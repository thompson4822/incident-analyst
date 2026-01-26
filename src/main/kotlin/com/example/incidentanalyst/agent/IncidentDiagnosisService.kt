package com.example.incidentanalyst.agent

import com.example.incidentanalyst.diagnosis.DiagnosisError
import com.example.incidentanalyst.diagnosis.DiagnosisRepository
import com.example.incidentanalyst.diagnosis.DiagnosisResult
import com.example.incidentanalyst.incident.IncidentId
import com.example.incidentanalyst.incident.IncidentRepository
import com.example.incidentanalyst.incident.toDomain
import com.example.incidentanalyst.rag.RetrievalError
import com.example.incidentanalyst.rag.RetrievalResult
import com.example.incidentanalyst.rag.RetrievalService
import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
class IncidentDiagnosisService(
    private val incidentRepository: IncidentRepository,
    private val retrievalService: RetrievalService,
    private val aiService: IncidentAnalystAgent,
    private val diagnosisRepository: DiagnosisRepository
) {

    fun diagnose(incidentId: IncidentId): DiagnosisResult {
        val entity = incidentRepository.findById(incidentId.value)
            ?: return DiagnosisResult.Failure(DiagnosisError.IncidentNotFound)

        val incident = entity.toDomain()
        val retrievalResult = retrievalService.retrieve(incident)

        when (retrievalResult) {
            is RetrievalResult.Success -> {
                val context = retrievalResult.context
                val incidentText = incident.toString()
                val contextText = buildContextText(context)

                val raw = try {
                    aiService.proposeDiagnosis(incidentText, contextText)
                } catch (e: Exception) {
                    return DiagnosisResult.Failure(DiagnosisError.LlmUnavailable)
                }

                return DiagnosisResult.Failure(
                    DiagnosisError.LlmResponseInvalid("Not implemented")
                )
            }
            is RetrievalResult.Failure -> {
                return when (retrievalResult.error) {
                    is RetrievalError.InvalidQuery -> DiagnosisResult.Failure(DiagnosisError.LlmResponseInvalid("Invalid query"))
                    is RetrievalError.ModelUnavailable -> DiagnosisResult.Failure(DiagnosisError.LlmUnavailable)
                    is RetrievalError.SearchFailed -> DiagnosisResult.Failure(DiagnosisError.RetrievalFailed)
                    is RetrievalError.Unexpected -> DiagnosisResult.Failure(DiagnosisError.RetrievalFailed)
                }
            }
        }
    }

    private fun buildContextText(context: com.example.incidentanalyst.rag.RetrievalContext): String =
        """
        Similar Incidents:
        ${context.similarIncidents.joinToString("\n---\n") { match ->
            "ID: ${match.id.value}, Score: ${match.score.value}\nSnippet: ${match.snippet}"
        }}

        Similar Runbooks:
        ${context.similarRunbooks.joinToString("\n---\n") { match ->
            "ID: ${match.id.value}, Score: ${match.score.value}\nSnippet: ${match.snippet}"
        }}
        """.trimIndent()
}
