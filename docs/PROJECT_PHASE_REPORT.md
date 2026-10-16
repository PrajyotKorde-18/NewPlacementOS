# PlacementOS: Comprehensive Evidence-Based Project Phase Report
**Target Audience:** Project Evaluation Committee, Faculty Reviewers, and Presentation Slide Generation  
**Scope:** Dual-Module Architecture (`tpms` and `internship-approval`)  
**Repository State:** Verified against Git History, Compilation Artifacts, Test Suites, and Source Files  
**Constraints Followed:** Read-Only Verification, No Secrets Printed, Verifiable Metrics Only, Zero Code Modifications

---

## 1. Executive Summary & Problem Statement

PlacementOS is an institutional governance platform engineered specifically for Shri Ramdeobaba College of Engineering and Management (RCOEM / Ramdeobaba University), strictly restricted to verified institutional accounts (`@rknec.edu`). It resolves four critical breakdowns in university career administration:
1. **Manual Placement Screening**: Training & Placement Offices (TPOs) spend weeks manually cross-referencing candidate resumes and spreadsheets against complex job descriptions and academic cutoffs.
2. **Opaque Shortlisting & Bias**: Students receive rejections without transparent explanations, leading to grievance overhead and accusations of coordinator bias.
3. **Late-Stage Ineligibility Disqualifications**: Students with active academic backlogs or borderline CGPAs slip through manual screening, clearing initial interview rounds only to be disqualified during final HR verification.
4. **Unregulated Off-Campus Internships & Scam Vulnerability**: Students unwittingly accept exploitative or fraudulent off-campus offers (sub-minimum stipends, fake training certificates, security deposit demands, or spoofed offer letters).

PlacementOS solves these challenges through two decoupled, independently deployable microservices: the **Training & Placement Management System (TPMS)**, which automates on-campus placement drives with deterministic academic gatekeeping and explainable candidate scoring; and the **Internship Approval System**, which enforces institutional compliance policies, evaluates multi-signal fraud patterns, coordinates external HR verification, and issues cryptographically verifiable digital No Objection Certificates (NOCs).

---

## 2. Architecture & Design Rules

PlacementOS is organized into two independent microservices, each with its own dedicated relational database, Spring Boot backend, and React Vite frontend.

```
                           +-------------------------------------------------------+
                           |           INSTITUTIONAL BROWSER CLIENT               |
                           |            (@rknec.edu accounts only)                 |
                           +---------------------------+---------------------------+
                                                       |
                        +------------------------------+------------------------------+
                        | (Port 3000)                                                 | (Port 3001)
                        v                                                             v
        +-------------------------------+                             +-------------------------------+
        |         TPMS FRONTEND         |                             |  INTERNSHIP APPROVAL FRONTEND |
        |        (React 18 / Vite)      |                             |        (React 18 / Vite)      |
        +---------------+---------------+                             +---------------+---------------+
                        | REST / JSON                                                 | REST / JSON + Multipart
                        | (Port 8081)                                                 | (Port 8082)
                        v                                                             v
        +-------------------------------+                             +-------------------------------+
        |          TPMS BACKEND         |                             |   INTERNSHIP APPROVAL BACKEND |
        |      (Spring Boot 3.2.2)      |                             |      (Spring Boot 3.2.2)      |
        |                               |                             |                               |
        | - JWT Authentication & RBAC   |                             | - JWT Authentication & RBAC   |
        | - Groq LLM JD/Resume Parser   |                             | - Policy Rule Evaluator Engine|
        | - Offline Heuristic Fallback  |                             | - Multi-Signal Scam Detector  |
        | - Deterministic Cutoff Guard  |                             | - Apache PDFBox NOC Generator |
        | - Strategy Candidate Scoring  |                             | - HMAC-SHA256 Token QR Engine |
        | - Transparent Feedback Engine |                             | - Async Transactional Email   |
        | - Async Resume Executor       |                             | - HR Confirmation Workflow    |
        +---------------+---------------+                             +---------------+---------------+
                        |                                                             |
                        v                                                             v
        +-------------------------------+                             +-------------------------------+
        |         MySQL DATABASE        |                             |         MySQL DATABASE        |
        |           (tpms_db)           |                             |        (internship_db)        |
        +-------------------------------+                             +-------------------------------+
```

### Module Specifications & Connections
- **TPMS (Training & Placement Management System)**:
  - **Backend Port**: `8081` (Context path: `/api`)
  - **Frontend Port**: `3000` (Vite dev server)
  - **Database**: MySQL `tpms_db` (Hibernate `ddl-auto: update`, migrations `V1`–`V5`)
  - **Primary Responsibilities**: Placement drive lifecycle, JD parsing, resume ingestion, deterministic academic cutoff enforcement, Strategy-pattern candidate scoring (0–100), coordinator manual overrides, and multi-section rejection feedback generation.
- **Internship Approval System**:
  - **Backend Port**: `8082` (Context path: `/api`)
  - **Frontend Port**: `3001` (Vite dev server)
  - **Database**: MySQL `internship_db` (Hibernate `ddl-auto: update`, migrations `V1`–`V4`)
  - **Primary Responsibilities**: Student offer submission, SHA-256 PDF hashing, policy rule validation, multi-signal scam detection, automated external HR verification token dispatch, CDPC review queue, Apache PDFBox NOC generation with embedded ZXing QR code, and public verification endpoints.

### Core Architectural Design Rules
1. **LLM Scope Strictly Limited to Information Extraction**: Large Language Models (Groq Cloud API) are exclusively used to extract unstructured text into structured JSON schemas (Job Descriptions and PDF Resumes). The LLM is **never** permitted to decide student eligibility, calculate fit scores, or determine pass/fail verdicts.
2. **Deterministic Academic Gating**: Eligibility is evaluated via rigid mathematical comparisons against student records (`academic_profiles`, `semester_sgpas`, `backlog_records`). If a student does not meet CGPA or backlog criteria, the backend rejects the application outright with HTTP `400 Bad Request`.
3. **Scam Flags, Never Declarations**: The scam detector identifies heuristic anomalies (`HR_DOMAIN_MISMATCH`, `DUPLICATE_OFFER_LETTER_FILE`, `MULTIPLE_REQUESTS_SAME_COMPANY`, `DUPLICATE_HR_CONTACT_EMAIL`). These anomalies are flagged as review signals for CDPC staff; the system never unilaterally accuses a student of fraud or auto-cancels without staff review.
4. **Mandatory Human Accountability**: Coordinator overrides in TPMS and CDPC manual approvals/rejections in the Internship module require mandatory written justification. All decisions record the reviewer's institutional identity and timestamp into audit logs.
5. **HR Confirmation Veto**: If an external company HR denies an offer via the secure verification link, the backend strictly blocks CDPC staff from approving the request (`CdpcReviewService` throws HTTP 400); only rejection is permitted.
6. **Independent Deployability**: Neither module shares database tables, memory space, or direct synchronous network dependencies. Failure of one microservice does not cascade to the other.

---

## 3. Chronological Phases of Development (from Git History)

### Phase 1: Project Genesis & Baseline Microservices (2026-09-13)
- **Git Commit**: `f2b918d` (*Initial commit of PlacementOS project*)
- **Goal**: Establish the decoupled dual-microservice foundation for RCOEM placement drives and internship governance.
- **What Was Built**:
  - TPMS backend (Port 8081) with Groq LLM JD extraction, resume parsing, academic cutoff validation, Strategy-pattern scoring, and React frontend (Port 3000).
  - Internship Approval backend (Port 8082) with offer submission, dynamic policy rule engine, scam pattern detector, Apache PDFBox NOC generator, and React frontend (Port 3001).
- **Key Files**: `DriveService.java`, `ScoringEngineService.java`, `InternshipSubmissionService.java`, `CdpcReviewService.java`, `NocPdfGeneratorService.java`.
- **Problems Faced & Fixed**: Hardcoded active Groq API keys existed in initial local configs; commit `f2b918d` sanitized keys to environment placeholders.
- **What Was Learned**: Maintaining two completely independent microservices cleanly isolates domain logic, but requires structured configuration management for ports and database connections.

### Phase 2: Configuration Sanitization & Java 17 Standardization (2026-10-15)
- **Git Commits**:
  - `f93acc4`: *chore: secrets cleanup, repo hygiene, root README, and Java 17 synchronization*
  - `1036553`: *feat: fail-fast startup validation for JWT secret and DB password with tests*
  - `c7ea638`: *fix: move test configurations exclusively to test resources and enforce env vars for bootstrap accounts*
- **Goal**: Eliminate hardcoded configuration secrets, prevent startup on insecure default credentials, and synchronize Java versions across modules.
- **What Was Built**:
  - Root `README.md` and repository setup documentation.
  - Fail-fast startup configuration validators checking that JWT secret and database passwords are set and meet minimum entropy.
  - Isolation of test configurations strictly to `src/test/resources/application-test.yml`.
  - Synchronization to Java 17 across both Maven `pom.xml` files and technical documentation.
- **Key Files**: `SecurityConfig.java`, `application.yml`, `application-test.yml`, `FailFastConfigTest.java`.
- **Problems Faced & Fixed**: Application previously booted successfully even if default placeholder secrets were active; solved by introducing fail-fast bean validators.
- **What Was Learned**: Security must be enforced at startup; a production backend must fail immediately rather than run with compromised defaults.

### Phase 3: Automated Smoke Testing & The Empty-Policy-Rules Bug (2026-10-15)
- **Git Commits**:
  - `7ed64cf`: *test: add automated end-to-end smoke test script for internship approval module*
  - `c33e975`: *fix: seed default policy rules on startup and harden smoke test with live thresholds and fail-fast admin validation*
- **Goal**: Implement an end-to-end automated smoke test and verify internship policy compliance under fresh database conditions.
- **What Was Built**:
  - 12-step automated PowerShell smoke test suite (`internship-approval/smoke-test.ps1`).
  - Automatic startup seeding of default institutional policy rules (Minimum Stipend ₹15,000, Minimum Duration 8 weeks).
- **Key Files**: `internship-approval/smoke-test.ps1`, `RuleEvaluatorService.java`, `PolicyRuleSeeder.java`.
- **Problems Faced & Fixed**: **The Empty-Policy-Rules Bug**: On a completely clean database, the `policy_rules` table was empty. Because no rules failed, the evaluator engine defaulted to approving every submission automatically (`RULE_APPROVED`). Fixed by adding an automatic startup seeder and treating empty rule sets as `FLAGGED_FOR_REVIEW`.
- **What Was Learned**: Compliance engines must fail-safe; absent rules must trigger human review rather than automatic open approval.

### Phase 4: Security Hardening, Multipart Fixes & Mock LLM Fallback (2026-10-15)
- **Git Commits**:
  - `3c4bb13`: *feat: security hardening, document access control, input validation, and LLM mock badges*
  - `94c4c2a`: *fix(internship): support frontend dto multipart part, allow error dispatchers, and enhance PDF signature check*
- **Goal**: Harden document access controls, validate file payloads cryptographically, fix multipart upload bugs, and provide visual indicators for offline AI fallbacks.
- **What Was Built**:
  - Magic-byte PDF inspection checking the `%PDF-` binary header, preventing disguised executable uploads.
  - Ownership-based document access controls preventing students from downloading other students' offer letters or NOCs.
  - Heuristic mock LLM fallback in TPMS and Internship with a visible `MockLLMBadge` in the UI to maintain demo continuity without misleading evaluators.
- **Key Files**: `FileUploadValidator.java`, `DocumentAccessController.java`, `ResumeExtractionService.java`, `MockLLMBadge.jsx`.
- **Problems Faced & Fixed**:
  - **Upload Part-Name Mismatch**: Frontend submitted JSON under multipart name `dto`, but Spring `@RequestPart` expected `request`, throwing HTTP 400 Bad Request. Fixed by accepting either name.
  - **File Extension Spoofing**: Users could rename `.txt` files to `.pdf`. Fixed by inspecting binary header magic bytes.
- **What Was Learned**: File extension checks are inadequate for compliance portals; byte-level inspection and transparent fallback indicators are required.

### Phase 5: Demo Safety Net & Login Preservation Contract (2026-10-16)
- **Git Commits**:
  - `9acd8ee`: *feat(auth): establish demo safety net and login preservation contract*
  - `1bae22b`: *test(internship): assert verified student record for demo student in preservation test*
  - `b7de82a`: *fix(api-contract): align controller endpoints with frontend contracts and add flow integration tests*
- **Goal**: Protect evaluation workflows, quick demo logins, and presentation states from accidental deletion during routine maintenance.
- **What Was Built**:
  - Demo profile (`application-demo.yml`) seeding persistent demo accounts (`candidate.student@rknec.edu`, `admin@rknec.edu`, `coord_user@rknec.edu`, `student1@rknec.edu`).
  - Formal preservation contract (`docs/PRESERVATION_CONTRACT.md`) locking localStorage keys, ports, and demo accounts.
  - Exemption of demo accounts from brute-force login lockouts.
- **Key Files**: `docs/PRESERVATION_CONTRACT.md`, `application-demo.yml`, `DemoAccountPreservationTest.java`.
- **Problems Faced & Fixed**: Test cleanup endpoints wiped demo accounts, breaking live presentations. Fixed by creating dedicated demo preservation rules and tests.
- **What Was Learned**: Development and presentation safety nets must be codified with regression tests to prevent live demo failures.

### Phase 6: Bounded Async Resume Processing & Lockout Isolation (2026-10-16)
- **Git Commit**: `0211281` (*feat(tpms): bounded async resume processing, status polling, startup recovery, and lockout isolation*)
- **Goal**: Prevent web thread starvation during large resume PDF parsing and isolate brute-force lockout tracking per client IP.
- **What Was Built**:
  - In-memory bounded async execution queue for resume parsing with status polling endpoints.
  - Automatic startup recovery logic marking incomplete in-flight extractions as failed.
  - IP-isolated brute-force rate limiter (5 failed attempts trigger 15-minute lockout per IP/email pair).
- **Key Files**: `ResumeExtractionService.java`, `AsyncConfig.java`, `LoginRateLimiterService.java`, `LoginRateLimiterTest.java`.
- **Problems Faced & Fixed**: Synchronous LLM extraction blocked HTTP server threads, causing frontend timeouts on large PDFs. Fixed by decoupling extraction into background tasks with polling.
- **What Was Learned**: Heavy AI operations must be decoupled from the synchronous HTTP request-response cycle.

### Phase 7: Cryptographic NOC QR Verification & Public Verify Page (2026-10-16)
- **Git Commits**:
  - `4ebeee8`: *feat(internship): cryptographic NOC QR verification, HMAC token generation, and backfill runner*
  - `f788741`: *fix(internship): add compatible constructor and import in CdpcReviewService*
  - `f7a16b1`: *fix(internship): enable authenticated official NOC PDF download for students*
  - `7a7a31c`: *feat(internship): add direct Verify NOC navigation link in header*
- **Goal**: Prevent fraudulent physical NOC printouts by embedding tamper-proof cryptographic QR codes linking to an unauthenticated public verification portal.
- **What Was Built**:
  - HMAC-SHA256 verification token generation stored in `noc_records` (`V2__add_verification_token_to_noc_records.sql`).
  - ZXing QR code generator embedding a live verification URL into the Apache PDFBox NOC PDF.
  - Public verification API (`GET /api/public/noc/verify/{token}`) and React view (`VerifyNocView.jsx`).
  - Student authenticated NOC download endpoint.
- **Key Files**: `NocVerificationService.java`, `PublicNocVerificationController.java`, `NocPdfGeneratorService.java`, `VerifyNocView.jsx`.
- **Problems Faced & Fixed**:
  - **Student NOC Download 403**: Approved students received access denied errors downloading their generated NOC. Fixed by adding explicit ownership validation in `InternshipSubmissionController`.
  - **Forged Printouts**: Physical NOCs could be doctored. Solved by allowing employers to scan the QR code and view official university records directly on the portal without logging in.
- **What Was Learned**: Digital documents require physical-to-digital cryptographic linkage for field verification.

### Phase 8: Async Transactional Email Notifications & Audit Logging (2026-10-16)
- **Git Commit**: `3d9f953` (*feat(notifications): async transactional email notifications, audit logging, and MailHog support for TPMS and internship approval*)
- **Goal**: Implement decoupled transactional notifications for placement drive publishing, student application status, and NOC issuance.
- **What Was Built**:
  - Spring `@TransactionalEventListener(phase = AFTER_COMMIT)` for async email dispatch.
  - Database audit tables (`notification_logs` in `tpms_db` and `internship_db`) logging recipient, subject, status (`SENT`, `FAILED`), and error traces.
  - MailHog / SMTP configuration for development and local testing.
- **Key Files**: `EmailNotificationService.java`, `InternshipNotificationEventListener.java`, `NotificationLog.java`, `V3__add_notification_logs.sql`.
- **Problems Faced & Fixed**: SMTP connection timeouts previously caused Spring Boot to roll back the surrounding database transaction. Fixed by moving mail dispatch to post-commit event listeners.
- **What Was Learned**: External network operations (email/SMS) must never run inside atomic database transactions.

### Phase 9: External HR Confirmation Workflow & Approval Blocking (2026-10-16)
- **Git Commits**:
  - `64cfd6d` through `d963411`: *HR offer confirmation link workflow, MailHog/Hibernate 6 fixes, and CDPC approval blocking*
- **Goal**: Eliminate counterfeit offer letters by dispatching a secure, unauthenticated tokenized confirmation link to company HR contacts and blocking approval if HR denies the offer.
- **What Was Built**:
  - Secure tokenized HR verification link generation (`/hr-confirm?token=...`).
  - Public HR Confirmation portal (`HrConfirmView.jsx`) with "Confirm Offer" and "Deny Offer" actions and optional remarks.
  - Database table `hr_confirmations` (`V4__add_hr_confirmations.sql`).
  - CDPC Review Queue live badges: Yellow (Pending), Green (Confirmed), Red (Denied).
  - **CDPC Approval Blocking**: If HR denies the offer, `CdpcReviewService` strictly blocks approval (throws HTTP 400); the frontend hides the approval button and only permits rejection.
  - Resend HR email functionality capped at 3 attempts to prevent spam.
- **Key Files**: `HrConfirmation.java`, `HrConfirmationService.java`, `PublicHrConfirmController.java`, `CdpcReviewService.java`, `CdpcReviewQueueView.jsx`.
- **Problems Faced & Fixed**:
  - `LazyInitializationException` in async email listener resolved via eager joins.
  - Hibernate 6 schema validation error resolved via `@JdbcTypeCode(SqlTypes.VARCHAR)`.
  - Transaction rollbacks resolved via `Propagation.REQUIRES_NEW`.
  - CDPC staff could previously approve offers that HR had officially denied. Fixed by enforcing a backend hard block when HR status is `DENIED`.
- **What Was Learned**: Real-world verification must cross institutional boundaries; external negative confirmation must have veto power over internal approval.

---

## 4. Feature Inventory per Module

| Module | Feature | Status | Evidence in Code | Target Persona |
| :--- | :--- | :---: | :--- | :--- |
| **TPMS** | Institutional Email Authentication (`@rknec.edu`) | **BUILT** | `AuthController.java`, `JwtFilter.java`, `SecurityConfig.java` | All TPMS Users |
| **TPMS** | AI Job Description (JD) Extraction Wizard | **BUILT** | `JdExtractionService.java`, `CreateDriveView.jsx` | Coordinator, Admin |
| **TPMS** | AI PDF Resume Parser & Skill Extraction | **BUILT** | `ResumeExtractionService.java`, `ProfileView.jsx` | Student |
| **TPMS** | Offline Heuristic Mock AI Fallback & Badge | **BUILT** | `JdExtractionService.java`, `MockLLMBadge.jsx` | Student, Coordinator |
| **TPMS** | Deterministic Academic Eligibility Guard | **BUILT** | `DriveService.java` (lines 146–196), `AcademicProfileEligibilityTest.java` | Student |
| **TPMS** | Strategy-Pattern Candidate Fit Scoring (0–100) | **BUILT** | `ScoringEngineService.java`, `SkillMatchScoringStrategy.java`, etc. | Coordinator |
| **TPMS** | Coordinator Manual Overrides with Mandatory Justification | **BUILT** | `ApplicationService.java` (lines 33–55), `RankedShortlistView.jsx` | Coordinator |
| **TPMS** | Transparent Rejection Feedback Generator | **BUILT** | `FeedbackGeneratorService.java`, `FeedbackModal.jsx` | Student |
| **TPMS** | Bounded Async Resume Queue & Polling | **BUILT** | `ResumeExtractionService.java`, `AsyncConfig.java` | Student |
| **TPMS** | IP-Based Brute Force Login Rate Limiter & Lockout | **BUILT** | `LoginRateLimiterService.java`, `LoginRateLimiterTest.java` | All TPMS Users |
| **TPMS** | Placement Drive Application History & Status View | **BUILT** | `DriveController.java`, `StudentApplicationsView.jsx` | Student |
| **TPMS** | Administrative Test Data Cleanup Endpoint | **BUILT** | `AdminDataCleanupController.java`, `AdminDataCleanupTest.java` | Admin |
| **TPMS** | TPO Executive Analytics & Placement Visual Charts | **PLANNED** | Documented in roadmap; no controllers or charts exist | TPO Staff, Admin |
| **TPMS** | Automated Interview Slot Booking Engine | **PLANNED** | Documented in roadmap; no entities or calendar logic | Student, Coordinator |
| **Internship** | Student Offer Submission & SHA-256 PDF Hashing | **BUILT** | `InternshipSubmissionService.java`, `StudentSubmissionView.jsx` | Student |
| **Internship** | Dynamic Policy Rule Evaluator (Stipend, Duration, Lists)| **BUILT** | `RuleEvaluatorService.java`, `MinStipendRuleEvaluator.java`, etc. | Student, CDPC Staff |
| **Internship** | Multi-Signal Scam Pattern Detector (4 Heuristics) | **BUILT** | `ScamPatternDetectorService.java`, `ScamPatternDetectorServiceTest.java` | CDPC Staff |
| **Internship** | CDPC Review Queue with Flag Indicators | **BUILT** | `CdpcReviewService.java`, `CdpcReviewQueueView.jsx` | CDPC Staff, Admin |
| **Internship** | Apache PDFBox Digital NOC PDF Generator | **BUILT** | `NocPdfGeneratorService.java`, `NocRecord.java` | Student, CDPC Staff |
| **Internship** | Cryptographic HMAC-SHA256 Token QR Verification | **BUILT** | `NocVerificationService.java`, `PublicNocVerificationController.java` | Public / Employer |
| **Internship** | Public Unauthenticated NOC Verification Page | **BUILT** | `PublicNocVerificationController.java`, `VerifyNocView.jsx` | Public / Employer |
| **Internship** | External HR Offer Confirmation Link Workflow | **BUILT** | `HrConfirmationService.java`, `PublicHrConfirmController.java`, `HrConfirmView.jsx` | External Company HR |
| **Internship** | Hard CDPC Approval Blocking on HR Denial | **BUILT** | `CdpcReviewService.java` (lines 75–88), `CdpcReviewQueueView.jsx` | CDPC Staff |
| **Internship** | CDPC Policy Rule Management Editor | **BUILT** | `PolicyRuleController.java`, `CdpcPolicyRulesView.jsx` | CDPC Admin |
| **Internship** | Authenticated Student NOC PDF Download | **BUILT** | `InternshipSubmissionController.java` (lines 142–165) | Student |
| **Internship** | Institutional Email Authentication & Rate Limiting | **BUILT** | `AuthController.java`, `LoginRateLimiterService.java` | All Users |
| **Cross-Mod** | Single Sign-On (SSO) / Shared Identity Provider | **NOT FOUND**| Independent user tables (`tpms_db.users` vs `internship_db.users`) | All Users |
| **Cross-Mod** | Dockerfile & Docker Compose Multi-Container Orchestration| **NOT FOUND**| No Dockerfiles or docker-compose manifests exist in repo | DevOps / Infra |
| **Cross-Mod** | GitHub Actions CI/CD Automated Build Pipeline | **NOT FOUND**| No `.github/workflows/` files exist in repository | DevOps / Infra |
| **Cross-Mod** | WhatsApp / SMS Alert Gateway Integration | **NOT FOUND**| Mentioned in preliminary pitch docs; no code or SDKs exist | Student |

---

## 5. Security & Integrity Measures in Code

### 1. Role-Based Access Control (RBAC) & Institutional Domain Locking
- **Domain Guard**: Self-registration endpoints in both modules validate email suffixes against `@rknec.edu`. Personal emails (`@gmail.com`, `@yahoo.com`) are rejected with HTTP 400.
- **Role Hierarchy**:
  - TPMS: `ROLE_STUDENT`, `ROLE_COORDINATOR`, `ROLE_ADMIN` (`tpms/backend/src/main/java/com/placementos/tpms/model/Role.java`).
  - Internship: `ROLE_STUDENT`, `ROLE_CDPC_STAFF`, `ROLE_CDPC_ADMIN` (`internship-approval/backend/src/main/java/com/placementos/internship/model/Role.java`).
- **Endpoint Security**: Configured via Spring Security `SecurityFilterChain` with method-level authorization (`@PreAuthorize("hasRole('ADMIN')")`).

### 2. Upload Validation & Anti-Spoofing Checks
- **Magic-Byte Inspection**: `FileUploadValidator.java` inspects the initial binary bytes of uploaded files. Files must match `%PDF-` (bytes `0x25, 0x50, 0x44, 0x46, 0x2D`) to be processed. Renamed executables or text files are rejected.
- **Document Access Control**: `DocumentAccessController.java` verifies that the requesting JWT user owns the document or possesses staff/admin privileges. Students cannot access peers' offer letters or resumes (returns HTTP 403 Forbidden).

### 3. Brute Force Protection & Isolated Lockout
- **Service**: `LoginRateLimiterService.java` (present in both backends).
- **Rule**: Tracks failed login attempts per client IP and email address. 5 failed attempts within 15 minutes trigger a 15-minute lockout (HTTP 429 Too Many Requests).
- **IP Isolation**: Lockouts are isolated to the offending client IP via `X-Forwarded-For` inspection, preventing malicious actors from locking out legitimate users on other networks.
- **Demo Exemption**: Verified demo credentials are explicitly exempted to prevent presentation disruptions.

### 4. Audit Logging & Non-Repudiation
- **Coordinator Overrides**: `ApplicationService.java` requires mandatory non-empty text justification for force-including or force-excluding students. Records coordinator ID, action, reason, and timestamp.
- **CDPC Decision Audit**: `CdpcReviewService.java` enforces terminal status protection (`MANUALLY_APPROVED` or `MANUALLY_REJECTED` cannot be re-executed) and logs staff reviewer ID, decision notes, and approval timestamp.
- **Notification Logs**: All transactional emails are recorded in database tables (`notification_logs`) with timestamps, delivery status (`SENT`, `FAILED`), recipient, and error traces.

### 5. Cryptographic Tokens & Public Endpoint Protections
- **HMAC-SHA256 NOC Tokens**: `NocVerificationService.java` signs NOC records using an HMAC-SHA256 signature containing the request ID, student ID, and company details.
- **Public Verification Endpoint**: `GET /api/public/noc/verify/{token}` allows employers or campus guards to verify authenticity without requiring institutional login credentials.
- **Secure HR Tokenized Links**: HR verification links use cryptographically random 256-bit URL-safe tokens with expiration timestamps, rate-limited resend caps (maximum 3), and one-time execution flags.

---

## 6. AI Usage and Honesty

PlacementOS enforces strict architectural boundaries regarding where Artificial Intelligence is utilized:

### Exactly Where the LLM is Used
1. **Job Description Extraction** (`JdExtractionService.java`): Coordinators paste unstructured JD text or upload PDF/Word files. Groq Cloud API (Llama models via JSON mode) extracts:
   - Categorized skills: `MUST_HAVE`, `NICE_TO_HAVE`, `BONUS`.
   - Dimension weights: Skills, Projects, Coding, Certifications, Experience (summing to 1.0).
   - Academic Cutoffs: Minimum CGPA, 10th %, 12th %, maximum allowed backlogs.
2. **Resume PDF Parsing** (`ResumeExtractionService.java`): Extracts student skills, projects, certifications, and work experience into structured profile entities.

### The Offline Mock Fallback Engine
- If Groq API keys are missing, invalid, or rate-limited, the system automatically falls back to an internal heuristic regex extractor (`extractMockedJdData`, `extractMockedLlmData`).
- **Zero Hallucination UI Badging**: The system tags extracted records with `extraction_source = MOCK`. The React frontend displays an amber `MockLLMBadge` ("Demo / Mock Mode Active") ensuring evaluators are never misled into believing mock data came from a live LLM.

### What the LLM Never Decides
- **Eligibility**: Academic cutoffs are evaluated deterministically in Java code. The LLM has zero input on eligibility verdicts.
- **Candidate Scoring**: Fit scores are computed deterministically via Strategy Pattern classes normalized to a 0–100 scale.
- **Fraud/Scam Detection**: Scam signals are computed using deterministic heuristic rules and database counts.
- **Approval / Rejection Decisions**: Final approvals for placement drives and internship NOCs are exclusively executed by human coordinators and CDPC staff.

---

## 7. Testing & Quality Assurance

### Verified Test Counts (from Workspace Execution Records)
- **TPMS Backend**: **21 tests run, 0 failures, 0 errors** (`mvn test`, verified in audit baseline).
- **Internship Approval Backend**: **20 tests run, 0 failures, 0 errors** (`mvn test`, verified in audit baseline).
- **Subsequent Test Suites**: Not re-run for this report (per read-only task constraints).
- **Frontend Production Bundles**:
  - TPMS Frontend: Built cleanly via `npm run build` (1,480 modules transformed).
  - Internship Frontend: Built cleanly via `npm run build` (1,477 modules transformed).

### Test Types Present in Codebase
1. **Unit Tests**:
   - Scoring strategy evaluation (`SkillMatchScoringStrategyTest`, `CodingActivityScoringStrategyTest`).
   - Scam pattern detection heuristics (`ScamPatternDetectorServiceTest`).
   - Policy rule evaluators (`MinStipendRuleEvaluatorTest`, `MinDurationRuleEvaluatorTest`).
2. **Integration / Web Layer Tests**:
   - Rate limiting and IP lockout isolation (`LoginRateLimiterTest`).
   - Fail-fast configuration validation (`FailFastConfigTest`).
   - Data cleanup endpoint security (`AdminDataCleanupTest`).
   - Demo preservation contract assertion (`DemoAccountPreservationTest`).
3. **Automated End-to-End Smoke Test** (`internship-approval/smoke-test.ps1`):
   - Executes 12 live HTTP API steps against running backend (Port 8082):
     1. Student registration (2 distinct students).
     2. Authentication verification for students and CDPC Admin.
     3. Active policy rule validation.
     4. Dynamic threshold reading.
     5. Real-time dynamic PDF creation.
     6. Multi-scenario offer submissions (clean offer, free Gmail domain, low stipend, short duration, duplicate PDF hash).
     7. Offer letter document access control (owner 200, other student 403, unauthenticated 401).
     8. CDPC review queue retrieval.
     9. Admin approval & terminal state conflict guard (409 Conflict on re-approval).
     10. Approved NOC document access control (owner 200, other student 403, unauthenticated 401).
     11. Admin rejection with mandatory reason validation.
     12. RBAC check (student calling admin action receives 403 Forbidden).

### What Has NOT Been Verified
- **Live Production Groq Cloud API**: Groq API keys in the repository are placeholder strings; live LLM extraction was not tested with an active paid Groq key during this audit (offline mock fallback verified).
- **Real-World SMTP Server Delivery**: Email notifications were verified against local MailHog / SMTP listeners; production delivery to public email servers (e.g. Google Workspace SMTP) has not been run.
- **High-Concurrency Load Testing**: No stress/load testing (e.g., JMeter, Gatling) has been executed.

---

## 8. Known Limitations & Plain Technical Discrepancies

1. **Independent Identity Silos (No Single Sign-On)**: TPMS and Internship Approval maintain separate user tables in separate databases (`tpms_db.users` vs `internship_db.users`). A student registered in TPMS cannot log into the Internship portal without registering a second time.
2. **Flyway Migration Dependency Omission**: SQL migration scripts (`V1`–`V5` in TPMS, `V1`–`V4` in Internship) are maintained in `src/main/resources/db/migration/`, but `flyway-core` is omitted from both `pom.xml` files. The backends rely on Hibernate `ddl-auto: update`, meaning versioned migrations are not executed deterministically by Flyway.
3. **In-Memory Asynchronous Queue**: The TPMS resume parsing queue uses Spring `@Async` thread pools rather than an external distributed message broker (e.g. RabbitMQ, Kafka). If the server restarts during bulk uploads, in-flight jobs must be re-submitted.
4. **No Dockerization or Container Orchestration**: There are no `Dockerfile` or `docker-compose.yml` configurations in the repository. Running the full stack requires manually launching four local processes and two MySQL databases.
5. **No Automated CI/CD Workflows**: The repository contains no `.github/workflows/` files. Automated regression testing and build validation on git push are not configured.

---

## 9. Future Work (Unbuilt Roadmap Features)

The following features were documented in early planning roadmaps (`PROJECT_DEVELOPMENT_TIMELINE.md`) but have **0% code implementation** in the repository:
1. **TPO Executive Analytics Dashboard**: Real-time visualization of placement drive percentages, department-wise recruitment ratios, and CTC distribution graphs.
2. **Automated Interview Slot Booking Engine**: Calendar scheduling entity enabling shortlisted students to self-select available interview slots with company interviewers.
3. **SMS & WhatsApp Multi-Channel Alert Gateway**: Direct mobile messaging integration for urgent drive deadlines or NOC approvals.
4. **Unified OAuth2 / OIDC Central Identity Provider**: Single Sign-On service allowing unified institutional authentication across both microservices.

---

## 10. Claims to Avoid in the Presentation

When presenting this project to faculty or technical reviewers, **strictly avoid** the following unsupported statements:
- ❌ **Do NOT claim**: *"The system uses Kafka or distributed message brokers for async processing."*  
  ✔️ **Truth**: Uses Spring Boot's internal bounded `@Async` thread pool.
- ❌ **Do NOT claim**: *"Internships are instantly approved by AI."*  
  ✔️ **Truth**: The AI never approves anything; offers undergo deterministic rule evaluation, fraud pattern checks, external HR confirmation, and final CDPC staff review.
- ❌ **Do NOT claim**: *"The platform sends WhatsApp or SMS alerts."*  
  ✔️ **Truth**: Only transactional email notifications (with database logging) are implemented.
- ❌ **Do NOT claim**: *"Placement drive time is reduced by 85%" or "resumes are parsed in 1.2 seconds."*  
  ✔️ **Truth**: No formal performance benchmarks or speed percentages were measured; state the algorithmic flow instead.
- ❌ **Do NOT claim**: *"Database migrations are automatically managed by Flyway in production."*  
  ✔️ **Truth**: SQL migration scripts follow Flyway conventions, but execution currently relies on Hibernate `ddl-auto: update`.
- ❌ **Do NOT claim**: *"A single user account works seamlessly across both frontends."*  
  ✔️ **Truth**: TPMS and Internship Approval currently operate as independent microservices with separate user registries.

---

## 11. Slide Plan (16-Slide Presentation Outline)

### Slide 1: Title & Institutional Vision
- **Title**: PlacementOS: Institutional Placement & Internship Governance Platform
- **Bullets**:
  - Engineered specifically for Shri Ramdeobaba College of Engineering and Management (`@rknec.edu`).
  - Dual-microservice architecture governing on-campus placement drives and off-campus internships.
  - Transparent candidate scoring, deterministic academic gatekeeping, and cryptographic fraud protection.
  - Presenter: Arjun Jaiswal | Computer Science & Engineering.
- **Visual**: PlacementOS logo, dual-module system architecture diagram.

### Slide 2: The Core Problem Statement
- **Title**: Operational Bottlenecks in University Career Centers
- **Bullets**:
  - Manual spreadsheet evaluation: TPOs spend weeks cross-checking candidate cutoffs.
  - Opaque shortlisting: Students lack actionable feedback on rejections.
  - Cutoff bypasses: Students with uncleared backlogs face embarrassing final-round disqualifications.
  - Unregulated off-campus offers: Proliferation of fraudulent offers, sub-minimum stipends, and fake certificates.
- **Visual**: Side-by-side comparison diagram: "Manual Spreadsheets vs. PlacementOS Governance".

### Slide 3: System Architecture & Dual Microservices
- **Title**: Dual Microservice Architecture & Port Topology
- **Bullets**:
  - TPMS (Port 8081 / 3000): On-campus drives, JD/resume parsing, candidate scoring, and feedback.
  - Internship Approval (Port 8082 / 3001): Compliance evaluation, scam detection, HR confirmation, and NOC generation.
  - Fully decoupled relational databases: `tpms_db` and `internship_db`.
  - Independent deployability: Neither module is a single point of failure for the other.
- **Visual**: Microservices block diagram showing React frontends, Spring Boot backends, MySQL databases, and MailHog.

### Slide 4: Core Architectural Design Principles
- **Title**: Architectural Invariants & Responsible Design Rules
- **Bullets**:
  - AI for extraction only: LLM parses documents into JSON; never calculates scores or decides eligibility.
  - Deterministic cutoff gating: Hard mathematical evaluation of CGPA, 10th/12th %, and backlogs.
  - Fraud flagged, never declared: Heuristics surface risk signals for human evaluation.
  - Mandatory human accountability: Coordinator overrides and CDPC decisions require written reasons in audit logs.
- **Visual**: Flowchart showing LLM Extraction -> Deterministic Rules -> Human Review -> Final Action.

### Slide 5: TPMS: AI Job Description Parsing & Drive Wizard
- **Title**: TPMS: AI-Assisted Job Description Extraction
- **Bullets**:
  - Coordinators upload JD files (`.pdf`, `.docx`, `.txt`) or paste raw job descriptions.
  - Groq Cloud LLM extracts categorized skills (`MUST_HAVE`, `NICE_TO_HAVE`), weights, and cutoffs into structured JSON.
  - Resilient offline fallback: Automatic heuristic mock engine activates if API keys fail.
  - 3-step interactive coordinator wizard to review and calibrate parameters before drive publication.
- **Visual**: Screenshot of `CreateDriveView` wizard with extracted skills and cutoffs (Role: `COORDINATOR`, `coord_user@rknec.edu`).

### Slide 6: TPMS: Deterministic Academic Eligibility Gating
- **Title**: Hard Academic Eligibility Enforcement
- **Bullets**:
  - Validates applicant against drive criteria before saving application:
    - Student CGPA $\ge$ Minimum CGPA
    - 10th & 12th Board Marks $\ge$ Minimum Percentage
    - Active Backlog Count $\le$ Maximum Allowed Backlogs
  - Enforced server-side in `DriveService.java`; ineligible attempts immediately return HTTP 400 Bad Request.
  - Completely prevents late-stage student disqualifications during company interviews.
- **Visual**: Code snippet of `DriveService.applyToDrive` and student application rejection UI.

### Slide 7: TPMS: Strategy-Pattern Candidate Scoring & Ranking
- **Title**: Explainable Candidate Scoring Engine (0–100)
- **Bullets**:
  - Implements the Strategy Pattern across 5 distinct evaluation dimensions:
    - Skill Match, Project Relevance, Coding Activity, Certifications, Experience.
  - Normalized composite fit score calculated dynamically based on drive weights.
  - Prerequisite rule: Applicants with 0 skill match are barred from automatic shortlisting.
  - Generates dynamic ranked shortlists based on available vacancy quotas.
- **Visual**: Screenshot of `RankedShortlistView` showing candidate scores and sub-score breakdowns (Role: `COORDINATOR`).

### Slide 8: TPMS: Accountable Overrides & Transparent Feedback
- **Title**: Accountable Overrides & Explainable Rejection Feedback
- **Bullets**:
  - Coordinators can force-include or force-exclude candidates with mandatory written justification.
  - Audited actions log coordinator ID, timestamp, and justification to the database.
  - Feedback Generator produces transparent, multi-section markdown reports for students.
  - Reports outline decision drivers, candidate strengths, missing skills, and a personalized improvement roadmap.
- **Visual**: Screenshot of `FeedbackModal` displaying detailed candidate feedback (Role: `STUDENT`, `candidate.student@rknec.edu`).

### Slide 9: Internship Approval: Dynamic Policy Rule Engine
- **Title**: Internship Governance & Dynamic Rule Evaluation
- **Bullets**:
  - Students submit company, role, stipend, duration, and offer letter PDF.
  - Backend calculates cryptographic SHA-256 checksum of uploaded offer letter.
  - Dynamic rule evaluator checks live institutional policy rules:
    - Minimum stipend threshold (e.g., ₹15,000/month).
    - Minimum internship duration (e.g., 8 weeks).
    - Institutional company allowlists and denylists.
  - Assigns initial evaluation status: `RULE_APPROVED`, `RULE_REJECTED`, or `FLAGGED_FOR_REVIEW`.
- **Visual**: Screenshot of `StudentSubmissionView` and policy rules configuration table (Role: `STUDENT` & `CDPC_ADMIN`).

### Slide 10: Multi-Signal Internship Scam Pattern Detector
- **Title**: Multi-Signal Fraud & Scam Pattern Detection
- **Bullets**:
  - Evaluates 4 distinct heuristic scam indicators across submissions:
    - `HR_DOMAIN_MISMATCH`: Flags HR contact emails using free domains (`gmail.com`, `yahoo.com`).
    - `DUPLICATE_OFFER_LETTER_FILE`: Detects identical PDFs uploaded by different students via SHA-256 hash match.
    - `MULTIPLE_REQUESTS_SAME_COMPANY`: Flags abnormal submission velocity for a company within 60 days.
    - `DUPLICATE_HR_CONTACT_EMAIL`: Flags identical HR emails submitted for different companies.
  - Flags surface as high-visibility warning badges in the CDPC staff review queue.
- **Visual**: Screenshot of CDPC Review Queue with Scam Pattern Warning Badge (Role: `CDPC_ADMIN`, `admin@rknec.edu`).

### Slide 11: External HR Offer Confirmation Workflow
- **Title**: External HR Verification & CDPC Approval Block
- **Bullets**:
  - System automatically generates a secure, tokenized confirmation link sent to the company HR contact.
  - Company HR accesses public portal (`/hr-confirm?token=...`) to verify or deny the offer without logging in.
  - Live CDPC Review Queue badges: Yellow (Pending), Green (Confirmed), Red (Denied).
  - **Hard Approval Block**: If HR denies the offer, CDPC staff are strictly blocked from approving; only rejection is permitted.
  - Resend HR link feature capped at 3 attempts to prevent spamming recruiters.
- **Visual**: Screenshot of the public HR Confirmation page (`/hr-confirm`) and CDPC approval button disabled in red.

### Slide 12: Digital NOC Generation & Cryptographic QR Verification
- **Title**: Official NOC Generation & Tamper-Proof QR Verification
- **Bullets**:
  - CDPC staff approval triggers Apache PDFBox to generate an official institutional No Objection Certificate.
  - Generates an HMAC-SHA256 verification token embedded into a ZXing QR code on the certificate.
  - Public verification portal (`/verify-noc/:token`) allows employers or authorities to verify certificate validity instantly.
  - Students can download the official signed NOC PDF directly from their authenticated portal.
- **Visual**: Generated NOC PDF showing the embedded QR code and the public `/verify-noc` verification page.

### Slide 13: Enterprise Security & System Integrity
- **Title**: Security Hardening & Abuse Prevention
- **Bullets**:
  - Strict institutional domain locking: Only `@rknec.edu` email registrations permitted.
  - File security: Magic-byte inspection (`%PDF-`) prevents executable spoofing; strict ownership access controls.
  - Brute force protection: 5 failed login attempts trigger a 15-minute lockout isolated per client IP.
  - Async transactional emails: Event-driven post-commit email dispatch with database audit logging.
- **Visual**: Diagram of the Security Filter Chain, Rate Limiter, and Magic-Byte File Validator.

### Slide 14: Engineering Verification & Smoke Test Results
- **Title**: Quality Assurance & Verification Evidence
- **Bullets**:
  - TPMS Backend: 21 passing automated tests (`mvn test`, 0 failures, 0 errors).
  - Internship Approval Backend: 20 passing automated tests (`mvn test`, 0 failures, 0 errors).
  - Production builds: Clean frontend bundle generation via Vite (TPMS: 297 kB, Internship: 201 kB).
  - 12-Step Automated Smoke Test Suite (`smoke-test.ps1`): End-to-end verification of registration, submission, policy rules, and NOC generation.
- **Visual**: Summary table of test counts and screenshot of passing smoke test output terminal.

### Slide 15: Limitations, Plain Truths & Next Steps
- **Title**: Current Limitations & Future Roadmap
- **Bullets**:
  - Honest current state: Separate identity stores (no SSO); migrations rely on Hibernate update (Flyway missing in pom); in-memory async queues.
  - DevOps gaps: No Docker containers or CI/CD pipelines currently configured.
  - Future roadmap: TPO executive analytics dashboard, automated interview slot scheduling, and central SSO identity.
- **Visual**: Roadmap quadrant table comparing Built Features vs. Planned Extensions.

### Slide 16: Summary & Conclusion
- **Title**: PlacementOS: Governance, Transparency & Trust
- **Bullets**:
  - Transformed campus placements from manual spreadsheet chaos to structured, explainable automation.
  - Eliminated student uncertainty with transparent cutoff enforcement and multi-section feedback.
  - Protected students and the university from fraudulent internships with multi-signal detection and HR verification.
  - Delivered a robust, verifiable software foundation tailored specifically to RCOEM.
- **Visual**: High-level platform summary card with key architectural metrics.

---

## 12. Faculty Viva Voce Q&A (15 Technical Questions & Honest Answers)

**Q1: Why did you separate the system into two distinct microservices instead of building a single monolithic application?**  
**A:** TPMS and Internship Approval serve fundamentally different operational cadences and administrative stakeholders. TPMS operates in seasonal, high-intensity spikes during campus recruitment drives (September–March), managed by department placement coordinators. Internship Approval runs continuously year-round, managed by the Central Department Placement Cell (CDPC). Decoupling them into independent microservices with separate databases ensures that high traffic during a mega-drive on TPMS cannot impair internship compliance or NOC generation, and allows independent scaling and maintenance.

**Q2: What is the exact role of Artificial Intelligence in PlacementOS, and can the LLM make mistaken shortlisting decisions?**  
**A:** The LLM is strictly confined to unstructured information extraction (parsing Job Descriptions and PDF resumes into structured JSON). The LLM is **never** permitted to calculate fit scores, evaluate cutoffs, or make shortlisting decisions. Scoring is computed deterministically via Java Strategy classes, and eligibility is enforced through rigid mathematical comparisons in `DriveService.java`. Even if the LLM extracts an unexpected skill, human coordinators review the extracted parameters in a 3-step wizard before the drive is published.

**Q3: What happens if the Groq Cloud LLM API is unavailable, rate-limited, or the internet disconnects?**  
**A:** Both backends implement an automatic heuristic offline mock fallback (`extractMockedJdData`, `extractMockedLlmData`). If API calls fail or keys are omitted, the service falls back to regex-based heuristics without throwing runtime exceptions. Furthermore, the system tags extracted records with `extraction_source = MOCK`, causing the React frontend to display an amber `MockLLMBadge` so users are aware mock data is active.

**Q4: How do quick demo logins work, and why do they exist? Would they be present in production?**  
**A:** Quick demo logins (e.g. clicking "Student Demo" or "CDPC Admin Demo" on the login screen) are development-profile conveniences enabled via `application-demo.yml` and `DemoDataSeeder.java`. They auto-fill verified credentials so evaluators can inspect multi-role workflows without manual account creation. In a production build, the demo profile is disabled, demo seeders do not run, and the frontend demo button components are omitted or conditioned on `import.meta.env.DEV`.

**Q5: How does the system prevent student fraud in off-campus internship offer letters?**  
**A:** Through a 3-tier defense: (1) Cryptographic SHA-256 hashing detects if multiple students upload the identical PDF file; (2) Multi-signal heuristic detection flags mismatched free HR email domains (`@gmail.com`) and suspicious company submission velocity; and (3) The external HR confirmation workflow dispatches a one-click verification link to the official company contact. If HR denies the offer, the backend strictly blocks CDPC staff from approving it.

**Q6: What was the "Empty-Policy-Rules Bug" discovered during Phase 3, and how was it fixed?**  
**A:** During initial smoke testing on a fresh database, the `policy_rules` table was empty. Because the rule evaluator found no active rules to violate, it defaulted to open evaluation, marking every student submission as `RULE_APPROVED`. We fixed this by introducing a startup seeder (`PolicyRuleSeeder.java`) that guarantees baseline minimum stipend (₹15,000) and duration (8 weeks) rules exist, and updated the engine to treat an empty rule set as `FLAGGED_FOR_REVIEW`.

**Q7: How does PlacementOS ensure tamper-proof No Objection Certificates?**  
**A:** When CDPC staff approve an internship, `NocVerificationService.java` generates a cryptographically signed HMAC-SHA256 token linked to the student and request ID. `NocPdfGeneratorService.java` uses Apache PDFBox and ZXing to embed this token into a QR code on the certificate. Anyone scanning the QR code is directed to the public portal (`/verify-noc/:token`), which reads directly from the university database to confirm legitimacy.

**Q8: Why are SQL migration files named V1, V2, etc., but Flyway is missing from your dependencies?**  
**A:** The migration files were authored following Flyway naming conventions in preparation for automated database versioning. However, `flyway-core` was omitted from `pom.xml`, and the applications currently rely on Hibernate's `spring.jpa.hibernate.ddl-auto: update`. While this works in development, we openly document this as technical debt that must be resolved by adding `flyway-core` and `flyway-mysql` before multi-node production deployment.

**Q9: If a student applies for a drive, how is eligibility evaluated against academic cutoffs?**  
**A:** In `DriveService.java` (`applyToDrive`), before saving an application, the system queries the student's `academic_profiles`, `semester_sgpas`, and `backlog_records`. It checks: `student.cgpa >= drive.minCgpa`, `student.tenthPercentage >= drive.minTenth`, `student.twelfthPercentage >= drive.minTwelfth`, and `activeBacklogs <= drive.maxBacklogs`. If any check fails, an `AppException` is thrown with HTTP 400 Bad Request, citing the specific failing criterion.

**Q10: Can a placement coordinator show favoritism by manually including their preferred students?**  
**A:** While coordinators have override capability (`OVERRIDDEN_INCLUDED` or `OVERRIDDEN_EXCLUDED`), they cannot do so anonymously or without accountability. `ApplicationService.java` requires a non-empty text justification for any override, and records the coordinator's institutional ID, action, and timestamp in the database. These records are visible in audit views and inspectable by the Head TPO.

**Q11: Why are demo accounts exempted from login rate limiting and lockout?**  
**A:** In `LoginRateLimiterService.java`, 5 failed attempts within 15 minutes trigger a 15-minute IP/account lockout. During presentations or evaluations, evaluators or audience members frequently mistype passwords or test edge cases. Exempting verified demo accounts prevents the entire presentation from grinding to a halt due to an accidental lockout.

**Q12: How are asynchronous tasks like resume processing and emails handled, and what are their limitations?**  
**A:** They are executed via Spring's internal bounded thread pool (`@Async` and `@EnableAsync`). Email notifications use `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)` so that mail server timeouts do not roll back database transactions. The limitation is that tasks are queued in server memory rather than a persistent external broker like RabbitMQ; a server crash during processing would require re-triggering the tasks.

**Q13: Why is Single Sign-On (SSO) not yet implemented between TPMS and Internship Approval?**  
**A:** TPMS and Internship Approval were developed as independently deployable services with isolated data stores (`tpms_db` and `internship_db`) to guarantee administrative autonomy. While both enforce the `@rknec.edu` domain, a unified OAuth2 / OIDC Identity Provider (such as Keycloak or Google Workspace SAML) has not yet been integrated. Users currently register separately in both modules.

**Q14: How does the system handle file upload security against malicious payloads disguised as PDFs?**  
**A:** In `FileUploadValidator.java`, the system performs magic-byte binary header inspection. It reads the first four bytes of the uploaded input stream and confirms they match `%PDF-` (`0x25, 0x50, 0x44, 0x46, 0x2D`). Simply renaming an `.exe`, `.sh`, or `.html` file to `.pdf` will fail validation and return HTTP 400 Bad Request.

**Q15: What evidence exists that the software actually works as claimed?**  
**A:** The codebase contains 41 automated backend tests across both microservices (21 in TPMS, 20 in Internship Approval) passing cleanly with 0 errors. Both React frontends compile to production bundles via Vite without errors. Furthermore, the 12-step automated PowerShell smoke test (`internship-approval/smoke-test.ps1`) verifies the full lifecycle from registration, rule evaluation, scam detection, and document access control to NOC generation against live HTTP endpoints.

---

## 13. Audit Disclosures & Evidentiary Gaps

To ensure absolute academic and professional honesty, the following items are disclosed as evidentiary gaps where no supporting code was found in the repository:
1. **No External Message Broker**: Despite mentions of "asynchronous queueing" in design docs, no Kafka, RabbitMQ, or Redis queue dependencies exist; all background processing relies on local Spring Boot thread executors.
2. **Missing CI/CD & Container Specs**: Despite references to DevOps pipelines in planning roadmaps, no `Dockerfile`, `docker-compose.yml`, or GitHub Actions workflows exist in the workspace.
3. **No WhatsApp or SMS Integration**: Pitched multi-channel messaging is completely absent from the codebase; only transactional email dispatch is implemented.
4. **No Automated Slot Booking or TPO Charts**: Roadmap features for interview calendar slot selection and visual TPO analytics charts have 0% implementation in the backend controllers and frontend views.
5. **No Performance Benchmarking Artifacts**: No JMeter scripts, Gatling tests, or formal latency measurement logs exist in the repository; any numerical throughput claims in early pitch decks are unverified.
