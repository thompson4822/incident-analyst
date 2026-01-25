# TICKET-008: Agent diagnosis pipeline

## Goal
Orchestrate retrieval and AI diagnosis with explicit results and persistence.

## Scope
- Wire `IncidentAnalystAgent` with LangChain4j/Ollama.
- Implement `IncidentDiagnosisService` to assemble context, call agent, parse JSON, and persist.
- Add pure functions to render prompts and parse JSON into domain types.

## Acceptance criteria
- Diagnosis flow returns `DiagnosisResult.Success` or `DiagnosisResult.Failure` for all outcomes.
- Invalid AI responses are mapped to `DiagnosisError.LlmResponseInvalid`.
- Successful diagnoses are persisted and linked to incidents.

## Dependencies
- TICKET-003, TICKET-004, TICKET-007.
