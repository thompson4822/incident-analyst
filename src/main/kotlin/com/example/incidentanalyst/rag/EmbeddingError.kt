package com.example.incidentanalyst.rag

sealed interface EmbeddingError {
    data object ModelUnavailable : EmbeddingError
    data object EmbeddingFailed : EmbeddingError
    data class PersistenceError(val cause: String) : EmbeddingError
    data object InvalidText : EmbeddingError
    data object Unexpected : EmbeddingError
}
