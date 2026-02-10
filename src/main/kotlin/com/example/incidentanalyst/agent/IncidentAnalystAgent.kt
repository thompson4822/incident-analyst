package com.example.incidentanalyst.agent

import dev.langchain4j.service.UserMessage
import dev.langchain4j.service.V
import io.quarkiverse.langchain4j.RegisterAiService

@RegisterAiService
interface IncidentAnalystAgent {

    @UserMessage(
        """
        You are an expert operations analyst for the application '{appName}'.
        The application stack includes: {appStack}.
        Key components are: {appComponents}.
        
        Given the incident and context below, respond with a JSON object:
        {
          "rootCause": "...",
          "steps": ["...", "..."],
          "confidence": "HIGH|MEDIUM|LOW"
        }

        Incident:
        {incident}

        Context (Similar past incidents and runbooks):
        {context}
        """
    )
    fun proposeDiagnosis(
        @V("appName") appName: String,
        @V("appStack") appStack: String,
        @V("appComponents") appComponents: String,
        @V("incident") incident: String,
        @V("context") context: String
    ): String
}
