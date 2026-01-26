package com.example.incidentanalyst.rag

sealed interface RetrievalResult {
    data class Success(val context: RetrievalContext) : RetrievalResult
    data class Failure(val error: RetrievalError) : RetrievalResult
}
