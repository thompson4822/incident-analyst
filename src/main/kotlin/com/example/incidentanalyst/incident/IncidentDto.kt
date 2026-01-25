package com.example.incidentanalyst.incident

data class IncidentResponseDto(
    val id: Long,
    val source: String,
    val title: String,
    val description: String,
    val severity: String,
    val status: String
)
