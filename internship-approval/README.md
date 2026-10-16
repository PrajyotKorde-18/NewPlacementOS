# Internship Approval System

A gatekeeping workflow and verification portal for college student internships.

---

## Environment Variables Configuration

Configure the following variables in `.env` (or pass as environment variables) based on `.env.example`:

| Variable | Description | Default |
|---|---|---|
| `SPRING_DATASOURCE_URL` | MySQL JDBC URL | `jdbc:mysql://localhost:3306/internship_db` |
| `SPRING_DATASOURCE_USERNAME` | MySQL Username | `root` |
| `SPRING_DATASOURCE_PASSWORD` | MySQL Password | - |
| `JWT_SECRET` | 256-bit secret key for JWT signing | - |
| `JWT_EXPIRATION_MS` | JWT validity duration (ms) | `86400000` (24h) |
| `ALLOWED_EMAIL_DOMAIN` | Allowed student email domain | `rknec.edu` |
| `BOOTSTRAP_ADMIN_EMAIL` | Initial CDPC Admin email | `admin@rknec.edu` |
| `BOOTSTRAP_ADMIN_PASSWORD` | Initial CDPC Admin password | - |
| `NOC_VERIFICATION_SECRET` | HMAC-SHA256 secret key for tamper-proof QR NOCs | - |
| `VERIFICATION_BASE_URL` | Public verification frontend URL | `http://localhost:3001/verify-noc` |
| `NOTIFICATIONS_ENABLED` | Master switch for email notifications | `false` |
| `MAIL_HOST` | SMTP server host (e.g. MailHog `localhost`) | `localhost` |
| `MAIL_PORT` | SMTP port (e.g. MailHog `1025`) | `1025` |
| `MAIL_USERNAME` | SMTP username (optional for MailHog) | - |
| `MAIL_PASSWORD` | SMTP password (optional for MailHog) | - |
| `MAIL_SMTP_AUTH` | Enable SMTP authentication | `false` |
| `MAIL_SMTP_STARTTLS` | Enable STARTTLS | `false` |
| `MAIL_FROM` | Outgoing sender address | `noreply@placementos.local` |
| `FRONTEND_BASE_URL` | Base URL for links sent in emails | `http://localhost:3001` |
| `HR_CONFIRMATION_EXPIRY_DAYS` | Validity period (in days) for HR confirmation links | `14` |

---

## HR Confirmation Feature

When a student submits an internship offer:
1. An email is dispatched asynchronously (`AFTER_COMMIT`) to the provided HR contact email address.
2. The email contains a link: `${FRONTEND_BASE_URL}/hr-confirm?token=<32_byte_secure_token>`.
3. The backend stores **only the SHA-256 hash** of the token in the `hr_confirmations` table.
4. The HR contact opens the public `/hr-confirm` page (no login required) which displays the student's **masked name** (e.g. `R*** S***`), company, and role.
5. The contact can respond with either:
   - **"Yes, we issued this offer"** -> Records `CONFIRMED`.
   - **"No, this is not ours"** -> Records `DENIED`.
6. CDPC reviewers see the status badge (`Confirmed`, `Denied`, `Pending`, `Expired`, `Not sent`) in the review queue.
7. If **DENIED**, a prominent critical flag is displayed on the request card.
8. It is purely an informational signal: it **never** automatically approves or rejects any submission, and never automatically ticks reviewer checklists.
9. CDPC staff can use the **Resend HR Email** button (up to 3 resends) if status is `PENDING` or `EXPIRED`.

---

## Testing with MailHog

1. **Start MailHog**:
   MailHog runs an SMTP server on port `1025` and a web UI on port `8025`:
   ```bash
   mailhog
   ```
   Open `http://localhost:8025` in your browser.

2. **Configure Environment**:
   Ensure `internship-approval/backend/.env` has:
   ```env
   NOTIFICATIONS_ENABLED=true
   MAIL_HOST=localhost
   MAIL_PORT=1025
   MAIL_SMTP_AUTH=false
   MAIL_SMTP_STARTTLS=false
   FRONTEND_BASE_URL=http://localhost:3001
   ```

3. **Submit an Offer as Student**:
   - Log into the Student Portal at `http://localhost:3001` as the student.
   - Submit an internship offer form, providing a real or test HR email (e.g. `recruiter@techcorp-demo.com`).

4. **Open MailHog**:
   - Navigate to `http://localhost:8025`.
   - Locate the incoming email with subject: *"Please confirm an internship offer"*.
   - View the email body showing the masked student name, company, role, and the confirmation button/link.

5. **Confirm Offer**:
   - Click the link or open the URL in your browser: `http://localhost:3001/hr-confirm?token=...`.
   - Click **"Yes, we issued this offer"** (or **"No, this is not ours"**).
   - Verify the thank-you confirmation screen appears.

6. **Verify CDPC Review Queue**:
   - Log into the CDPC review queue at `http://localhost:3001` as CDPC admin/staff.
   - Observe the updated HR confirmation badge on the student's request card:
     - Shows **"HR Confirmed"** (green badge) if confirmed.
     - Shows **"HR Denied Offer"** (red badge with prominent critical alert banner) if denied.
