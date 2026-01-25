package com.example.incidentanalyst.agent

import com.example.incidentanalyst.diagnosis.DiagnosisError
import com.example.incidentanalyst.diagnosis.DiagnosisRepository
import com.example.incidentanalyst.diagnosis.DiagnosisResult
import com.example.incidentanalyst.incident.IncidentId
import com.example.incidentanalyst.incident.IncidentRepository
import com.example.incidentanalyst.incident.toDomain
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
        val context = retrievalService.retrieveContext(incident)
            ?: return DiagnosisResult.Failure(DiagnosisError.RetrievalFailed)

        val incidentText = incident.toString()
        val contextText = context.toString()

        val raw = try {
            aiService.proposeDiagnosis(incidentText, contextText)
        } catch (e: Exception) {
            return DiagnosisResult.Failure(DiagnosisError.LlmUnavailable)
        }

        return DiagnosisResult.Failure(
            DiagnosisError.LlmResponseInvalid("Not implemented")
        )
    }
}
