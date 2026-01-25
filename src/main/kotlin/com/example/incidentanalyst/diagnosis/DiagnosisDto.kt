package com.example.incidentanalyst.diagnosis

data class DiagnosisResponseDto(
    val id: Long,
    val incidentId: Long,
    val rootCause: String,
    val steps: List<String>,
    val confidence: String,
    val verified: Boolean
)
