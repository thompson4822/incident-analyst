package com.example.incidentanalyst.rag

import com.example.incidentanalyst.incident.Incident
import com.example.incidentanalyst.runbook.RunbookFragment

data class RetrievalContext(
    val similarIncidents: List<Incident>,
    val runbookFragments: List<RunbookFragment>
)
