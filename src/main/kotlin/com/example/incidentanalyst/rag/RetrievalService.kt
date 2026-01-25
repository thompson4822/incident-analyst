package com.example.incidentanalyst.rag

import com.example.incidentanalyst.incident.Incident
import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
class RetrievalService {

    fun retrieveContext(incident: Incident): RetrievalContext? = null
}
