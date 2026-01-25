-- Test migration for H2 (PostgreSQL compatibility mode)
-- Excludes vector extension since H2 doesn't support it

CREATE TABLE IF NOT EXISTS incidents (
    id BIGSERIAL PRIMARY KEY,
    source TEXT NOT NULL,
    title TEXT NOT NULL,
    description TEXT NOT NULL,
    severity TEXT NOT NULL,
    status TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS diagnoses (
    id BIGSERIAL PRIMARY KEY,
    incident_id BIGINT REFERENCES incidents(id),
    suggested_root_cause TEXT NOT NULL,
    remediation_steps TEXT NOT NULL,
    confidence TEXT NOT NULL,
    verification TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS runbook_fragments (
    id BIGSERIAL PRIMARY KEY,
    title TEXT NOT NULL,
    content TEXT NOT NULL,
    tags VARCHAR(1000),
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS incident_embeddings (
    id BIGSERIAL PRIMARY KEY,
    incident_id BIGINT REFERENCES incidents(id),
    text TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS runbook_embeddings (
    id BIGSERIAL PRIMARY KEY,
    fragment_id BIGINT REFERENCES runbook_fragments(id),
    text TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL
);
