-- MySQL DDL Schema for Internship Approval System (System 2)

CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL DEFAULT 'STUDENT',
    college_id VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS students (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    department VARCHAR(100),
    gpa DOUBLE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_students_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS policy_rules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    rule_name VARCHAR(100) NOT NULL UNIQUE,
    label VARCHAR(255) NOT NULL,
    rule_type VARCHAR(50) NOT NULL,
    rule_value VARCHAR(255) NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS internship_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    company_name VARCHAR(255) NOT NULL,
    role_title VARCHAR(255) NOT NULL,
    duration_weeks INT NOT NULL,
    stipend DECIMAL(10, 2),
    start_date DATE,
    end_date DATE,
    mode VARCHAR(50) NOT NULL DEFAULT 'ON_SITE',
    hr_contact_email VARCHAR(255) NOT NULL,
    offer_letter_url VARCHAR(512),
    offer_letter_hash VARCHAR(64), -- SHA-256 hash for duplicate PDF file detection
    status VARCHAR(50) NOT NULL DEFAULT 'SUBMITTED',
    rejection_reason TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_requests_student FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS noc_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    request_id BIGINT NOT NULL UNIQUE,
    approved_by BIGINT NOT NULL,
    approved_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    pdf_path VARCHAR(512) NOT NULL,
    CONSTRAINT fk_noc_request FOREIGN KEY (request_id) REFERENCES internship_requests(id) ON DELETE CASCADE,
    CONSTRAINT fk_noc_approver FOREIGN KEY (approved_by) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS scam_pattern_flags (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    flag_type VARCHAR(100) NOT NULL,
    match_key VARCHAR(255),
    detail TEXT NOT NULL,
    detected_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS request_scam_flags (
    request_id BIGINT NOT NULL,
    flag_id BIGINT NOT NULL,
    PRIMARY KEY (request_id, flag_id),
    CONSTRAINT fk_rsf_request FOREIGN KEY (request_id) REFERENCES internship_requests(id) ON DELETE CASCADE,
    CONSTRAINT fk_rsf_flag FOREIGN KEY (flag_id) REFERENCES scam_pattern_flags(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS rule_evaluations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    request_id BIGINT NOT NULL,
    rule_name VARCHAR(100) NOT NULL,
    passed BOOLEAN NOT NULL,
    failure_reason TEXT,
    evaluated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_evaluations_request FOREIGN KEY (request_id) REFERENCES internship_requests(id) ON DELETE CASCADE
);
