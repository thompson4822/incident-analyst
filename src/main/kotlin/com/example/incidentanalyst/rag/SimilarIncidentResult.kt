package com.example.incidentanalyst.rag

data class SimilarIncidentResult(
    val id: Long,
    val incidentId: Long,
    val text: String?,
    val similarity: Double
)
