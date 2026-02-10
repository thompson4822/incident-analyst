-- Add knowledge promotion fields to diagnoses table
ALTER TABLE diagnoses ADD COLUMN IF NOT EXISTS verified_at TIMESTAMPTZ;
ALTER TABLE diagnoses ADD COLUMN IF NOT EXISTS verified_by TEXT;

-- Add source_type and diagnosis_id to incident_embeddings table
ALTER TABLE incident_embeddings ADD COLUMN IF NOT EXISTS source_type TEXT NOT NULL DEFAULT 'RAW_INCIDENT';
ALTER TABLE incident_embeddings ADD COLUMN IF NOT EXISTS diagnosis_id BIGINT REFERENCES diagnoses(id);

-- Add index on source_type for efficient filtering
CREATE INDEX IF NOT EXISTS idx_incident_embeddings_source_type ON incident_embeddings(source_type);
