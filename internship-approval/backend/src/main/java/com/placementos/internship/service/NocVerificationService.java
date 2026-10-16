package com.placementos.internship.service;

import com.placementos.internship.dto.NocVerificationResponseDTO;
import com.placementos.internship.entity.InternshipRequest;
import com.placementos.internship.entity.NocRecord;
import com.placementos.internship.repository.NocRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class NocVerificationService {

    private final NocRecordRepository nocRecordRepository;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Transactional(readOnly = true)
    public NocVerificationResponseDTO verifyToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            return buildInvalidResponse("Verification token cannot be empty.");
        }

        Optional<NocRecord> optionalRecord = nocRecordRepository.findByVerificationToken(token.trim());
        if (optionalRecord.isEmpty()) {
            log.warn("[NOC VERIFY] Verification failed for invalid or unknown token: {}", token);
            return buildInvalidResponse("This NOC certificate cannot be verified. The token is invalid or has expired.");
        }

        NocRecord noc = optionalRecord.get();
        InternshipRequest request = noc.getRequest();

        String rawStudentName = (request != null && request.getStudent() != null && request.getStudent().getUser() != null)
                ? request.getStudent().getUser().getName()
                : "Student";
        String maskedStudentName = maskStudentName(rawStudentName);

        String companyName = request != null ? request.getCompanyName() : "N/A";
        String roleTitle = request != null ? request.getRoleTitle() : "N/A";
        String approvalDate = noc.getApprovedAt() != null ? noc.getApprovedAt().format(DATE_FORMATTER) : "N/A";

        log.info("[NOC VERIFY] Successful NOC verification for token {} (Request #{})", token, request != null ? request.getId() : "null");

        return NocVerificationResponseDTO.builder()
                .valid(true)
                .studentName(maskedStudentName)
                .companyName(companyName)
                .roleTitle(roleTitle)
                .approvalDate(approvalDate)
                .message("Official NOC Certificate verified successfully.")
                .build();
    }

    private NocVerificationResponseDTO buildInvalidResponse(String reason) {
        return NocVerificationResponseDTO.builder()
                .valid(false)
                .studentName(null)
                .companyName(null)
                .roleTitle(null)
                .approvalDate(null)
                .message(reason)
                .build();
    }

    /**
     * Masks student full name preserving only the initial letter of each word:
     * e.g., "Arjun Jaiswal" -> "A*** J***"
     */
    public static String maskStudentName(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            return "***";
        }
        String[] parts = fullName.trim().split("\\s+");
        StringBuilder masked = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (!part.isEmpty()) {
                masked.append(Character.toUpperCase(part.charAt(0))).append("***");
                if (i < parts.length - 1) {
                    masked.append(" ");
                }
            }
        }
        return masked.toString();
    }
}
