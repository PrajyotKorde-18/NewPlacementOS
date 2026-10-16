-- ============================================================
-- Placement OS — System 1: TPMS Database Schema (V4)
-- Add missing entity columns, align enum types, and align TEXT/LONGTEXT column definitions
-- ============================================================

-- Resume version extraction source & enum type alignment
ALTER TABLE resume_versions
ADD COLUMN extraction_source VARCHAR(255) NULL,
MODIFY COLUMN extraction_status ENUM('PENDING','PROCESSING','COMPLETED','PROCESSED','FAILED','FLAGGED_FOR_REVIEW') NOT NULL DEFAULT 'PENDING';

-- Role profile eligibility criteria, extraction source, and column types
ALTER TABLE role_profiles
ADD COLUMN min_cgpa DOUBLE NULL,
ADD COLUMN min_tenth_percentage DOUBLE NULL,
ADD COLUMN min_twelfth_percentage DOUBLE NULL,
ADD COLUMN max_active_backlogs INT NULL,
ADD COLUMN extraction_source VARCHAR(255) NULL,
MODIFY COLUMN required_skills LONGTEXT NOT NULL,
MODIFY COLUMN weights LONGTEXT NOT NULL;

-- Drive CTC field
ALTER TABLE drives
ADD COLUMN ctc VARCHAR(255) NULL;

-- Students placement_status enum type alignment
ALTER TABLE students
MODIFY COLUMN placement_status ENUM('NOT_PLACED','PLACED_OPEN_TO_BETTER','PLACED_FINAL') NOT NULL DEFAULT 'NOT_PLACED';

-- Student profiles text columns alignment (entities map to LONGTEXT)
ALTER TABLE student_profiles
MODIFY COLUMN skills LONGTEXT NULL,
MODIFY COLUMN projects LONGTEXT NULL,
MODIFY COLUMN internships LONGTEXT NULL,
MODIFY COLUMN certifications LONGTEXT NULL,
MODIFY COLUMN coding_profiles LONGTEXT NULL;

-- Applications status enum type & score_breakdown alignment
ALTER TABLE applications
MODIFY COLUMN status ENUM('PENDING','SHORTLISTED','NOT_SHORTLISTED','OVERRIDDEN_INCLUDED','OVERRIDDEN_EXCLUDED') NOT NULL DEFAULT 'PENDING',
MODIFY COLUMN score_breakdown LONGTEXT NULL;
