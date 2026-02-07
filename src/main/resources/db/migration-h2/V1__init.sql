CREATE TABLE IF NOT EXISTS incidents (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    source VARCHAR(255) NOT NULL,
    title VARCHAR(255) NOT NULL,
    description CLOB NOT NULL,
    severity VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS diagnoses (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    incident_id BIGINT,
    suggested_root_cause CLOB NOT NULL,
    remediation_steps CLOB NOT NULL,
    confidence VARCHAR(50) NOT NULL,
    verification VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_diagnoses_incident_id
        FOREIGN KEY (incident_id) REFERENCES incidents(id)
);

CREATE TABLE IF NOT EXISTS runbook_fragments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    content CLOB NOT NULL,
    tags CLOB,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS incident_embeddings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    incident_id BIGINT,
    text CLOB NOT NULL,
    embedding BLOB,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_incident_embeddings_incident_id
        FOREIGN KEY (incident_id) REFERENCES incidents(id)
);

CREATE TABLE IF NOT EXISTS runbook_embeddings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    fragment_id BIGINT,
    text CLOB NOT NULL,
    embedding BLOB,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_runbook_embeddings_fragment_id
        FOREIGN KEY (fragment_id) REFERENCES runbook_fragments(id)
);
