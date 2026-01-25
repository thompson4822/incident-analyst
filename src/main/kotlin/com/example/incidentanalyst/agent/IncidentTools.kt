package com.example.incidentanalyst.agent

import com.example.incidentanalyst.incident.IncidentRepository
import dev.langchain4j.agent.tool.Tool
import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
class IncidentTools(
    private val incidentRepository: IncidentRepository
) {

    @Tool("Fetch recent incidents by source")
    fun fetchRecentIncidentsBySource(source: String): String {
        val incidents = incidentRepository.find("source", source).page(0, 5).list()
        return incidents.joinToString("\n") { it.toString() }
    }
}
