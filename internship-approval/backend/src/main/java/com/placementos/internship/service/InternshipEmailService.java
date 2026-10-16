package com.placementos.internship.service;

import com.placementos.internship.entity.InternshipRequest;
import com.placementos.internship.entity.NocRecord;
import com.placementos.internship.entity.NotificationLog;
import com.placementos.internship.repository.NotificationLogRepository;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.io.File;

@Service
@RequiredArgsConstructor
@Slf4j
public class InternshipEmailService {

    private final NotificationLogRepository notificationLogRepository;

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${app.notifications.enabled:false}")
    private boolean notificationsEnabled;

    @Value("${app.notifications.mail-from:noreply@placementos.local}")
    private String mailFrom;

    @Value("${app.frontend.base-url:http://localhost:3001}")
    private String frontendBaseUrl;

    @Value("${app.verification.base-url:http://localhost:3001/verify-noc}")
    private String verificationBaseUrl;

    public String escape(String input) {
        return HtmlUtils.htmlEscape(input != null ? input : "");
    }

    public void sendEmail(String to, String subject, String notificationType, String htmlBody, File attachmentFile, String attachmentFilename) {
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
            boolean isMultipart = attachmentFile != null && attachmentFile.exists();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, isMultipart, "UTF-8");
            helper.setFrom(mailFrom);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);

            if (isMultipart) {
                String safeAttachmentName = attachmentFilename != null ? attachmentFilename : "NOC_Certificate.pdf";
                helper.addAttachment(safeAttachmentName, new FileSystemResource(attachmentFile));
            }

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

    public void sendApprovedEmail(String studentEmail, String studentName, InternshipRequest request, NocRecord nocRecord) {
        String safeName = escape(studentName);
        String safeCompany = escape(request.getCompanyName());
        String safeRole = escape(request.getRoleTitle());

        String verificationToken = nocRecord != null ? nocRecord.getVerificationToken() : null;
        String verificationUrl = null;
        if (verificationToken != null && !verificationToken.isBlank()) {
            verificationUrl = verificationBaseUrl + "?token=" + verificationToken;
        }

        String verificationLinkHtml = "";
        if (verificationUrl != null) {
            verificationLinkHtml = """
                <div style="background-color: #ecfdf5; border: 1px solid #10b981; border-radius: 8px; padding: 16px; margin: 20px 0; text-align: center;">
                  <p style="color: #065f46; font-weight: 600; margin: 0 0 6px 0; font-size: 14px;">Cryptographically Signed NOC Issued</p>
                  <p style="color: #047857; font-size: 12px; margin: 0 0 12px 0;">This NOC certificate carries an HMAC-SHA256 digital stamp verifiable online by employers.</p>
                  <a href="%s" style="background-color: #059669; color: #ffffff; padding: 10px 20px; text-decoration: none; border-radius: 6px; font-weight: 600; font-size: 13px; display: inline-block;">Verify NOC Online</a>
                </div>
                """.formatted(escape(verificationUrl));
        }

        File pdfFile = null;
        if (nocRecord != null && nocRecord.getPdfPath() != null) {
            File f = new File(nocRecord.getPdfPath());
            if (f.exists()) {
                pdfFile = f;
            }
        }

        String subject = "Internship Approved — NOC Issued for " + request.getCompanyName();

        String html = """
            <!DOCTYPE html>
            <html>
            <head><meta charset="UTF-8"></head>
            <body style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background-color: #f8fafc; margin: 0; padding: 24px;">
              <table width="100%%" border="0" cellspacing="0" cellpadding="0" style="max-width: 600px; margin: 0 auto; background-color: #ffffff; border: 1px solid #e2e8f0; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.05);">
                <tr>
                  <td style="background-color: #059669; padding: 24px; text-align: left;">
                    <h1 style="color: #ffffff; margin: 0; font-size: 20px; font-weight: 700;">Internship Approval Granted</h1>
                  </td>
                </tr>
                <tr>
                  <td style="padding: 24px;">
                    <p style="font-size: 15px; color: #334155; margin-top: 0;">Dear %s,</p>
                    <p style="font-size: 15px; color: #334155; line-height: 1.5;">
                      Congratulations! Your internship request with <strong>%s</strong> for the role of <strong>%s</strong> has been reviewed and approved by the Career Development &amp; Placement Cell (CDPC).
                    </p>
                    <div style="background-color: #f1f5f9; border-radius: 8px; padding: 16px; margin: 20px 0;">
                      <table width="100%%" cellpadding="6" cellspacing="0">
                        <tr><td width="35%%" style="color: #64748b; font-size: 13px;">Organization:</td><td style="color: #0f172a; font-weight: 600; font-size: 14px;">%s</td></tr>
                        <tr><td style="color: #64748b; font-size: 13px;">Designation:</td><td style="color: #0f172a; font-weight: 600; font-size: 14px;">%s</td></tr>
                        <tr><td style="color: #64748b; font-size: 13px;">Duration:</td><td style="color: #0f172a; font-weight: 600; font-size: 14px;">%d Weeks</td></tr>
                      </table>
                    </div>
                    %s
                    <p style="font-size: 13px; color: #475569; line-height: 1.5;">
                      Your official digitally stamped No Objection Certificate (NOC) is attached to this email. You may submit this document to your prospective employer.
                    </p>
                    <p style="text-align: center; margin: 24px 0;">
                      <a href="%s" style="background-color: #059669; color: #ffffff; padding: 12px 24px; text-decoration: none; border-radius: 6px; font-weight: 600; font-size: 14px; display: inline-block;">Go to Internship Portal</a>
                    </p>
                    <p style="font-size: 12px; color: #94a3b8; margin-bottom: 0;">Career Development &amp; Placement Cell • Ramdeobaba University</p>
                  </td>
                </tr>
              </table>
            </body>
            </html>
            """.formatted(safeName, safeCompany, safeRole, safeCompany, safeRole, request.getDurationWeeks(), verificationLinkHtml, escape(frontendBaseUrl));

        sendEmail(studentEmail, subject, "INTERNSHIP_APPROVED", html, pdfFile, "NOC_Request_" + request.getId() + ".pdf");
    }

    public void sendRejectedEmail(String studentEmail, String studentName, InternshipRequest request, String rejectionReason) {
        String safeName = escape(studentName);
        String safeCompany = escape(request.getCompanyName());
        String safeRole = escape(request.getRoleTitle());
        String safeReason = escape(rejectionReason != null ? rejectionReason : "Policy rule non-compliance");

        String subject = "Internship Request Decision: " + request.getCompanyName() + " (" + request.getRoleTitle() + ")";

        String html = """
            <!DOCTYPE html>
            <html>
            <head><meta charset="UTF-8"></head>
            <body style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background-color: #f8fafc; margin: 0; padding: 24px;">
              <table width="100%%" border="0" cellspacing="0" cellpadding="0" style="max-width: 600px; margin: 0 auto; background-color: #ffffff; border: 1px solid #e2e8f0; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.05);">
                <tr>
                  <td style="background-color: #e11d48; padding: 24px; text-align: left;">
                    <h1 style="color: #ffffff; margin: 0; font-size: 20px; font-weight: 700;">Internship Request Not Approved</h1>
                  </td>
                </tr>
                <tr>
                  <td style="padding: 24px;">
                    <p style="font-size: 15px; color: #334155; margin-top: 0;">Dear %s,</p>
                    <p style="font-size: 15px; color: #334155; line-height: 1.5;">
                      Your internship request with <strong>%s</strong> for the position of <strong>%s</strong> has been reviewed by the CDPC authority and was not approved.
                    </p>
                    <div style="background-color: #fff1f2; border: 1px solid #fecdd3; border-radius: 8px; padding: 16px; margin: 20px 0;">
                      <p style="color: #9f1239; font-weight: 600; margin: 0 0 6px 0; font-size: 13px;">Reason for Rejection:</p>
                      <p style="color: #881337; margin: 0; font-size: 14px; line-height: 1.4;">%s</p>
                    </div>
                    <p style="font-size: 13px; color: #475569; line-height: 1.5;">
                      Please review the institutional guidelines or contact the CDPC office if you believe there is an error in your submission.
                    </p>
                    <p style="text-align: center; margin: 24px 0;">
                      <a href="%s" style="background-color: #e11d48; color: #ffffff; padding: 12px 24px; text-decoration: none; border-radius: 6px; font-weight: 600; font-size: 14px; display: inline-block;">Review in Portal</a>
                    </p>
                    <p style="font-size: 12px; color: #94a3b8; margin-bottom: 0;">Career Development &amp; Placement Cell • Ramdeobaba University</p>
                  </td>
                </tr>
              </table>
            </body>
            </html>
            """.formatted(safeName, safeCompany, safeRole, safeReason, escape(frontendBaseUrl));

        sendEmail(studentEmail, subject, "INTERNSHIP_REJECTED", html, null, null);
    }
}
