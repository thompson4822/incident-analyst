package com.example.incidentanalyst.rag

import com.example.incidentanalyst.incident.IncidentId
import com.example.incidentanalyst.runbook.RunbookFragmentId

data class RetrievalContext(
    val similarIncidents: List<RetrievalMatch<IncidentId>>,
    val similarRunbooks: List<RetrievalMatch<RunbookFragmentId>>,
    val query: String
)
