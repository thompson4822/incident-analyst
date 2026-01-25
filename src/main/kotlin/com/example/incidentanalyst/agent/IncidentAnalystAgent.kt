package com.example.incidentanalyst.agent

import dev.langchain4j.service.UserMessage
import dev.langchain4j.service.V
import io.quarkiverse.langchain4j.RegisterAiService

@RegisterAiService
interface IncidentAnalystAgent {

    @UserMessage(
        """
        You are an AWS incident analyst.
        Given the incident and context below, respond with a JSON object:
        {
          "rootCause": "...",
          "steps": ["...", "..."],
          "confidence": "HIGH|MEDIUM|LOW"
        }

        Incident:
        {incident}

        Context:
        {context}
        """
    )
    fun proposeDiagnosis(@V("incident") incident: String, @V("context") context: String): String
}
