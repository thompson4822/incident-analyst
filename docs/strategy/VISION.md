# Strategy: Self-Learning Operations Platform

## Vision
To build a domain-agnostic, configuration-driven operational brain that reduces the burden of incident management by learning from human expertise. The system transitions from a passive monitoring tool to an active, self-improving diagnostician.

## Core Architectural Pillars

### 1. The Learning Loop (The "Flywheel")
The system's value increases with every incident handled.
1.  **Ingest**: Incidents are pulled from configured sources (CloudWatch, Webhooks).
2.  **Propose**: AI uses RAG to suggest a diagnosis based on past verified cases and runbooks.
3.  **Verify**: A human reviews, edits, and approves the diagnosis.
4.  **Promote**: The verified diagnosis is "promoted" to the Knowledge Base (Vector Store).
5.  **Automate**: Future similar incidents are auto-diagnosed with high confidence.

### 2. Domain Agnosticism (Configuration-Driven)
The application is not "hardcoded" for AWS. It is contextualized via an **Application Profile**:
- **Context**: Defines the target app's name, tech stack, and critical components (e.g., Stripe, Postgres).
- **Ingestion**: Pluggable adapters for different signal sources.
- **Remediation**: Pluggable executors for different action types.

### 3. Data-Oriented Knowledge
Knowledge is treated as a first-class citizen:
- **Incidents**: The "Problem" state.
- **Diagnoses**: The "Analysis" state.
- **Resolutions**: The "Truth" state (what actually fixed it).
- **Runbooks**: The "Procedure" state.

## Technical Roadmap
1.  **Phase 1: Knowledge Capture**: Implement the "Verify" and "Promote" logic to start building the learning loop.
2.  **Phase 2: Contextualization**: Move domain-specific logic into dynamic configuration profiles.
3.  **Phase 3: Weighted RAG**: Update retrieval logic to prioritize human-verified knowledge over raw AI suggestions.
4.  **Phase 4: Resolution Loop**: Force the capture of the "Actual Fix" during incident resolution.
5.  **Phase 5: Generic Ingestion**: Support non-AWS sources via a unified webhook adapter.
