# PlacementOS — End-to-End Manual & API Testing Guide

This guide provides comprehensive, step-by-step instructions to run, navigate, and test all features of **PlacementOS** (comprising the **TPMS** and **Internship Approval** microservices).

---

## 1. System Overview & Architecture

PlacementOS consists of two decoupled, independent full-stack microservices:

| Service / Component | Port | Frontend URL | Backend API Base URL | Primary Role |
| :--- | :--- | :--- | :--- | :--- |
| **TPMS** (Placement Management System) | `3000` (FE) / `8081` (BE) | `http://localhost:3000` | `http://localhost:8081/api` | Resume Parsing, AI JD Criteria Extraction, Candidate Match Scoring & Ranking, Drive Management, Admin Purge |
| **Internship Approval & Policy Engine** | `3001` (FE) / `8082` (BE) | `http://localhost:3001` | `http://localhost:8082/api` | Student Offer Letter Submissions, Automated Rule Engine Verification, CDPC Review Queue, Dynamic Policy Management |

---

## 2. Environment Setup & Service Startup

### Environment Variables
Before running backends, ensure the following environment variables are set in your terminal session:

```powershell
$env:JWT_SECRET="<generate-with-openssl-rand-hex-32-or-use-a-secure-32-byte-hex-string>"
$env:SPRING_DATASOURCE_USERNAME="root"
$env:SPRING_DATASOURCE_PASSWORD="<your_local_mysql_password>"
$env:BOOTSTRAP_ADMIN_EMAIL="admin@rknec.edu"
$env:BOOTSTRAP_ADMIN_PASSWORD="Admin@12345"
$env:BOOTSTRAP_COORDINATOR_EMAIL="coord_user@rknec.edu"
$env:BOOTSTRAP_COORDINATOR_PASSWORD="Coord@12345"
$env:GROQ_API_KEY="<your_groq_api_key_or_leave_empty_for_mock_mode>"
```

### Starting the Applications

Open 4 separate terminal windows:

#### Terminal 1: TPMS Backend
```powershell
Set-Location "c:\Users\Lenovo\Downloads\PlacementOS\tpms\backend"
mvn spring-boot:run
```

#### Terminal 2: Internship Approval Backend
```powershell
Set-Location "c:\Users\Lenovo\Downloads\PlacementOS\internship-approval\backend"
mvn spring-boot:run
```

#### Terminal 3: TPMS Frontend
```powershell
Set-Location "c:\Users\Lenovo\Downloads\PlacementOS\tpms\frontend"
npm run dev
```

#### Terminal 4: Internship Approval Frontend
```powershell
Set-Location "c:\Users\Lenovo\Downloads\PlacementOS\internship-approval\frontend"
npm run dev
```

---

## 3. Pre-Configured Test Accounts

> **Important Domain Policy**: Both microservices enforce campus email domain validation. All registered or logged-in users **must use `@rknec.edu`** email addresses.

| System | Role | Email | Password | Roll No / Staff ID | Primary Capabilities |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **TPMS** | Coordinator | `coord_user@rknec.edu` | `Coord@12345` | `TPO-OFFICER-01` *(Optional on Reg)* | Create Placement Drives, AI JD Parsing, Rank Candidates, Purge System Data |
| **TPMS** | Student | `student1@rknec.edu` | `Student@12345` | `2022CS099` *(Required for Student)* | Extended Profile, PDF Resume Parsing, Apply to Drives, View Rejection/Match Feedback |
| **Internship** | CDPC Admin | `admin@rknec.edu` | `Admin@12345` | N/A | Review Queue Audit, Approve/Reject Offers, Customize Dynamic Policy Rules |
| **Internship** | Student | `student1@rknec.edu` | `Student@12345` | `2022CS099` | Submit Offer Letters (PDF + Details), Real-time Policy Rule Compliance Checking |

> **Registration Tip**: In the **Create Account** modal:
> - Selecting **Student Candidate** displays **College Roll Number** (required, e.g. `2022CS099`).
> - Selecting **TPO / Coordinator** displays **Staff / Officer ID** (optional, defaults to `TPO-OFFICER-01` if left blank).

---

## 4. Comprehensive End-to-End Test Scenarios

### Scenario A: Placement Drive Creation & AI JD Extraction (TPMS - Coordinator)

1. Open **`http://localhost:3000`** in your browser.
2. Click **Sign In / Register** in the top navigation bar.
3. Log in using **Coordinator Credentials**:
   - **Email**: `coord_user@rknec.edu`
   - **Password**: `Coord@12345`
4. Click **Create New Drive** in the navbar/dashboard.
5. **Test Automated JD Criteria Extraction**:
   - Paste a sample Job Description into the text box:
     ```text
     We are hiring a Full Stack Developer proficient in React, Spring Boot, Java, and MySQL. Minimum 7.5 CGPA required. 10th and 12th minimum 70%. Maximum 1 active backlog.
     ```
   - Click **Extract Criteria via AI**.
   - Verify that fields (Role, Skills: `React, Spring Boot, Java, MySQL`, Min CGPA: `7.5`, 10th/12th %: `70%`) auto-populate accurately.
6. Click **Publish Placement Drive**.
7. Confirm the drive appears on the **Placement Drives Overview** card grid.

---

### Scenario B: Extended Student Profile & PDF Resume Skill Parsing (TPMS - Student)

1. Open **`http://localhost:3000`**. Log out if currently logged in as Coordinator.
2. Log in with **Student Credentials**:
   - **Email**: `student1@rknec.edu`
   - **Password**: `Student@12345`
3. Click **Profile / Resume** (or Academic Setup).
4. **Test Extended Profile & Roll Number Entry**:
   - **Step 1 (Identity)**: Enter **College Roll Number**: `2022CS099` (or `2026CS101`), Phone Number (`9876543210`). *(Note: Default system `collegeId` is auto-generated as `STUDENT1` from the email prefix upon registration)*.
   - **Step 2 (School & Degree)**: Fill in CGPA (`8.2`), 10th % (`85.0`), 12th % (`82.0`), Branch (`Computer Science & Engineering`).
   - **Step 3 (SGPAs & Backlogs)**: Add semester SGPA history (Sem 1: `8.0`, Sem 2: `8.5`) and active backlogs (`0`).
   - Click **Save Academic Profile**.
5. **Test PDF Resume Parsing & Fallback Skill Matching**:
   - Drag & drop a PDF resume file.
   - Click **Upload & Extract Skills**.
   - Verify that skills extracted from **Summary**, **Achievements**, and **Skills** sections are parsed, displayed as tags, and persisted to the profile.

---

### Scenario C: Placement Drive Application & Score Feedback (TPMS - Student)

1. Stay logged in as **Student** (`student1@rknec.edu`).
2. Navigate to **Placement Drives**.
3. Select an active placement drive (e.g. `Full Stack Developer`) and click **Apply Now**.
4. Go to **My Applications**.
5. Verify application status (`SHORTLISTED` or `REJECTED`).
6. Click **View Match Breakdown & Feedback** to inspect:
   - Match Score percentage calculation.
   - Deterministic rejection reasons (if ineligible due to CGPA, backlogs, or missing skills).

---

### Scenario D: Candidate Leaderboard & Automated Shortlist Matrix (TPMS - Coordinator)

1. Log back in as **Coordinator** (`coord_user@rknec.edu`).
2. Navigate to **Placement Drives Overview** and click on the published drive.
3. Click **Calculate & Rank Candidates**.
4. Observe the candidate leaderboard matrix:
   - Ordered by composite match score.
   - Breakdown of Skill Match %, Qualification Match, and Backlog Eligibility status.

---

### Scenario E: Administrative Database Purge / Data Reset (TPMS - Coordinator)

1. Log in as **Coordinator** (`coord_user@rknec.edu`).
2. Click **Admin Controls** or **Database Reset** in the navigation header.
3. Click **Purge All Student Data & Company Drives**.
4. Confirm the prompt.
5. **Verify System Reset**:
   - Confirm deletion of all student user records, academic profiles, uploaded resume versions, placement drives, and application entries.
   - Verify that Coordinator user accounts remain intact and functional.

---

### Scenario F: Internship Offer Submission & Rule Engine (Internship System - Student)

1. Open **`http://localhost:3001`** in your browser.
2. Select **Student Portal**.
3. Complete the **Offer Letter Submission Form**:
   - **Company Name**: `Acme Solutions`
   - **Role Title**: `Software Engineering Intern`
   - **Duration (Weeks)**: `12`
   - **Monthly Stipend (₹)**: `25000`
   - **HR Contact Email**: `hr@acme.com`
   - **Offer Letter PDF**: Upload any sample PDF.
4. Click **Submit Offer for Verification**.
5. Observe the immediate **Policy Engine Audit Result**:
   - Green badge **APPROVED** (stipend >= ₹15,000 threshold and duration within allowed limits).

---

### Scenario G: CDPC Review Queue & Dynamic Policy Rules (Internship System - Admin)

1. Open **`http://localhost:3001`**.
2. Click **CDPC Coordinator** in top navbar and log in:
   - **Email**: `admin@rknec.edu`
   - **Password**: `Admin@12345`
3. View **Review Queue**:
   - Inspect incoming student offer letters, PDF attachments, and rule evaluation breakdown.
   - Click **Approve** or **Reject** with comments.
4. View **Policy Rules**:
   - Edit minimum stipend requirement (e.g. increase threshold to `₹30,000`).
5. Return to Student Portal (`http://localhost:3001`) and submit a new offer with `₹25,000` stipend.
6. Verify that the policy engine now flags or rejects the submission based on the updated rule set.

---

## 5. API Testing via cURL / Command Line

### TPMS Services (`http://localhost:8081`)

#### Coordinator / Student Authentication
```bash
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"coord_user@rknec.edu","password":"Coord@12345"}'
```

#### Fetch All Active Placement Drives
```bash
curl -X GET http://localhost:8081/api/drives
```

#### Admin Data Cleanup Endpoint (Coordinator Token Required)
```bash
curl -X POST http://localhost:8081/api/admin/data/cleanup \
  -H "Authorization: Bearer <COORDINATOR_JWT_TOKEN>"
```

### Internship System Services (`http://localhost:8082`)

#### CDPC Admin Login
```bash
curl -X POST http://localhost:8082/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@rknec.edu","password":"Admin@12345"}'
```

#### Fetch Internship Submissions Queue
```bash
curl -X GET http://localhost:8082/api/internships \
  -H "Authorization: Bearer <ADMIN_JWT_TOKEN>"
```

---

## 6. Troubleshooting & FAQs

| Symptom / Issue | Root Cause | Solution |
| :--- | :--- | :--- |
| `Email domain not allowed` error on registration | non-campus email used | Ensure registration email ends strictly with `@rknec.edu`. |
| `500 Internal Server Error` on startup | Environment variables missing | Verify `$env:SPRING_DATASOURCE_PASSWORD` and `$env:JWT_SECRET` are exported prior to starting Spring Boot. |
| Groq LLM extraction fails or returns generic fallback | Invalid API Key or network issue | Verify `$env:GROQ_API_KEY` is set and active. Fallback heuristic keyword parsing will automatically kick in if LLM service is unreachable. |
| Port 8081 / 8082 / 3000 / 3001 already in use | Previous process hanging | Run PowerShell command: `Stop-Process -Id (Get-NetTCPConnection -LocalPort <PORT>).OwningProcess -Force`. |
