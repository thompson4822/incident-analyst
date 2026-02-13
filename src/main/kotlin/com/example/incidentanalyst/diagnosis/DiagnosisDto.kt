package com.example.incidentanalyst.diagnosis

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class DiagnosisResponseDto(
    val id: Long,
    val incidentId: Long,
    val rootCause: String,
    val steps: List<String>,
    val confidence: String,
    val verified: Boolean,
    val verifiedAt: String? = null,
    val verifiedBy: String? = null
)

data class DiagnosisVerificationUpdateRequestDto(
    val verified: Boolean
)

data class DiagnosisVerifyRequestDto(
    @field:NotBlank(message = "verifiedBy is required and must not be blank")
    @field:Size(max = 255, message = "verifiedBy must not exceed 255 characters")
    val verifiedBy: String
)

// Extension function to convert Diagnosis domain model to DTO
fun Diagnosis.toResponseDto(): DiagnosisResponseDto =
    DiagnosisResponseDto(
        id = id.value,
        incidentId = incidentId.value,
        rootCause = rootCause,
        steps = steps,
        confidence = confidence.name,
        verified = verification is DiagnosisVerification.VerifiedByHuman,
        verifiedAt = verifiedAt?.toString(),
        verifiedBy = verifiedBy
    )
