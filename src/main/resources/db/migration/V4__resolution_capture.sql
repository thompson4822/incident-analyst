-- Add resolution_text to incidents table
ALTER TABLE incidents ADD COLUMN IF NOT EXISTS resolution_text TEXT;
