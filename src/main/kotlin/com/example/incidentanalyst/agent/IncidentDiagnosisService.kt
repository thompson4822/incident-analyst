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
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import java.time.Instant
import org.jboss.logging.Logger

import com.example.incidentanalyst.config.ProfileService

@ApplicationScoped
class IncidentDiagnosisService(
    private val incidentRepository: IncidentRepository,
    private val aiService: IncidentAnalystAgent,
    private val diagnosisRepository: DiagnosisRepository,
    private val objectMapper: ObjectMapper,
    private val profileService: ProfileService
) {
    private val log = Logger.getLogger(javaClass)

    @Transactional
    fun diagnose(incidentId: IncidentId): Either<DiagnosisError, DiagnosisSuccess> {
        log.infof("Starting diagnosis for incident %d", incidentId.value)
        val entity = incidentRepository.findById(incidentId.value)
            ?: return Either.Left(DiagnosisError.IncidentNotFound).also { log.error("Incident not found") }

        // Check for existing diagnosis
        val existing = diagnosisRepository.findByIncidentId(incidentId.value)
        if (existing != null) {
            log.info("Found existing diagnosis, skipping AI call")
            return Either.Right(DiagnosisSuccess.ExistingDiagnosisFound(existing.toDomain()))
        }

        val incident = entity.toDomain()
        val profile = profileService.getProfile()
        
        val incidentText = incident.toString()

        log.info("Calling AI service for diagnosis (RAG is automated)...")
        val raw = try {
            aiService.proposeDiagnosis(
                appName = profile.name,
                appStack = profile.stack.joinToString(", "),
                appComponents = profile.components.joinToString(", "),
                incident = incidentText
            )
        } catch (e: Exception) {
            log.error("AI service call failed", e)
            return Either.Left(DiagnosisError.LlmUnavailable)
        }

        log.infof("AI Response received: %s", raw)
        val llmResponse = try {
            // Clean up potential markdown blocks if LLM included them
            val cleanJson = raw.trim().removePrefix("```json").removeSuffix("```").trim()
            objectMapper.readValue<LlmDiagnosisResponse>(cleanJson)
        } catch (e: Exception) {
            log.error("Failed to parse AI response", e)
            return Either.Left(
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

        log.infof("Diagnosis %d created and linked to incident %d", diagnosisEntity.id, entity.id)
        return Either.Right(DiagnosisSuccess.NewDiagnosisGenerated(diagnosisEntity.toDomain()))
    }
}

data class LlmDiagnosisResponse(
    val rootCause: String,
    val steps: List<String>,
    val confidence: String
)
