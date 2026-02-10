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

### ✅ Phase 1: Knowledge Capture (COMPLETED)
Implemented the "Verify" and "Promote" logic. Human-verified diagnoses are now automatically embedded into the RAG vector store.

### ✅ Phase 2: Contextualization (COMPLETED)
Moved domain-specific logic into dynamic configuration profiles. The AI now understands the specific application context (Name, Stack, Components).

### ✅ Phase 3: Weighted RAG (COMPLETED)
Updated retrieval logic to prioritize human-verified knowledge and past resolutions over raw AI suggestions using a weighted boosting engine.

### ✅ Phase 4: Resolution Loop (COMPLETED)
Implemented mandatory capture of the "Actual Fix" during incident resolution, with automated promotion to the knowledge base.

### ⏳ Phase 5: Generic Ingestion (IN PROGRESS)
Support non-AWS sources via a unified webhook adapter to prove domain agnosticism.

### 📅 Phase 6: Remediation Executor (PLANNED)
Turn AI-suggested steps into real, executable actions via a pluggable executor system.
