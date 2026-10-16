-- ============================================================
-- Placement OS — System 2: Internship Approval Schema (V4)
-- Table: hr_confirmations (Records HR confirmation status, tokens, audit)
-- ============================================================

CREATE TABLE IF NOT EXISTS hr_confirmations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    request_id BIGINT NOT NULL UNIQUE,
    token_hash VARCHAR(64) NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'NOT_SENT',
    sent_at DATETIME NULL,
    responded_at DATETIME NULL,
    expires_at DATETIME NULL,
    responder_ip VARCHAR(64) NULL,
    resend_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_hr_confirmations_request FOREIGN KEY (request_id) REFERENCES internship_requests(id) ON DELETE CASCADE,
    INDEX idx_hr_conf_token_hash (token_hash)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
