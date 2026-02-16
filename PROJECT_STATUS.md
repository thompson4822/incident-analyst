# Incident Analyst - Project Status

## Overview

**Incident Analyst** is an AI-powered incident management and diagnosis platform. It combines RAG (Retrieval-Augmented Generation) with LLM-based analysis to help operations teams diagnose, remediate, and learn from production incidents.

The system serves as a "Generic Operational Brain" - domain-agnostic and capable of ingesting incidents from multiple sources (CloudWatch, Sentry, GitHub, PagerDuty, custom webhooks), analyzing them with AI, and tracking remediation steps for future learning.

---

## Tech Stack

| Layer | Technology |
|-------|------------|
| **Runtime** | Quarkus 3.8 + Kotlin 2.0 |
| **Persistence** | PostgreSQL + pgvector, Hibernate Panache ORM, Flyway migrations |
| **AI/LLM** | LangChain4j + Ollama (qwen2.5:7b-instruct, nomic-embed-text) |
| **Embeddings** | pgvector (768 dimensions) |
| **Frontend** | Qute templates + HTMX + Tailwind CSS + DaisyUI |
| **AWS Integration** | AWS SDK v2 (CloudWatch, CloudWatch Logs) |
| **Testing** | Quarkus Test, JUnit 5, Mockito, H2 (PostgreSQL mode) |

---

## Architecture

The project follows a **vertical slice architecture** with data-oriented programming principles:

```
src/main/kotlin/com/example/incidentanalyst/
├── agent/           # AI agent and diagnosis pipeline
├── aws/             # CloudWatch ingestion, test data generation
├── common/          # Either pattern, validation, shared utilities
├── config/          # Application profile, database config
├── diagnosis/       # Diagnosis domain (models, service, resource)
├── home/            # Dashboard and home page
├── incident/        # Incident domain (models, service, resource)
├── ingestion/       # Webhook ingestion
├── rag/             # Embedding and retrieval services
├── remediation/     # Action execution and progress tracking
├── runbook/         # Runbook fragment management
├── training/        # Training data endpoints
└── web/             # Template extensions, error handlers
```

### Key Design Patterns

- **Either Pattern**: Explicit error handling with `Either<Error, Success>` instead of exceptions
- **ADTs (Algebraic Data Types)**: Sealed interfaces for domain states (IncidentStatus, DiagnosisError, etc.)
- **Value Classes**: Type-safe IDs (IncidentId, DiagnosisId, RunbookFragmentId)
- **Vertical Slices**: Each domain is self-contained with its own models, service, and resource

---

## Completed Work

### Foundation (TICKET-001 to TICKET-005)

| Ticket | Description |
|--------|-------------|
| TICKET-001 | Bootstrap dependencies and configuration |
| TICKET-002 | Database schema with pgvector migrations |
| TICKET-003 | Incident slice (models, repository, service, resource) |
| TICKET-004 | Diagnosis slice with verification support |
| TICKET-005 | Runbook fragment management |

### Core Features (TICKET-006 to TICKET-010)

| Ticket | Description |
|--------|-------------|
| TICKET-006 | AWS CloudWatch alarm ingestion with scheduled polling |
| TICKET-007 | RAG embeddings and retrieval with pgvector |
| TICKET-008 | AI diagnosis pipeline with LangChain4j/Ollama |
| TICKET-009 | HTMX-based UI with Qute templates and DaisyUI |
| TICKET-010 | Comprehensive testing strategy (~420 tests) |

### Enhancements (TICKET-011 to TICKET-017)

| Ticket | Description |
|--------|-------------|
| TICKET-011 | CloudWatch test data generator |
| TICKET-012 | Knowledge promotion (verified diagnoses → RAG) |
| TICKET-013 | Application profile configuration |
| TICKET-014 | Resolution capture loop |
| TICKET-015 | Weighted RAG retrieval (verified cases prioritized) |
| TICKET-016 | Generic webhook ingestion |
| TICKET-017 | Remediation executor (Phase 1 - simulated) |

---

## Current Capabilities

### Incident Management
- List, view, acknowledge, and resolve incidents
- Filter by status, severity, source
- Capture resolution text for knowledge base
- Track incident lifecycle with timestamps

### AI-Powered Diagnosis
- Automatic context retrieval from similar incidents and runbooks
- LLM-generated root cause analysis
- Structured remediation steps
- Verification workflow for human approval

### Knowledge System (RAG)
- Embed incidents, runbooks, verified diagnoses, and resolutions
- Semantic search with pgvector
- Weighted retrieval prioritizing verified cases
- Source type tracking (VERIFIED_CASE, RUNBOOK, RAW_INCIDENT)

### Ingestion
- **CloudWatch**: Scheduled polling (60s), alarm-to-incident mapping
- **Webhook**: Generic REST endpoint with API key auth
- **Training**: Manual training data submission

### Remediation
- Step-by-step execution tracking
- Real-time progress via HTMX polling
- Simulated executor (ready for real integrations)

### UI/UX
- Dashboard with auto-refresh stats
- Incident list/detail views
- Diagnosis panel with verify/apply actions
- Runbook browser
- Toast notifications
- Mobile-responsive with DaisyUI

---

## Statistics

| Metric | Value |
|--------|-------|
| **Source Files** | 68 Kotlin files |
| **Test Files** | 36 test classes |
| **Tests** | 420 (418 passing, 2 skipped) |
| **Templates** | 15+ Qute templates |
| **Tickets Completed** | 17 |

---

## Future Directions

### 1. Real Action Executors (TICKET-017 Phase 2)

**Priority: High**

The remediation executor currently uses a simulated executor. Real integrations would enable automated remediation:

- **AWS Executor**: Restart EC2 instances, scale ASG, clear ElastiCache
- **Kubernetes Executor**: Restart pods, scale deployments, update configmaps
- **Slack/PagerDuty Executor**: Post updates, acknowledge alerts
- **Custom Script Executor**: Run bash/Python scripts with safety checks

**Considerations**:
- Approval workflows before execution
- Rollback mechanisms
- Audit logging
- Rate limiting and circuit breakers

---

### 2. Multi-Tenancy & Team Support

**Priority: Medium**

Enable multiple teams to use the platform with isolated data:

- Team/workspace model
- Role-based access control (RBAC)
- Per-team incident sources and runbooks
- Shared vs. private knowledge bases

---

### 3. Advanced Analytics & Reporting

**Priority: Medium**

Turn incident data into actionable insights:

- MTTR (Mean Time To Resolution) trends
- Incident frequency by source/component
- AI diagnosis accuracy tracking (verified vs. rejected)
- Knowledge base effectiveness metrics
- Export to BI tools (dashboards, reports)

---

### 4. Incident Correlation & Grouping

**Priority: Medium**

Reduce alert fatigue by correlating related incidents:

- Automatic deduplication of similar alerts
- Incident grouping by root cause
- Related incident suggestions
- Suppression rules for known issues

---

### 5. On-Call Integration

**Priority: Medium**

Connect with on-call workflows:

- PagerDuty bi-directional sync
- Slack bot for quick actions
- Microsoft Teams integration
- Mobile push notifications
- Escalation policy awareness

---

### 6. Multi-Model AI Support

**Priority: Low-Medium**

Support different LLM providers:

- OpenAI GPT-4 as alternative to Ollama
- Amazon Bedrock integration
- Azure OpenAI
- Model selection per use case (fast vs. accurate)

---

### 7. Runbook Automation

**Priority: Low-Medium**

Make runbooks more actionable:

- Auto-generate runbooks from verified diagnoses
- Step-by-step runbook execution
- Runbook versioning and approval
- Import from Confluence/Notion

---

### 8. Observability Improvements

**Priority: Low**

Better operational visibility:

- OpenTelemetry tracing
- Structured logging (JSON)
- Metrics export (Prometheus)
- Health check dashboards
- Performance monitoring

---

### 9. API & SDK

**Priority: Low**

Enable programmatic access:

- Full REST API coverage
- OpenAPI/Swagger documentation
- TypeScript SDK for frontend integrations
- CLI tool for common operations
- Terraform provider

---

### 10. Deployment & Infrastructure

**Priority: Low**

Production readiness:

- Docker/Kubernetes deployment manifests
- Helm chart
- AWS CDK/Terraform modules
- CI/CD pipeline improvements
- Database backup/restore procedures

---

## Technical Debt & Improvements

### Code Quality
- [ ] Add ktlint/detekt for code formatting
- [ ] Increase test coverage to 90%+
- [ ] Add mutation testing
- [ ] Document public APIs with KDoc

### Performance
- [ ] Add caching for frequently accessed data
- [ ] Optimize embedding queries with indexes
- [ ] Connection pool tuning
- [ ] Async processing for heavy operations

### Security
- [ ] Add authentication/authorization (OIDC, OAuth2)
- [ ] Audit logging for sensitive operations
- [ ] Secret management (Vault integration)
- [ ] Rate limiting on public endpoints

---

## Quick Start

```bash
# Start development server (requires Ollama running)
mvn quarkus:dev

# Run tests
mvn test

# Build for production
mvn clean package -DskipTests

# Generate test CloudWatch data
curl -X POST http://localhost:8080/test/cloudwatch/generate-alarms \
  -H "Content-Type: application/json" \
  -d '{"count": 10, "minSeverity": "HIGH"}'
```

---

## Contributing

1. Follow the patterns in AGENTS.md
2. Use Either pattern for error handling
3. Write tests for all new code
4. Update relevant ticket files
5. Keep slices self-contained

---

*Last updated: February 2026*
*Version: 0.1.0-SNAPSHOT*
