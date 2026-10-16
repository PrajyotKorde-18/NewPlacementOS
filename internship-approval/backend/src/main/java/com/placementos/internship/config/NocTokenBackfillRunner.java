package com.placementos.internship.config;

import com.placementos.internship.entity.NocRecord;
import com.placementos.internship.repository.NocRecordRepository;
import com.placementos.internship.service.NocPdfGeneratorService;
import com.placementos.internship.service.NocVerificationTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Order(15)
@RequiredArgsConstructor
@Slf4j
public class NocTokenBackfillRunner implements CommandLineRunner {

    private final NocRecordRepository nocRecordRepository;
    private final NocVerificationTokenService tokenService;
    private final NocPdfGeneratorService pdfGeneratorService;

    @Override
    @Transactional
    public void run(String... args) {
        backfillMissingVerificationTokens();
    }

    public void backfillMissingVerificationTokens() {
        List<NocRecord> unbackfilledRecords = nocRecordRepository.findByVerificationTokenIsNull();
        if (unbackfilledRecords.isEmpty()) {
            log.info("[NOC BACKFILL] All existing NOC records have verification tokens. No backfill needed.");
            return;
        }

        log.info("[NOC BACKFILL] Found {} NOC records without verification tokens. Starting backfill...", unbackfilledRecords.size());

        for (NocRecord record : unbackfilledRecords) {
            try {
                LocalDateTime issuedAt = record.getApprovedAt() != null ? record.getApprovedAt() : LocalDateTime.now();
                String studentEmail = (record.getRequest() != null && record.getRequest().getStudent() != null
                        && record.getRequest().getStudent().getUser() != null)
                        ? record.getRequest().getStudent().getUser().getEmail()
                        : "";
                String companyName = record.getRequest() != null ? record.getRequest().getCompanyName() : "";

                String token = tokenService.generateToken(
                        record.getRequest() != null ? record.getRequest().getId() : record.getId(),
                        studentEmail,
                        companyName,
                        issuedAt
                );

                record.setVerificationToken(token);
                record.setQrGeneratedAt(LocalDateTime.now());

                // Regenerate PDF with embedded QR code if request and approver exist
                if (record.getRequest() != null && record.getApprovedBy() != null) {
                    try {
                        String newPdfPath = pdfGeneratorService.generateNocPdf(record.getRequest(), record.getApprovedBy(), token);
                        record.setPdfPath(newPdfPath);
                    } catch (Exception ex) {
                        log.warn("[NOC BACKFILL] Could not regenerate PDF for NOC record ID {}: {}", record.getId(), ex.getMessage());
                    }
                }

                nocRecordRepository.save(record);
                log.info("[NOC BACKFILL] Successfully backfilled verification token for NOC record ID {}", record.getId());
            } catch (Exception e) {
                log.error("[NOC BACKFILL] Failed to backfill verification token for NOC record ID {}: {}", record.getId(), e.getMessage());
            }
        }

        log.info("[NOC BACKFILL] Verification token backfill completed.");
    }
}
