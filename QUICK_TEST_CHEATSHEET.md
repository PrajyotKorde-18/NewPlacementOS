# PlacementOS — Quick Copy-Paste Testing Cheatsheet

Use this document during manual testing to quickly copy and paste test data, credentials, and sample job descriptions into the UI.

---

## 1. Quick Copy-Paste Credentials

| App | URL | Role | Email | Password | Roll No / Staff ID | Primary Capabilities |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **TPMS** | `http://localhost:3000` | Coordinator | `coord_user@rknec.edu` | `Coord@12345` | `TPO-OFFICER-01` *(Optional)* | Create Drives, AI JD Extraction, Rank Candidates, Purge Data |
| **TPMS** | `http://localhost:3000` | Student | `student1@rknec.edu` | `Student@12345` | `2022CS099` | Extended Profile, Upload Resume, Apply to Drives, View Feedback |
| **Internship** | `http://localhost:3001` | CDPC Admin | `admin@rknec.edu` | `Admin@12345` | N/A | Review Queue, Approve/Reject Offers, Edit Policy Rules |
| **Internship** | `http://localhost:3001` | Student | `student1@rknec.edu` | `Student@12345` | `2022CS099` | Submit Offer PDF & Details, Policy Rule Check |

---

## 2. Environment Variables Setup (PowerShell)

```powershell
$env:JWT_SECRET="b5fdcda6d2fac77740f1edac9926630fedb032c696a707cf81438282219d09f74"
$env:SPRING_DATASOURCE_USERNAME="root"
$env:SPRING_DATASOURCE_PASSWORD="Arjun"
$env:BOOTSTRAP_ADMIN_EMAIL="admin@rknec.edu"
$env:BOOTSTRAP_ADMIN_PASSWORD="Admin@12345"
$env:BOOTSTRAP_COORDINATOR_EMAIL="coord_user@rknec.edu"
$env:BOOTSTRAP_COORDINATOR_PASSWORD="Coord@12345"
$env:GROQ_API_KEY="your_groq_api_key_here"
```

---

## 3. Test Case 1: Create Placement Drive & AI JD Extraction (TPMS)

### Navigation
1. Go to **`http://localhost:3000`**.
2. Log in as Coordinator: `coord_user@rknec.edu` / `Coord@12345`.
3. Click **Create Drive & Extract JD** in top navbar.

### Form Inputs
- **Company Name**: `Acme Technologies`
- **Role Title**: `Full Stack Engineer`
- **Available Vacancies**: `5`

### Sample Job Description (Copy & Paste Into Text Area)
```text
We are seeking a highly motivated Full Stack Engineer to join our core engineering team. The candidate will build scalable web applications using Java, Spring Boot, React, and MySQL.

Key Responsibilities:
- Design and develop backend microservices and RESTful APIs using Spring Boot and Java.
- Build modern responsive user interfaces using React and Tailwind CSS.
- Optimize database schemas and write complex SQL queries in MySQL.

Requirements:
- Bachelor's degree in Computer Science or Information Technology with minimum 7.5 CGPA.
- Strong proficiency in Java, React, JavaScript, Spring Boot, and MySQL.
- Minimum 70% in 10th and 12th standard examinations.
- Maximum 1 active backlog allowed.
```

### Execution Steps
1. Click **Extract Criteria via AI**.
2. **Verify Output**: Criteria fields (`React`, `Spring Boot`, `Java`, `MySQL`, Min CGPA: `7.5`, 10th/12th: `70%`) auto-fill accurately.
3. Click **Publish Placement Drive**.

---

## 4. Test Case 2: Extended Student Profile & PDF Resume Extraction (TPMS)

### Navigation
1. Open **`http://localhost:3000`**.
2. Log in as Student: `student1@rknec.edu` / `Student@12345`.
3. Go to **Profile / Resume**.

### Form Inputs
- **College Roll Number**: `2022CS099` (or `2026CS101`)
- **Phone Number**: `9876543210`
- **CGPA**: `8.2`
- **10th Percentage**: `85.0`
- **12th Percentage**: `82.0`
- **Branch**: `Computer Science & Engineering`
- **Active Backlogs**: `0`

### Execution Steps
1. Drag & drop a PDF resume file.
2. Click **Upload & Extract Skills**.
3. **Verify Output**: Extracted skills (parsed from Summary, Achievements, and Skills sections) display as interactive tags on profile.

---

## 5. Test Case 3: Apply to Drive & View Score Breakdown (TPMS)

### Navigation & Steps
1. Navigate to **Placement Drives Overview**.
2. Click **Apply Now** on `Acme Technologies — Full Stack Engineer`.
3. Go to **My Applications** tab.
4. **Verify Output**: Application appears with status (`SHORTLISTED` / `REJECTED`) and score breakdown feedback.

---

## 6. Test Case 4: Candidate Leaderboard & Ranking (TPMS - Coordinator)

### Navigation & Steps
1. Log in as Coordinator (`coord_user@rknec.edu` / `Coord@12345`) at `http://localhost:3000`.
2. On **Placement Drives Overview**, click on `Acme Technologies`.
3. Click **Calculate & Rank Candidates**.
4. **Verify Output**: Candidate leaderboard ranks applications with full match score breakdown.

---

## 7. Test Case 5: Administrative Database Purge (TPMS - Coordinator)

### Navigation & Steps
1. Log in as Coordinator at `http://localhost:3000`.
2. Click **Admin Controls** / **Database Reset**.
3. Click **Purge All Student Data & Company Drives**.
4. **Verify Output**: Confirmation toast appears; drives and student records are deleted while coordinator accounts remain active.

---

## 8. Test Case 6: Internship Offer Letter Submission (Internship Approval)

### Navigation & Form Inputs
1. Go to **`http://localhost:3001`** (Student Portal).
2. Enter Details:
   - **Company Name**: `CBL Solutions`
   - **Role Title**: `Software Engineer Intern`
   - **Duration (Weeks)**: `12`
   - **Monthly Stipend (₹)**: `25000`
   - **HR Contact Email**: `hr@cbl.com`
   - **Offer Letter PDF**: Upload sample PDF.
3. Click **Submit Offer for Verification**.
4. **Verify Output**: Instant compliance audit displays **APPROVED**.

---

## 9. Test Case 7: CDPC Review Queue & Policy Rule Customization (Internship Approval)

### Navigation & Steps
1. Go to **`http://localhost:3001`**.
2. Click **CDPC Coordinator** and log in as Admin (`admin@rknec.edu` / `Admin@12345`).
3. In **Review Queue**, inspect submitted offer and click **Approve** or **Reject**.
4. Switch to **Policy Rules** tab to inspect or edit stipend/duration thresholds.
