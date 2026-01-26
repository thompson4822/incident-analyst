package com.example.incidentanalyst.rag

sealed interface RetrievalError {
    data object InvalidQuery : RetrievalError
    data object ModelUnavailable : RetrievalError
    data object SearchFailed : RetrievalError
    data object Unexpected : RetrievalError
}
