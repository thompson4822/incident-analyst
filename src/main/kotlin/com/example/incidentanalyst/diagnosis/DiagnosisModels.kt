package com.example.incidentanalyst.diagnosis

import com.example.incidentanalyst.incident.IncidentEntity
import com.example.incidentanalyst.incident.IncidentId
import java.time.Instant

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
    val confidence: Confidence,
    val verification: DiagnosisVerification,
    val createdAt: Instant
)

sealed interface DiagnosisError {
    data object IncidentNotFound : DiagnosisError
    data object RetrievalFailed : DiagnosisError
    data object LlmUnavailable : DiagnosisError
    data class LlmResponseInvalid(val reason: String) : DiagnosisError
    data object NotFound : DiagnosisError
    data object UpdateFailed : DiagnosisError
}

sealed interface DiagnosisResult {
    data class Success(val diagnosis: Diagnosis) : DiagnosisResult
    data class Failure(val error: DiagnosisError) : DiagnosisResult
}

fun DiagnosisEntity.toDomain(): Diagnosis =
    Diagnosis(
        id = DiagnosisId(requireNotNull(id)),
        incidentId = IncidentId(requireNotNull(incident?.id)),
        rootCause = suggestedRootCause,
        steps = remediationSteps.split("\n").filter { it.isNotBlank() },
        confidence = Confidence.valueOf(confidence),
        verification = when (verification) {
            "VERIFIED" -> DiagnosisVerification.VerifiedByHuman
            else -> DiagnosisVerification.Unverified
        },
        createdAt = createdAt
    )

fun Diagnosis.toEntity(incidentEntity: IncidentEntity): DiagnosisEntity =
    DiagnosisEntity(
        id = id.value,
        incident = incidentEntity,
        suggestedRootCause = rootCause,
        remediationSteps = steps.joinToString("\n"),
        confidence = confidence.name,
        verification = when (verification) {
            DiagnosisVerification.VerifiedByHuman -> "VERIFIED"
            DiagnosisVerification.Unverified -> "UNVERIFIED"
        },
        createdAt = createdAt
    )
