-- ============================================================
-- Placement OS — System 1: TPMS Database Schema (V3)
-- Extend Student Profile: Academic Eligibility, Backlogs, Identity, Professional Fields
-- ============================================================

-- Identity & Placement Status on students table
ALTER TABLE students
ADD COLUMN college_roll_number VARCHAR(100) NULL,
ADD COLUMN phone_number VARCHAR(20) NULL,
ADD COLUMN placement_status VARCHAR(50) NOT NULL DEFAULT 'NOT_PLACED';

ALTER TABLE students
ADD CONSTRAINT uk_students_college_roll_number UNIQUE (college_roll_number);

-- Professional signal fields on student_profiles table
ALTER TABLE student_profiles
ADD COLUMN linkedin_url VARCHAR(500) NULL,
ADD COLUMN github_url VARCHAR(500) NULL,
ADD COLUMN portfolio_url VARCHAR(500) NULL;

-- Academic Profile table
CREATE TABLE IF NOT EXISTS academic_profiles (
    student_id BIGINT PRIMARY KEY,
    tenth_percentage DOUBLE NULL,
    tenth_cgpa DOUBLE NULL,
    tenth_board VARCHAR(100) NULL,
    tenth_year_of_passing INT NULL,
    twelfth_percentage DOUBLE NULL,
    twelfth_cgpa DOUBLE NULL,
    twelfth_board VARCHAR(100) NULL,
    twelfth_stream VARCHAR(100) NULL,
    twelfth_year_of_passing INT NULL,
    branch VARCHAR(100) NULL,
    admission_year INT NULL,
    expected_graduation_year INT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_academic_student FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Semester SGPA table
CREATE TABLE IF NOT EXISTS semester_sgpas (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    academic_profile_id BIGINT NOT NULL,
    semester_number INT NOT NULL,
    sgpa DOUBLE NOT NULL,
    is_finalized BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_sgpa_academic FOREIGN KEY (academic_profile_id) REFERENCES academic_profiles(student_id) ON DELETE CASCADE,
    CONSTRAINT uk_academic_semester UNIQUE (academic_profile_id, semester_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Backlog records table
CREATE TABLE IF NOT EXISTS backlog_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    academic_profile_id BIGINT NOT NULL,
    subject_name VARCHAR(255) NOT NULL,
    semester INT NOT NULL,
    cleared BOOLEAN NOT NULL DEFAULT FALSE,
    cleared_on DATE NULL,
    CONSTRAINT fk_backlog_academic FOREIGN KEY (academic_profile_id) REFERENCES academic_profiles(student_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Structured Certifications table
CREATE TABLE IF NOT EXISTS student_certifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_profile_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    issuing_body VARCHAR(255) NOT NULL,
    issue_date DATE NULL,
    credential_url VARCHAR(500) NULL,
    CONSTRAINT fk_cert_profile FOREIGN KEY (student_profile_id) REFERENCES student_profiles(student_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
