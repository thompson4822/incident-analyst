package com.example.incidentanalyst.diagnosis

data class DiagnosisResponseDto(
    val id: Long,
    val incidentId: Long,
    val rootCause: String,
    val steps: List<String>,
    val confidence: String,
    val verified: Boolean
)

data class DiagnosisVerificationUpdateRequestDto(
    val verified: Boolean
)

// Extension function to convert Diagnosis domain model to DTO
fun Diagnosis.toResponseDto(): DiagnosisResponseDto =
    DiagnosisResponseDto(
        id = id.value,
        incidentId = incidentId.value,
        rootCause = rootCause,
        steps = steps,
        confidence = confidence.name,
        verified = verification is DiagnosisVerification.VerifiedByHuman
    )
