-- Add source_type to runbook_embeddings for consistency
ALTER TABLE runbook_embeddings ADD COLUMN IF NOT EXISTS source_type VARCHAR(50) NOT NULL DEFAULT 'OFFICIAL_RUNBOOK';

-- Add index for consistency
CREATE INDEX IF NOT EXISTS idx_runbook_embeddings_source_type ON runbook_embeddings(source_type);
