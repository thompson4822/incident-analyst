package com.example.incidentanalyst.agent

import io.quarkiverse.langchain4j.RegisterAiService
import com.example.incidentanalyst.rag.IncidentRetrievalAugmentorSupplier
import dev.langchain4j.service.UserMessage
import dev.langchain4j.service.V

@RegisterAiService(retrievalAugmentor = IncidentRetrievalAugmentorSupplier::class)
interface IncidentAnalystAgent {
    @UserMessage(
        """
        You are an expert operations analyst for the application '{appName}'.
        The application stack includes: {appStack}.
        Key components are: {appComponents}.
        
        Your task is to diagnose the current incident.
        
        ### PRIORITIZATION RULES:
        1. If a 'PAST RESOLUTION' is provided in the context and matches the current incident, prioritize its solution.
        2. If a 'VERIFIED DIAGNOSIS' is provided, use it as a high-confidence reference.
        3. Use 'Relevant Runbook Procedures' for standard troubleshooting steps.
        4. 'RAW INCIDENT' data is for pattern matching but has lower confidence than verified data.

        Given the incident below, respond with a JSON object:
        {
          "rootCause": "...",
          "steps": ["...", "..."],
          "confidence": "HIGH|MEDIUM|LOW"
        }

        Incident:
        {incident}
        """
    )
    fun proposeDiagnosis(
        @V("appName") appName: String,
        @V("appStack") appStack: String,
        @V("appComponents") appComponents: String,
        @V("incident") incident: String
    ): String
}
