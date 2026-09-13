-- ============================================================
-- Placement OS — System 1: TPMS Database Schema (V2)
-- Add summary and achievements text columns to student_profiles
-- ============================================================

ALTER TABLE student_profiles
ADD COLUMN summary LONGTEXT NULL,
ADD COLUMN achievements LONGTEXT NULL;
