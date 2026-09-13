-- ============================================================
-- Placement OS — System 1: TPMS Database Schema (V1)
-- Target Database: tpms_db
-- ============================================================

CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL DEFAULT 'STUDENT', -- STUDENT, COORDINATOR, ADMIN
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS students (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    college_id VARCHAR(100) NOT NULL UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_students_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS resume_versions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    extraction_status VARCHAR(50) NOT NULL DEFAULT 'PENDING', -- PENDING, PROCESSED, FAILED, FLAGGED_FOR_REVIEW
    error_log TEXT NULL,
    CONSTRAINT fk_resumes_student FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS student_profiles (
    student_id BIGINT PRIMARY KEY,
    skills JSON NULL,
    projects JSON NULL,
    internships JSON NULL,
    certifications JSON NULL,
    coding_profiles JSON NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_profiles_student FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS drives (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    college_id VARCHAR(100) NOT NULL,
    company_name VARCHAR(255) NOT NULL,
    role_title VARCHAR(255) NOT NULL,
    vacancies INT NOT NULL DEFAULT 1,
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_drives_creator FOREIGN KEY (created_by) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS role_profiles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    drive_id BIGINT NOT NULL UNIQUE,
    required_skills JSON NOT NULL, -- Map/List of skills with MUST_HAVE / NICE_TO_HAVE / BONUS
    weights JSON NOT NULL,         -- Sub-score category weights (skill match, project relevance, coding activity, certs, experience)
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_role_profiles_drive FOREIGN KEY (drive_id) REFERENCES drives(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS applications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    drive_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    score DOUBLE NULL,
    rank_position INT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING', -- PENDING, SHORTLISTED, NOT_SHORTLISTED, OVERRIDDEN_INCLUDED, OVERRIDDEN_EXCLUDED
    score_breakdown JSON NULL,                     -- Persisted 5 sub-scores breakdown
    override_reason VARCHAR(1000) NULL,            -- Mandatory non-empty reason if coordinator overrides
    overridden_by BIGINT NULL,                     -- Logged user id of coordinator who actioned override
    applied_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_apps_drive FOREIGN KEY (drive_id) REFERENCES drives(id) ON DELETE CASCADE,
    CONSTRAINT fk_apps_student FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
    CONSTRAINT fk_apps_overrider FOREIGN KEY (overridden_by) REFERENCES users(id),
    CONSTRAINT uk_drive_student UNIQUE (drive_id, student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
