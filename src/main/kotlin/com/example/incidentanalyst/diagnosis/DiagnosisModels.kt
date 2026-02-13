package com.example.incidentanalyst.diagnosis

import com.example.incidentanalyst.incident.IncidentEntity
import com.example.incidentanalyst.incident.IncidentId
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import java.time.Instant

private val objectMapper = ObjectMapper()
    .registerModule(KotlinModule.Builder().build())

fun parseStructuredSteps(json: String?): List<com.example.incidentanalyst.remediation.RemediationStep> {
    if (json.isNullOrBlank()) return emptyList()
    return try {
        objectMapper.readValue(
            json,
            objectMapper.typeFactory.constructCollectionType(
                List::class.java,
                com.example.incidentanalyst.remediation.RemediationStep::class.java
            )
        )
    } catch (e: Exception) {
        emptyList()
    }
}

fun serializeStructuredSteps(steps: List<com.example.incidentanalyst.remediation.RemediationStep>): String {
    return objectMapper.writeValueAsString(steps)
}

@JvmInline
value class DiagnosisId(val value: Long)

enum class Confidence {
    HIGH, MEDIUM, LOW, UNKNOWN
}

sealed interface DiagnosisVerification {
    data object Unverified : DiagnosisVerification
    data object VerifiedByHuman : DiagnosisVerification
}

data class Diagnosis(
    val id: DiagnosisId,
    val incidentId: IncidentId,
    val rootCause: String,
    val steps: List<String>,
    val structuredSteps: List<com.example.incidentanalyst.remediation.RemediationStep> = emptyList(),
    val confidence: Confidence,
    val verification: DiagnosisVerification,
    val createdAt: Instant,
    val verifiedAt: Instant? = null,
    val verifiedBy: String? = null
)

sealed interface DiagnosisError {
    data object IncidentNotFound : DiagnosisError
    data object RetrievalFailed : DiagnosisError
    data object LlmUnavailable : DiagnosisError
    data class LlmResponseInvalid(val reason: String) : DiagnosisError
    data object NotFound : DiagnosisError
    data object UpdateFailed : DiagnosisError
}

sealed interface DiagnosisSuccess {
    data class NewDiagnosisGenerated(val diagnosis: Diagnosis) : DiagnosisSuccess
    data class ExistingDiagnosisFound(val diagnosis: Diagnosis) : DiagnosisSuccess
}

fun DiagnosisEntity.toDomain(): Diagnosis =
    Diagnosis(
        id = DiagnosisId(requireNotNull(id)),
        incidentId = IncidentId(requireNotNull(incident?.id)),
        rootCause = suggestedRootCause,
        steps = remediationSteps.split("\n").filter { it.isNotBlank() },
        structuredSteps = parseStructuredSteps(structuredSteps),
        confidence = Confidence.valueOf(confidence),
        verification = when (verification) {
            "VERIFIED" -> DiagnosisVerification.VerifiedByHuman
            else -> DiagnosisVerification.Unverified
        },
        createdAt = createdAt,
        verifiedAt = verifiedAt,
        verifiedBy = verifiedBy
    )

fun Diagnosis.toEntity(incidentEntity: IncidentEntity): DiagnosisEntity =
    DiagnosisEntity(
        id = id.value,
        incident = incidentEntity,
        suggestedRootCause = rootCause,
        remediationSteps = steps.joinToString("\n"),
        structuredSteps = serializeStructuredSteps(structuredSteps),
        confidence = confidence.name,
        verification = when (verification) {
            DiagnosisVerification.VerifiedByHuman -> "VERIFIED"
            DiagnosisVerification.Unverified -> "UNVERIFIED"
        },
        createdAt = createdAt,
        verifiedAt = verifiedAt,
        verifiedBy = verifiedBy
    )
