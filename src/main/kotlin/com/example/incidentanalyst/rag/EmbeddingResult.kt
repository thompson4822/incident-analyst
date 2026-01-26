package com.example.incidentanalyst.rag

sealed interface EmbeddingResult {
    data class Success(val count: Int) : EmbeddingResult
    data class Failure(val error: EmbeddingError) : EmbeddingResult
}
