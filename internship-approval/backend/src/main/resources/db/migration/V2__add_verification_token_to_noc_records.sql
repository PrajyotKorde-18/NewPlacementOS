-- ============================================================
-- Placement OS — System 2: Internship Approval Schema (V2)
-- Add verification_token and qr_generated_at to noc_records
-- ============================================================

ALTER TABLE noc_records
ADD COLUMN verification_token VARCHAR(64) NULL UNIQUE,
ADD COLUMN qr_generated_at TIMESTAMP NULL;
