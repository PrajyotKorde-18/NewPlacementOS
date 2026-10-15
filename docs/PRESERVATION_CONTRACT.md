# PlacementOS — Quick-Login & Demo Authentication Preservation Contract

> **MANDATORY SYSTEM DIRECTIVE FOR ALL FUTURE CODEBASES, REFACTORINGS, AND FEATURE ADDITIONS**
>
> PlacementOS is an enterprise prototype and demonstration platform. The 1-click quick-login demo workflows, pre-seeded accounts, and authentication UI components documented herein constitute an immutable baseline contract. Under no circumstance may any future feature, migration, security rule, or styling change break, hide, or alter these demo capabilities.

---

## 1. Quick-Login Buttons & Associated Accounts

The following interactive buttons are permanently integrated into the frontends to ensure immediate evaluation without manual typing:

### A. TPMS (Training & Placement Management System — Port 3000)

| UI Location | Component File | Button Label / Text | Account Email | Account Role | Authentication Mechanism |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Landing Hero Card** (Unauthenticated) | `tpms/frontend/src/App.jsx` | `Student Candidate` (`candidate.student`) | `candidate.student@rknec.edu` | `STUDENT` | Calls `api.login()` directly via POST `/auth/login`, saves `tpms_token` & `tpms_user` in `localStorage`, navigates to `drives` tab. |
| **Landing Hero Card** (Unauthenticated) | `tpms/frontend/src/App.jsx` | `TPO / Coordinator` (`admin@rknec.edu`) | `admin@rknec.edu` | `ADMIN` / `COORDINATOR` | Calls `api.login()` directly via POST `/auth/login`, saves `tpms_token` & `tpms_user` in `localStorage`, navigates to `drives` tab. |
| **Auth Modal** (Sign In Mode) | `tpms/frontend/src/components/AuthModal.jsx` | `Student Login` (`candidate.student`) | `candidate.student@rknec.edu` | `STUDENT` | Triggers `handleDemoLogin()`, executes POST `/auth/login`, stores credentials in `localStorage`, closes modal, triggers `onAuthSuccess`. |
| **Auth Modal** (Sign In Mode) | `tpms/frontend/src/components/AuthModal.jsx` | `TPO / Admin` (`admin@rknec.edu`) | `admin@rknec.edu` | `ADMIN` / `COORDINATOR` | Triggers `handleDemoLogin()`, executes POST `/auth/login`, stores credentials in `localStorage`, closes modal, triggers `onAuthSuccess`. |

### B. Internship Approval & Anti-Scam Portal (Port 3001)

| UI Location | Component File | Button Label / Text | Account Email | Account Role | Authentication Mechanism |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Student Portal Segment** | `internship-approval/frontend/src/components/AuthPage.jsx` | `Rahul Sharma (Student)` with Arrow | `student1@rknec.edu` | `STUDENT` | Calls `handleQuickDemo()`, executes POST `/auth/login`, persists `internship_token` & `internship_user` in `localStorage`, triggers `onAuthSuccess`. |
| **CDPC Admin Segment** | `internship-approval/frontend/src/components/AuthPage.jsx` | `Sign In as CDPC Admin (admin@rknec.edu)` with Arrow | `admin@rknec.edu` | `CDPC_ADMIN` | Calls `handleQuickDemo()`, executes POST `/auth/login`, persists `internship_token` & `internship_user` in `localStorage`, triggers `onAuthSuccess`. |
| **Auth Modal Presets** | `internship-approval/frontend/src/components/AuthModal.jsx` | `Student Demo` | `student1@rknec.edu` | `STUDENT` | Calls `handleQuickLogin()`, pre-fills login inputs and switches mode to Sign In. |
| **Auth Modal Presets** | `internship-approval/frontend/src/components/AuthModal.jsx` | `CDPC Admin Demo` | `admin@rknec.edu` | `CDPC_ADMIN` | Calls `handleQuickLogin()`, pre-fills login inputs and switches mode to Sign In. |

---

## 2. Complete Demo Account Register (Emails and Roles Only)

The backends configure and seed these accounts automatically via Spring Boot's `demo` profile (`application-demo.yml`):

### TPMS Module (`tpms/backend`)
1. **`candidate.student@rknec.edu`** — Role: `STUDENT` (Primary quick-login student in TPMS UI)
2. **`student1@rknec.edu`** — Role: `STUDENT` (Testing guide & cross-module demo student)
3. **`admin@rknec.edu`** — Role: `ADMIN` (Primary quick-login administrator in TPMS UI)
4. **`coord_user@rknec.edu`** — Role: `COORDINATOR` (Original bootstrap coordinator from testing suite)

### Internship Approval Module (`internship-approval/backend`)
1. **`student1@rknec.edu`** — Role: `STUDENT` (Primary quick-login student "Rahul Sharma")
2. **`admin@rknec.edu`** — Role: `CDPC_ADMIN` (Primary quick-login CDPC Administrator)
3. *System Staff Role Support*: `CDPC_STAFF` can be granted or reviewed by `CDPC_ADMIN` through the role delegation workflow.

---

## 3. Login Screen Architecture & UI Structure

### A. TPMS UI Architecture
- **Navbar Button**: `id="login-btn"` ("Sign In / Register") opens `AuthModal.jsx`.
- **Hero State**: When `currentUser == null`, `App.jsx` renders a landing banner featuring:
  - "Instant 1-Click Demo Login" panel with `Student Candidate` and `TPO / Coordinator` shortcut cards.
  - "Sign In / Create Account" modal trigger button.
- **Modal Mode Switcher**:
  - `Sign In`: Includes email input (placeholder: `candidate.student@rknec.edu`), password input (`••••••••`), submit button (`#auth-submit-btn`), and the "Instant 1-Click Demo Login" grid.
  - `Create Account`: Exposes Full Name, Role Selector (`STUDENT` / `TPO`), College Roll Number (default `2022CS099`) or Staff ID (default `TPO-OFFICER-01`), Email restriction (`@rknec.edu`), and Password.

### B. Internship Approval UI Architecture
- **Entry Gatekeeper**: When `currentUser == null`, `App.jsx` renders `AuthPage.jsx` as a full-page view.
- **Portal Segmented Tabs**:
  - `Student Portal`: Shows title "Student Sign In", description, 1-Click demo button `Rahul Sharma (Student)`, email/password inputs, and bottom toggle link `"New Student? Register with @rknec.edu email"`.
  - `CDPC Admin`: Shows title "CDPC Coordinator Sign In", 1-Click demo button `Sign In as CDPC Admin (admin@rknec.edu)`, email/password inputs. No self-registration link is displayed on CDPC tab.

---

## 4. Token & LocalStorage Behavior

### A. TPMS Storage Contract
- **Token Key**: `tpms_token` (JWT string)
- **User Object Key**: `tpms_user` (JSON string serialized from backend auth response: `{ token, email, name, role, id }`)
- **HTTP Header**: Sent as `Authorization: Bearer <tpms_token>` on all authenticated API requests.
- **Route / Tab Mapping**:
  - Role `STUDENT`: Entitled to tabs `drives`, `academic-setup`, `resume-upload`, `profile`, `my-applications`.
  - Role `COORDINATOR` / `ADMIN` / `TPO`: Entitled to tabs `drives`, `create-drive`, candidate leaderboards, and action button `Purge Student DB` (`#nav-purge-db`).

### B. Internship Approval Storage Contract
- **Token Key**: `internship_token` (JWT string)
- **User Object Key**: `internship_user` (JSON string serialized from backend auth response: `{ token, email, name, role, id }`)
- **HTTP Header**: Sent as `Authorization: Bearer <internship_token>` on all requests.
- **Route / View Mapping**:
  - Role `STUDENT`: Renders `StudentSubmissionView` (Offer submission form, compliance badge, NOC download).
  - Role `CDPC_ADMIN` / `CDPC_STAFF`: Renders `CdpcReviewQueueView` (`cdpc-review` tab) and `CdpcPolicyRulesView` (`cdpc-rules` tab).

---

## 5. Hardcoded Values & Frontend Dependencies

The frontend modules rely on the following identifiers:
- **Campus Domain**: `@rknec.edu` strictly enforced by frontend validation and backend `jwtUtils.isAllowedDomain()`.
- **Demo Names**: "Rahul Sharma" (Internship), "Candidate Student" (TPMS Student), "System CDPC Admin" (Internship CDPC), "TPO Placement Admin" (TPMS Admin), "System Placement Coordinator" (TPMS Coordinator).
- **College Identifiers**:
  - Student Roll Numbers: `2022CS099`, `STU001`
  - Admin/Officer IDs: `ADMIN001`, `TPO-OFFICER-01`
- **UI Placeholders**: Email fields explicitly display demo addresses (`candidate.student@rknec.edu`, `student1@rknec.edu`, `admin@rknec.edu`).

---

## 6. The Six Golden Preservation Rules for All Future Changes

All developers, subagents, and automated workflows modifying PlacementOS must adhere strictly to these rules:

### Rule (a): Never Remove, Hide, or Restyle Quick-Login Buttons or Login Screens
The 1-click login buttons in `App.jsx`, `AuthModal.jsx`, and `AuthPage.jsx` must remain visible, directly clickable, and styled with their designated identifiers. Modifying layouts or adding authentication layers must preserve these shortcuts.

### Rule (b): Never Change Demo Emails, Passwords, Roles, or Names
Demo account credentials defined in `application-demo.yml` are permanent. Do not alter their email addresses, roles, names, or passwords. Any new security policy requiring complex passwords must ensure the demo passwords remain valid or are grandfathered in.

### Rule (c): Demo Accounts Must Be Seeded as Academically VERIFIED and Rate-Limit Exempt
1. **Academic Verification**: Demo student accounts must be seeded with:
   - High CGPA (minimum `8.5`) across at least two finalized semesters.
   - Secondary education scores: 10th percentage (`85.0%`) and 12th percentage (`82.0%`).
   - Active backlogs count of exactly `0`.
   - Branch: `Computer Science & Engineering`.
   - Skills: Core tech stack tags (`Java, Spring Boot, React, MySQL, JavaScript`).
   This ensures demo students remain immediately eligible to apply for any published placement drive without failing automated gatekeeper audits.
2. **Lockout Exemption**: All demo emails are hard-coded as exempt in `LoginRateLimiterService`. Failed attempts must never trigger HTTP 429 lockouts on demo accounts.

### Rule (d): Existing JWT Claims & Frontend Contract Must Remain Backward-Compatible
The JWT structure generated by `JwtUtils` and payload returned by `/auth/login` must always include:
- `token`: Bearer JWT string
- `email`: User email string
- `role`: Role string (`STUDENT`, `COORDINATOR`, `ADMIN`, `CDPC_ADMIN`, `CDPC_STAFF`)
- `name`: Full name string
- `id`: Numeric primary key ID
Any future JWT claims (e.g., department, permissions) must be additive.

### Rule (e): Standalone Module Independence
Each module (`tpms` and `internship-approval`) must continue to function and authenticate completely standalone. Neither frontend or backend may require the other module's database or server to be running in order to log in and evaluate its features.

### Rule (f): Post-Change Mandatory Preservation Check
After making ANY modification to either backend, database migrations, security configurations, or frontends:
1. Run all backend tests: `.\mvnw.cmd test`
2. Run both frontend builds: `npm run build`
3. Execute the preservation verification script:
   ```powershell
   powershell -ExecutionPolicy Bypass -File scripts/preservation-check.ps1
   ```
4. Verify that the script outputs an all-green PASS table and exits with code `0`.

---

## 7. Resilience Against Flyway Migrations & Database Resets

1. **Fresh Database Bootstrapping**:
   `DemoAccountSeeder` runs as a Spring `CommandLineRunner` with `@Order(20)` under `@Profile("demo")`. When a new database is initialized, all demo accounts, student profiles, academic profiles, and skills are seeded automatically on first boot.
2. **Flyway Migrations**:
   `DemoAccountSeeder` executes after Flyway and JPA DDL updates complete. It performs idempotent existence checks (`findByEmail`), guaranteeing that running database migrations never duplicates or corrupts existing demo accounts.
3. **Admin Data Cleanup Preservation Strategy**:
   In TPMS, the coordinator data purge endpoint (`POST /api/admin/data/cleanup`) clears transient placement drives, application entries, and temporary student uploads. To ensure the demo environment remains 100% operational after a database reset, `AdminDataCleanupController` explicitly invokes `demoAccountSeeder.seedDemoAccounts()` immediately after foreign key re-enabling. This guarantees that student and admin demo accounts, academic profiles, and verified status are immediately restored and ready for subsequent demonstration runs.
