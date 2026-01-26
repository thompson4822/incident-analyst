package com.example.incidentanalyst.rag

data class SimilarRunbookResult(
    val id: Long,
    val fragmentId: Long,
    val text: String?,
    val similarity: Double
)
