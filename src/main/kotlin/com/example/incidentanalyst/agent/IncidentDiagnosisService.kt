package com.example.incidentanalyst.agent

import com.example.incidentanalyst.common.Either
import com.example.incidentanalyst.diagnosis.Diagnosis
import com.example.incidentanalyst.diagnosis.DiagnosisEntity
import com.example.incidentanalyst.diagnosis.DiagnosisError
import com.example.incidentanalyst.diagnosis.DiagnosisRepository
import com.example.incidentanalyst.diagnosis.DiagnosisSuccess
import com.example.incidentanalyst.diagnosis.toDomain
import com.example.incidentanalyst.incident.IncidentId
import com.example.incidentanalyst.incident.IncidentRepository
import com.example.incidentanalyst.incident.toDomain
import com.example.incidentanalyst.rag.RetrievalError
import com.example.incidentanalyst.rag.RetrievalService
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import java.time.Instant

import com.example.incidentanalyst.config.ProfileService

@ApplicationScoped
class IncidentDiagnosisService(
    private val incidentRepository: IncidentRepository,
    private val retrievalService: RetrievalService,
    private val aiService: IncidentAnalystAgent,
    private val diagnosisRepository: DiagnosisRepository,
    private val objectMapper: ObjectMapper,
    private val profileService: ProfileService
) {

    @Transactional
    fun diagnose(incidentId: IncidentId): Either<DiagnosisError, DiagnosisSuccess> {
        val entity = incidentRepository.findById(incidentId.value)
            ?: return Either.Left(DiagnosisError.IncidentNotFound)

        // Check for existing diagnosis
        val existing = diagnosisRepository.findByIncidentId(incidentId.value)
        if (existing != null) {
            return Either.Right(DiagnosisSuccess.ExistingDiagnosisFound(existing.toDomain()))
        }

        val incident = entity.toDomain()
        val profile = profileService.getProfile()
        
        return retrievalService.retrieve(incident)
            .mapLeft { error ->
                when (error) {
                    is RetrievalError.InvalidQuery -> DiagnosisError.LlmResponseInvalid("Invalid query")
                    is RetrievalError.ModelUnavailable -> DiagnosisError.LlmUnavailable
                    is RetrievalError.SearchFailed -> DiagnosisError.RetrievalFailed
                    is RetrievalError.Unexpected -> DiagnosisError.RetrievalFailed
                }
            }
            .flatMap { context ->
                val incidentText = incident.toString()
                val contextText = buildContextText(context)

                val raw = try {
                    aiService.proposeDiagnosis(
                        appName = profile.name,
                        appStack = profile.stack.joinToString(", "),
                        appComponents = profile.components.joinToString(", "),
                        incident = incidentText,
                        context = contextText
                    )
                } catch (e: Exception) {
                    return@flatMap Either.Left(DiagnosisError.LlmUnavailable)
                }

                val llmResponse = try {
                    // Clean up potential markdown blocks if LLM included them
                    val cleanJson = raw.trim().removePrefix("```json").removeSuffix("```").trim()
                    objectMapper.readValue<LlmDiagnosisResponse>(cleanJson)
                } catch (e: Exception) {
                    return@flatMap Either.Left(
                        DiagnosisError.LlmResponseInvalid("Failed to parse LLM response: ${e.message}")
                    )
                }

                val diagnosisEntity = DiagnosisEntity(
                    incident = entity,
                    suggestedRootCause = llmResponse.rootCause,
                    remediationSteps = llmResponse.steps.joinToString("\n"),
                    confidence = llmResponse.confidence.uppercase(),
                    verification = "UNVERIFIED",
                    createdAt = Instant.now()
                )
                diagnosisRepository.persist(diagnosisEntity)

                // Update incident status
                entity.status = "DIAGNOSED:${diagnosisEntity.id}"
                entity.updatedAt = Instant.now()

                Either.Right(DiagnosisSuccess.NewDiagnosisGenerated(diagnosisEntity.toDomain()))
            }
    }

    private fun buildContextText(context: com.example.incidentanalyst.rag.RetrievalContext): String =
        """
        ### Similar Past Cases (Prioritize these):
        ${context.similarIncidents.joinToString("\n---\n") { match ->
            val typeLabel = when (match.sourceType) {
                com.example.incidentanalyst.rag.SourceType.RESOLVED_INCIDENT -> "PAST RESOLUTION (High Priority)"
                com.example.incidentanalyst.rag.SourceType.VERIFIED_DIAGNOSIS -> "VERIFIED DIAGNOSIS"
                else -> "RAW INCIDENT"
            }
            "Source: $typeLabel\nID: ${match.id.value}\nContent: ${match.snippet}"
        }}

        ### Relevant Runbook Procedures:
        ${context.similarRunbooks.joinToString("\n---\n") { match ->
            "ID: ${match.id.value}\nProcedure: ${match.snippet}"
        }}
        """.trimIndent()
}

data class LlmDiagnosisResponse(
    val rootCause: String,
    val steps: List<String>,
    val confidence: String
)
