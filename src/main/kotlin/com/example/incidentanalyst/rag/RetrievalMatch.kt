package com.example.incidentanalyst.rag

import com.example.incidentanalyst.incident.IncidentId
import com.example.incidentanalyst.runbook.RunbookFragmentId

data class RetrievalMatch<T>(
    val id: T,
    val score: EmbeddingScore,
    val snippet: String?
)
