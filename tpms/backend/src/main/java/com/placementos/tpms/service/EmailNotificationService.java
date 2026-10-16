package com.placementos.tpms.service;

import com.placementos.tpms.entity.Application;
import com.placementos.tpms.entity.Drive;
import com.placementos.tpms.entity.NotificationLog;
import com.placementos.tpms.entity.RoleProfile;
import com.placementos.tpms.repository.NotificationLogRepository;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationService {

    private final NotificationLogRepository notificationLogRepository;

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${app.notifications.enabled:false}")
    private boolean notificationsEnabled;

    @Value("${app.notifications.mail-from:noreply@placementos.local}")
    private String mailFrom;

    @Value("${app.frontend.base-url:http://localhost:3000}")
    private String frontendBaseUrl;

    public String escape(String input) {
        return HtmlUtils.htmlEscape(input != null ? input : "");
    }

    public void sendEmail(String to, String subject, String notificationType, String htmlBody) {
        if (!notificationsEnabled) {
            log.info("[EMAIL SKIPPED] Notifications disabled (app.notifications.enabled=false). Skipping email to '{}' [Type: {}]", to, notificationType);
            recordAudit(to, subject, notificationType, "SKIPPED", "Notifications disabled via app.notifications.enabled=false");
            return;
        }

        if (mailSender == null) {
            log.warn("[EMAIL SKIPPED] JavaMailSender bean not configured. Skipping email to '{}' [Type: {}]", to, notificationType);
            recordAudit(to, subject, notificationType, "SKIPPED", "JavaMailSender bean unavailable / no mail config");
            return;
        }

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, false, "UTF-8");
            helper.setFrom(mailFrom);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);

            mailSender.send(mimeMessage);
            recordAudit(to, subject, notificationType, "SENT", null);
            log.info("[EMAIL SENT] Successfully delivered email to '{}' [Type: {}]", to, notificationType);
        } catch (Exception ex) {
            log.warn("[EMAIL FAILED] SMTP error delivering email to '{}' [Type: {}]: {}", to, notificationType, ex.getMessage());
            String error = ex.getMessage() != null && ex.getMessage().length() > 500
                    ? ex.getMessage().substring(0, 500)
                    : ex.getMessage();
            recordAudit(to, subject, notificationType, "FAILED", error);
        }
    }

    private void recordAudit(String to, String subject, String notificationType, String status, String error) {
        try {
            NotificationLog logEntry = NotificationLog.builder()
                    .recipientEmail(to != null ? to : "unknown")
                    .subject(subject != null ? subject : "")
                    .notificationType(notificationType)
                    .status(status)
                    .errorMessage(error)
                    .build();
            notificationLogRepository.save(logEntry);
        } catch (Exception e) {
            log.error("[EMAIL AUDIT] Failed to save NotificationLog entry: {}", e.getMessage());
        }
    }

    public void sendDrivePublishedEmail(String studentEmail, String studentName, Drive drive, RoleProfile roleProfile) {
        String safeName = escape(studentName);
        String safeCompany = escape(drive.getCompanyName());
        String safeRole = escape(drive.getRoleTitle());
        String safeCtc = escape(drive.getCtc() != null ? drive.getCtc() : "As per industry standards");
        int vacancies = drive.getVacancies() != null ? drive.getVacancies() : 1;
        String driveUrl = escape(frontendBaseUrl);

        String subject = "New Placement Drive: " + drive.getCompanyName() + " (" + drive.getRoleTitle() + ")";

        String html = """
            <!DOCTYPE html>
            <html>
            <head><meta charset="UTF-8"></head>
            <body style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background-color: #f8fafc; margin: 0; padding: 24px;">
              <table width="100%%" border="0" cellspacing="0" cellpadding="0" style="max-width: 600px; margin: 0 auto; background-color: #ffffff; border: 1px solid #e2e8f0; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.05);">
                <tr>
                  <td style="background-color: #4338ca; padding: 24px; text-align: left;">
                    <h1 style="color: #ffffff; margin: 0; font-size: 20px; font-weight: 700;">PlacementOS — Campus Placement Drive</h1>
                  </td>
                </tr>
                <tr>
                  <td style="padding: 24px;">
                    <p style="font-size: 15px; color: #334155; margin-top: 0;">Dear %s,</p>
                    <p style="font-size: 15px; color: #334155; line-height: 1.5;">
                      A new campus placement drive has been announced for which you meet the preliminary academic eligibility criteria:
                    </p>
                    <div style="background-color: #f1f5f9; border-radius: 8px; padding: 16px; margin: 20px 0;">
                      <table width="100%%" cellpadding="6" cellspacing="0">
                        <tr><td width="35%%" style="color: #64748b; font-size: 13px;">Company:</td><td style="color: #0f172a; font-weight: 600; font-size: 14px;">%s</td></tr>
                        <tr><td style="color: #64748b; font-size: 13px;">Role:</td><td style="color: #0f172a; font-weight: 600; font-size: 14px;">%s</td></tr>
                        <tr><td style="color: #64748b; font-size: 13px;">Compensation / CTC:</td><td style="color: #0f172a; font-weight: 600; font-size: 14px;">%s</td></tr>
                        <tr><td style="color: #64748b; font-size: 13px;">Vacancies:</td><td style="color: #0f172a; font-weight: 600; font-size: 14px;">%d</td></tr>
                      </table>
                    </div>
                    <p style="text-align: center; margin: 28px 0;">
                      <a href="%s" style="background-color: #4338ca; color: #ffffff; padding: 12px 24px; text-decoration: none; border-radius: 6px; font-weight: 600; font-size: 14px; display: inline-block;">View Drive &amp; Apply</a>
                    </p>
                    <p style="font-size: 12px; color: #94a3b8; margin-bottom: 0;">This is an automated notification from Ramdeobaba University TPMS.</p>
                  </td>
                </tr>
              </table>
            </body>
            </html>
            """.formatted(safeName, safeCompany, safeRole, safeCtc, vacancies, driveUrl);

        sendEmail(studentEmail, subject, "DRIVE_PUBLISHED", html);
    }

    public void sendStudentShortlistedEmail(String studentEmail, String studentName, Drive drive, Application app, String slotBookingUrl) {
        String safeName = escape(studentName);
        String safeCompany = escape(drive.getCompanyName());
        String safeRole = escape(drive.getRoleTitle());
        int rank = app.getRankPosition() != null ? app.getRankPosition() : 1;
        String safeScore = app.getScore() != null ? String.format("%.1f", app.getScore()) : "N/A";

        String bookingLinkHtml = "";
        if (slotBookingUrl != null && !slotBookingUrl.isBlank()) {
            bookingLinkHtml = """
                <div style="background-color: #ecfdf5; border: 1px solid #10b981; border-radius: 8px; padding: 16px; margin: 20px 0; text-align: center;">
                  <p style="color: #065f46; font-weight: 600; margin: 0 0 10px 0;">Interview Slots Are Open!</p>
                  <a href="%s" style="background-color: #059669; color: #ffffff; padding: 10px 20px; text-decoration: none; border-radius: 6px; font-weight: 600; font-size: 13px; display: inline-block;">Book Your Interview Slot</a>
                </div>
                """.formatted(escape(slotBookingUrl));
        }

        String subject = "Congratulations! You are Shortlisted for " + drive.getCompanyName() + " (" + drive.getRoleTitle() + ")";

        String html = """
            <!DOCTYPE html>
            <html>
            <head><meta charset="UTF-8"></head>
            <body style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background-color: #f8fafc; margin: 0; padding: 24px;">
              <table width="100%%" border="0" cellspacing="0" cellpadding="0" style="max-width: 600px; margin: 0 auto; background-color: #ffffff; border: 1px solid #e2e8f0; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.05);">
                <tr>
                  <td style="background-color: #059669; padding: 24px; text-align: left;">
                    <h1 style="color: #ffffff; margin: 0; font-size: 20px; font-weight: 700;">Shortlist Confirmed!</h1>
                  </td>
                </tr>
                <tr>
                  <td style="padding: 24px;">
                    <p style="font-size: 15px; color: #334155; margin-top: 0;">Dear %s,</p>
                    <p style="font-size: 15px; color: #334155; line-height: 1.5;">
                      Congratulations! You have been successfully shortlisted for the next round of selection for <strong>%s</strong> (%s).
                    </p>
                    <div style="background-color: #f1f5f9; border-radius: 8px; padding: 16px; margin: 20px 0;">
                      <table width="100%%" cellpadding="6" cellspacing="0">
                        <tr><td width="35%%" style="color: #64748b; font-size: 13px;">Merit Rank:</td><td style="color: #0f172a; font-weight: 600; font-size: 14px;">#%d</td></tr>
                        <tr><td style="color: #64748b; font-size: 13px;">Composite Score:</td><td style="color: #0f172a; font-weight: 600; font-size: 14px;">%s / 100</td></tr>
                        <tr><td style="color: #64748b; font-size: 13px;">Total Vacancies:</td><td style="color: #0f172a; font-weight: 600; font-size: 14px;">%d</td></tr>
                      </table>
                    </div>
                    %s
                    <p style="text-align: center; margin: 28px 0;">
                      <a href="%s" style="background-color: #059669; color: #ffffff; padding: 12px 24px; text-decoration: none; border-radius: 6px; font-weight: 600; font-size: 14px; display: inline-block;">Open TPMS Portal</a>
                    </p>
                    <p style="font-size: 12px; color: #94a3b8; margin-bottom: 0;">Please keep your phone and portal accessible for schedule announcements.</p>
                  </td>
                </tr>
              </table>
            </body>
            </html>
            """.formatted(safeName, safeCompany, safeRole, rank, safeScore, drive.getVacancies() != null ? drive.getVacancies() : 1, bookingLinkHtml, escape(frontendBaseUrl));

        sendEmail(studentEmail, subject, "APPLICATION_SHORTLISTED", html);
    }

    public void sendRejectionFeedbackAvailableEmail(String studentEmail, String studentName, Drive drive, Application app) {
        String safeName = escape(studentName);
        String safeCompany = escape(drive.getCompanyName());
        String safeRole = escape(drive.getRoleTitle());
        String portalUrl = escape(frontendBaseUrl);

        String subject = "Application Update & Feedback Available: " + drive.getCompanyName() + " - " + drive.getRoleTitle();

        String html = """
            <!DOCTYPE html>
            <html>
            <head><meta charset="UTF-8"></head>
            <body style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background-color: #f8fafc; margin: 0; padding: 24px;">
              <table width="100%%" border="0" cellspacing="0" cellpadding="0" style="max-width: 600px; margin: 0 auto; background-color: #ffffff; border: 1px solid #e2e8f0; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.05);">
                <tr>
                  <td style="background-color: #475569; padding: 24px; text-align: left;">
                    <h1 style="color: #ffffff; margin: 0; font-size: 20px; font-weight: 700;">Drive Application Update</h1>
                  </td>
                </tr>
                <tr>
                  <td style="padding: 24px;">
                    <p style="font-size: 15px; color: #334155; margin-top: 0;">Dear %s,</p>
                    <p style="font-size: 15px; color: #334155; line-height: 1.5;">
                      The shortlisting results for <strong>%s</strong> (%s) have been finalized.
                    </p>
                    <p style="font-size: 14px; color: #475569; line-height: 1.5;">
                      Unfortunately, you were not shortlisted for this drive. However, full deterministic scoring breakdowns and actionable skill gap feedback are now available on your portal.
                    </p>
                    <p style="text-align: center; margin: 28px 0;">
                      <a href="%s" style="background-color: #475569; color: #ffffff; padding: 12px 24px; text-decoration: none; border-radius: 6px; font-weight: 600; font-size: 14px; display: inline-block;">View Detailed Feedback</a>
                    </p>
                    <p style="font-size: 12px; color: #94a3b8; margin-bottom: 0;">Review your skill match breakdowns to optimize your resume for upcoming drives.</p>
                  </td>
                </tr>
              </table>
            </body>
            </html>
            """.formatted(safeName, safeCompany, safeRole, portalUrl);

        sendEmail(studentEmail, subject, "REJECTION_FEEDBACK_AVAILABLE", html);
    }

    public void sendInterviewBookedEmail(String studentEmail, String studentName, Drive drive, String slotDetails) {
        String safeName = escape(studentName);
        String safeCompany = escape(drive.getCompanyName());
        String safeRole = escape(drive.getRoleTitle());
        String safeSlot = escape(slotDetails);

        String subject = "Interview Scheduled: " + drive.getCompanyName() + " (" + drive.getRoleTitle() + ")";

        String html = """
            <!DOCTYPE html>
            <html>
            <head><meta charset="UTF-8"></head>
            <body style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background-color: #f8fafc; margin: 0; padding: 24px;">
              <table width="100%%" border="0" cellspacing="0" cellpadding="0" style="max-width: 600px; margin: 0 auto; background-color: #ffffff; border: 1px solid #e2e8f0; border-radius: 12px; overflow: hidden;">
                <tr>
                  <td style="background-color: #0284c7; padding: 24px; text-align: left;">
                    <h1 style="color: #ffffff; margin: 0; font-size: 20px; font-weight: 700;">Interview Confirmed</h1>
                  </td>
                </tr>
                <tr>
                  <td style="padding: 24px;">
                    <p style="font-size: 15px; color: #334155; margin-top: 0;">Dear %s,</p>
                    <p style="font-size: 15px; color: #334155; line-height: 1.5;">
                      Your interview slot for <strong>%s</strong> (%s) has been successfully reserved.
                    </p>
                    <div style="background-color: #f0f9ff; border: 1px solid #bae6fd; border-radius: 8px; padding: 16px; margin: 20px 0;">
                      <p style="color: #0369a1; font-weight: 600; margin: 0;">Slot Details:</p>
                      <p style="color: #0f172a; margin: 6px 0 0 0; font-size: 14px;">%s</p>
                    </div>
                  </td>
                </tr>
              </table>
            </body>
            </html>
            """.formatted(safeName, safeCompany, safeRole, safeSlot);

        sendEmail(studentEmail, subject, "INTERVIEW_BOOKED", html);
    }

    public void sendInterviewCancelledEmail(String studentEmail, String studentName, Drive drive, String reason) {
        String safeName = escape(studentName);
        String safeCompany = escape(drive.getCompanyName());
        String safeRole = escape(drive.getRoleTitle());
        String safeReason = escape(reason != null ? reason : "Administrative rescheduling");

        String subject = "Interview Cancelled: " + drive.getCompanyName() + " (" + drive.getRoleTitle() + ")";

        String html = """
            <!DOCTYPE html>
            <html>
            <head><meta charset="UTF-8"></head>
            <body style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background-color: #f8fafc; margin: 0; padding: 24px;">
              <table width="100%%" border="0" cellspacing="0" cellpadding="0" style="max-width: 600px; margin: 0 auto; background-color: #ffffff; border: 1px solid #e2e8f0; border-radius: 12px; overflow: hidden;">
                <tr>
                  <td style="background-color: #e11d48; padding: 24px; text-align: left;">
                    <h1 style="color: #ffffff; margin: 0; font-size: 20px; font-weight: 700;">Interview Cancellation Notice</h1>
                  </td>
                </tr>
                <tr>
                  <td style="padding: 24px;">
                    <p style="font-size: 15px; color: #334155; margin-top: 0;">Dear %s,</p>
                    <p style="font-size: 15px; color: #334155; line-height: 1.5;">
                      Your scheduled interview for <strong>%s</strong> (%s) has been cancelled.
                    </p>
                    <div style="background-color: #fff1f2; border: 1px solid #fecdd3; border-radius: 8px; padding: 16px; margin: 20px 0;">
                      <p style="color: #be123c; font-weight: 600; margin: 0;">Reason:</p>
                      <p style="color: #0f172a; margin: 6px 0 0 0; font-size: 14px;">%s</p>
                    </div>
                  </td>
                </tr>
              </table>
            </body>
            </html>
            """.formatted(safeName, safeCompany, safeRole, safeReason);

        sendEmail(studentEmail, subject, "INTERVIEW_CANCELLED", html);
    }
}
