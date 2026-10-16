package com.placementos.internship.service;

import com.placementos.internship.dto.HrConfirmInfoDTO;
import com.placementos.internship.dto.HrConfirmResponseDTO;
import com.placementos.internship.dto.InternshipResponseDTO;
import com.placementos.internship.entity.*;
import com.placementos.internship.exception.AppException;
import com.placementos.internship.exception.ResourceNotFoundException;
import com.placementos.internship.repository.HrConfirmationRepository;
import com.placementos.internship.repository.InternshipRequestRepository;
import com.placementos.internship.repository.NotificationLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Set;

@Service
@Slf4j
public class HrConfirmationService {

    private final HrConfirmationRepository hrConfirmationRepository;
    private final InternshipRequestRepository requestRepository;
    private final NotificationLogRepository notificationLogRepository;
    private final JavaMailSender mailSender;
    private final InternshipSubmissionService submissionService;

    @Value("${app.notifications.enabled:false}")
    private boolean notificationsEnabled;

    @Value("${app.hr-confirmation.expiry-days:14}")
    private int expiryDays;

    @Value("${app.frontend.base-url:http://localhost:3001}")
    private String frontendBaseUrl;

    @Value("${app.notifications.mail-from:noreply@placementos.local}")
    private String mailFrom;

    private static final Set<String> DEMO_HR_DOMAINS = Set.of(
            "cleantech-alpha.com",
            "budgetworkshop.org",
            "speedyconsulting.com",
            "example.com",
            "test.com"
    );

    public HrConfirmationService(
            HrConfirmationRepository hrConfirmationRepository,
            InternshipRequestRepository requestRepository,
            NotificationLogRepository notificationLogRepository,
            @org.springframework.beans.factory.annotation.Autowired(required = false) JavaMailSender mailSender,
            InternshipSubmissionService submissionService
    ) {
        this.hrConfirmationRepository = hrConfirmationRepository;
        this.requestRepository = requestRepository;
        this.notificationLogRepository = notificationLogRepository;
        this.mailSender = mailSender;
        this.submissionService = submissionService;
    }

    /**
     * Generates a secure URL-safe 32-byte token.
     */
    public String generateRawToken() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * Computes SHA-256 hash of a raw token.
     */
    public static String hashToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) return "";
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 digest unavailable", e);
        }
    }

    /**
     * Masks student full name: "Rahul Sharma" -> "R*** S***"
     */
    public static String maskStudentName(String fullName) {
        if (fullName == null || fullName.isBlank()) return "S***";
        String[] parts = fullName.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (part.isEmpty()) continue;
            sb.append(Character.toUpperCase(part.charAt(0))).append("***");
            if (i < parts.length - 1) sb.append(" ");
        }
        return sb.toString();
    }

    private boolean isDemoHrAddress(String email) {
        if (email == null || email.isBlank()) return true;
        String lower = email.trim().toLowerCase();
        int atIndex = lower.indexOf('@');
        if (atIndex == -1) return true;
        String domain = lower.substring(atIndex + 1);
        return DEMO_HR_DOMAINS.contains(domain) || lower.contains("demo") || lower.contains("smoke");
    }

    /**
     * Process initial submission event (after transaction commit).
     */
    @Transactional
    public void processInitialSubmission(Long requestId) {
        InternshipRequest request = requestRepository.findByIdWithStudentAndUser(requestId)
                .orElseGet(() -> requestRepository.findById(requestId).orElse(null));
        if (request == null) return;

        HrConfirmation existing = hrConfirmationRepository.findByRequestId(requestId).orElse(null);
        if (existing != null) {
            return; // Already initialized
        }

        String hrEmail = request.getHrContactEmail();
        boolean isDemo = isDemoHrAddress(hrEmail);

        if (!notificationsEnabled || isDemo || mailSender == null) {
            HrConfirmation hrConf = HrConfirmation.builder()
                    .request(request)
                    .status(HrConfirmationStatus.NOT_SENT)
                    .resendCount(0)
                    .build();
            hrConfirmationRepository.save(hrConf);

            recordNotificationLog(hrEmail, "Please confirm an internship offer", "HR_CONFIRMATION_NOT_SENT",
                    notificationsEnabled ? "SKIPPED_DEMO" : "NOTIFICATIONS_DISABLED", null);
            log.info("[HR CONFIRM] Request #{} HR confirmation skipped (notificationsEnabled={}, isDemo={})", requestId, notificationsEnabled, isDemo);
            return;
        }

        dispatchHrConfirmationEmail(request, null, false);
    }

    /**
     * Dispatches an HR confirmation email, creates or updates the HrConfirmation row.
     */
    private void dispatchHrConfirmationEmail(InternshipRequest request, HrConfirmation existingRecord, boolean isResend) {
        String rawToken = generateRawToken();
        String tokenHash = hashToken(rawToken);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusDays(expiryDays > 0 ? expiryDays : 14);

        HrConfirmation hrConf;
        if (existingRecord != null) {
            hrConf = existingRecord;
            hrConf.setTokenHash(tokenHash);
            hrConf.setStatus(HrConfirmationStatus.PENDING);
            hrConf.setSentAt(now);
            hrConf.setExpiresAt(expiresAt);
            if (isResend && existingRecord.getStatus() != HrConfirmationStatus.NOT_SENT) {
                hrConf.setResendCount(hrConf.getResendCount() + 1);
            }
        } else {
            hrConf = HrConfirmation.builder()
                    .request(request)
                    .tokenHash(tokenHash)
                    .status(HrConfirmationStatus.PENDING)
                    .sentAt(now)
                    .expiresAt(expiresAt)
                    .resendCount(0)
                    .build();
        }

        hrConf = hrConfirmationRepository.save(hrConf);

        String hrEmail = request.getHrContactEmail();
        String subject = "Please confirm an internship offer";
        String notificationType = isResend ? "HR_CONFIRMATION_RESENT" : "HR_CONFIRMATION_SENT";

        String studentName = (request.getStudent() != null && request.getStudent().getUser() != null)
                ? request.getStudent().getUser().getName() : "Student";
        String maskedName = maskStudentName(studentName);

        String safeMaskedName = HtmlUtils.htmlEscape(maskedName);
        String safeCompany = HtmlUtils.htmlEscape(request.getCompanyName() != null ? request.getCompanyName() : "your organization");
        String safeRole = HtmlUtils.htmlEscape(request.getRoleTitle() != null ? request.getRoleTitle() : "Intern");
        String confirmUrl = frontendBaseUrl.replaceAll("/+$", "") + "/hr-confirm?token=" + rawToken;
        log.info("===============================================================================");
        log.info("[HR CONFIRMATION LINK GENERATED]");
        log.info("Recipient HR: {}", hrEmail);
        log.info("Verification URL: {}", confirmUrl);
        log.info("===============================================================================");

        String htmlBody = """
            <!DOCTYPE html>
            <html>
            <head><meta charset="UTF-8"></head>
            <body style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background-color: #f8fafc; margin: 0; padding: 24px;">
              <table width="100%%" border="0" cellspacing="0" cellpadding="0" style="max-width: 600px; margin: 0 auto; background-color: #ffffff; border: 1px solid #e2e8f0; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.05);">
                <tr>
                  <td style="background-color: #0E6E76; padding: 24px; text-align: left;">
                    <h1 style="color: #ffffff; margin: 0; font-size: 20px; font-weight: 700;">Internship Offer Verification</h1>
                  </td>
                </tr>
                <tr>
                  <td style="padding: 24px;">
                    <p style="font-size: 15px; color: #334155; margin-top: 0;">Dear HR Team,</p>
                    <p style="font-size: 15px; color: #334155; line-height: 1.5;">
                      A student (<strong>%s</strong>) from Ramdeobaba University has submitted an internship offer letter for the role of <strong>%s</strong> at <strong>%s</strong>.
                    </p>
                    <p style="font-size: 14px; color: #475569; line-height: 1.5;">
                      As part of our academic verification process, we request you to confirm whether this internship offer was legitimately issued by your company.
                    </p>
                    <div style="text-align: center; margin: 28px 0;">
                      <a href="%s" style="background-color: #0E6E76; color: #ffffff; padding: 14px 28px; text-decoration: none; border-radius: 8px; font-weight: 700; font-size: 14px; display: inline-block;">
                        Confirm or Deny Offer
                      </a>
                    </div>
                    <p style="font-size: 12px; color: #94a3b8; line-height: 1.4;">
                      This link is valid for %d days and can only be used once. If your email scanner opens links, no status will be changed until you explicitly click an option on the web page.
                    </p>
                    <hr style="border: none; border-top: 1px solid #e2e8f0; margin: 24px 0;" />
                    <p style="font-size: 12px; color: #94a3b8; margin-bottom: 0;">Career Development &amp; Placement Cell (CDPC) • Ramdeobaba University</p>
                  </td>
                </tr>
              </table>
            </body>
            </html>
            """.formatted(safeMaskedName, safeRole, safeCompany, HtmlUtils.htmlEscape(confirmUrl), expiryDays > 0 ? expiryDays : 14);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            String sender = (mailFrom != null && !mailFrom.isBlank()) ? mailFrom.trim() : "noreply@placementos.local";
            helper.setFrom(sender);
            helper.setTo(hrEmail);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);

            recordNotificationLog(hrEmail, subject, notificationType, "SUCCESS", null);
            log.info("[HR CONFIRM] Sent HR confirmation email to '{}' for Request #{} (Resend: {})", hrEmail, request.getId(), isResend);
        } catch (Exception e) {
            log.warn("[HR CONFIRM] Could not deliver email via SMTP to '{}' for Request #{}: {}. Status remains PENDING so the link can still be used.", hrEmail, request.getId(), e.getMessage());
            recordNotificationLog(hrEmail, subject, notificationType, "FAILED", e.getMessage());
        }
    }

    /**
     * Public GET /api/public/hr-confirm/{token}
     * Must NEVER change state so email scanners do not confirm anything.
     * Returns identical shape for invalid/expired/used tokens.
     */
    @Transactional(readOnly = true)
    public HrConfirmInfoDTO getPublicConfirmationInfo(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return genericInvalidResponse();
        }

        String tokenHash = hashToken(rawToken.trim());
        HrConfirmation conf = hrConfirmationRepository.findByTokenHash(tokenHash).orElse(null);

        if (conf == null || conf.getStatus() != HrConfirmationStatus.PENDING) {
            return genericInvalidResponse();
        }

        if (conf.getExpiresAt() != null && LocalDateTime.now().isAfter(conf.getExpiresAt())) {
            return genericInvalidResponse();
        }

        InternshipRequest request = conf.getRequest();
        if (request == null) {
            return genericInvalidResponse();
        }

        String studentName = (request.getStudent() != null && request.getStudent().getUser() != null)
                ? request.getStudent().getUser().getName() : "Student";

        return HrConfirmInfoDTO.builder()
                .valid(true)
                .studentMaskedName(maskStudentName(studentName))
                .companyName(request.getCompanyName())
                .roleTitle(request.getRoleTitle())
                .message("Please confirm whether your organization issued this internship offer.")
                .build();
    }

    private HrConfirmInfoDTO genericInvalidResponse() {
        return HrConfirmInfoDTO.builder()
                .valid(false)
                .studentMaskedName(null)
                .companyName(null)
                .roleTitle(null)
                .message("This confirmation link is invalid, has expired, or has already been used.")
                .build();
    }

    /**
     * Public POST /api/public/hr-confirm/{token}
     */
    @Transactional
    public HrConfirmResponseDTO recordPublicResponse(String rawToken, String answer, String clientIp) {
        if (rawToken == null || rawToken.isBlank()) {
            return HrConfirmResponseDTO.builder()
                    .success(false)
                    .message("Confirmation link is missing or invalid.")
                    .build();
        }

        String tokenHash = hashToken(rawToken.trim());
        HrConfirmation conf = hrConfirmationRepository.findByTokenHash(tokenHash).orElse(null);

        if (conf == null) {
            return HrConfirmResponseDTO.builder()
                    .success(false)
                    .message("This confirmation link is invalid, has expired, or has already been used.")
                    .build();
        }

        // Single use guard
        if (conf.getStatus() != HrConfirmationStatus.PENDING) {
            return HrConfirmResponseDTO.builder()
                    .success(false)
                    .status(conf.getStatus().name())
                    .message("A response has already been recorded for this confirmation request.")
                    .build();
        }

        // Expiry check
        if (conf.getExpiresAt() != null && LocalDateTime.now().isAfter(conf.getExpiresAt())) {
            conf.setStatus(HrConfirmationStatus.EXPIRED);
            hrConfirmationRepository.save(conf);

            recordNotificationLog(conf.getRequest() != null ? conf.getRequest().getHrContactEmail() : "unknown",
                    "HR Confirmation Link Expired", "HR_CONFIRMATION_EXPIRED", "EXPIRED", "Attempted submission after expiry");

            return HrConfirmResponseDTO.builder()
                    .success(false)
                    .status("EXPIRED")
                    .message("This confirmation link has expired.")
                    .build();
        }

        boolean isYes = "YES".equalsIgnoreCase(answer != null ? answer.trim() : "");
        HrConfirmationStatus newStatus = isYes ? HrConfirmationStatus.CONFIRMED : HrConfirmationStatus.DENIED;

        conf.setStatus(newStatus);
        conf.setRespondedAt(LocalDateTime.now());
        conf.setResponderIp(clientIp);
        conf = hrConfirmationRepository.save(conf);

        String hrEmail = conf.getRequest() != null ? conf.getRequest().getHrContactEmail() : "unknown";
        String notificationType = isYes ? "HR_CONFIRMED" : "HR_DENIED";

        recordNotificationLog(hrEmail, "HR Response: " + newStatus.name(), notificationType, "RECORDED",
                "Responder IP: " + clientIp);

        log.info("[HR CONFIRM] Recorded response '{}' for Request #{} from IP '{}'", newStatus,
                conf.getRequest() != null ? conf.getRequest().getId() : null, clientIp);

        return HrConfirmResponseDTO.builder()
                .success(true)
                .status(newStatus.name())
                .message(isYes
                        ? "Thank you! You have confirmed that this internship offer was issued by your organization."
                        : "Thank you. You have recorded that this offer was NOT issued by your organization.")
                .build();
    }

    /**
     * CDPC Staff action: Resend HR confirmation email.
     * Allowed only for PENDING or EXPIRED, max 3 resends.
     */
    @Transactional
    public InternshipResponseDTO resendHrEmail(Long requestId, User actioner) {
        InternshipRequest request = requestRepository.findByIdWithStudentAndUser(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Internship request not found with id: " + requestId));

        HrConfirmation conf = hrConfirmationRepository.findByRequestId(requestId).orElse(null);

        if (conf == null) {
            conf = HrConfirmation.builder()
                    .request(request)
                    .status(HrConfirmationStatus.NOT_SENT)
                    .resendCount(0)
                    .build();
            conf = hrConfirmationRepository.save(conf);
        }

        if (conf.getStatus() != HrConfirmationStatus.PENDING &&
            conf.getStatus() != HrConfirmationStatus.EXPIRED &&
            conf.getStatus() != HrConfirmationStatus.NOT_SENT) {
            throw new AppException("Cannot resend HR email when confirmation is already " + conf.getStatus(), HttpStatus.BAD_REQUEST);
        }

        if (conf.getResendCount() >= 3) {
            throw new AppException("Maximum limit of 3 resends has already been reached for this request.", HttpStatus.BAD_REQUEST);
        }

        dispatchHrConfirmationEmail(request, conf, true);

        log.info("[HR CONFIRM] Staff '{}' (ID: {}) initiated resend #{} for Request #{}",
                actioner != null ? actioner.getEmail() : "admin",
                actioner != null ? actioner.getId() : null,
                conf.getResendCount(),
                requestId);

        return submissionService.getRequestById(requestId);
    }

    private void recordNotificationLog(String recipient, String subject, String type, String status, String error) {
        try {
            NotificationLog logEntry = NotificationLog.builder()
                    .recipientEmail(recipient != null ? recipient : "unknown")
                    .subject(subject != null ? subject : "HR Confirmation")
                    .notificationType(type)
                    .status(status)
                    .errorMessage(error)
                    .build();
            notificationLogRepository.save(logEntry);
        } catch (Exception e) {
            log.error("[HR CONFIRM AUDIT] Failed to save NotificationLog entry: {}", e.getMessage());
        }
    }
}
