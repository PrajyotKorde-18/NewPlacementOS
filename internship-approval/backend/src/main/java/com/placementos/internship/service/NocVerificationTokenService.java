package com.placementos.internship.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
@Slf4j
public class NocVerificationTokenService {

    @Value("${app.noc.verification-secret:}")
    private String verificationSecret;

    @Value("${app.verification.base-url:http://localhost:3001/verify-noc}")
    private String verificationBaseUrl;

    @Autowired
    private Environment environment;

    @PostConstruct
    public void validateSecretOnStartup() {
        List<String> activeProfiles = Arrays.asList(environment.getActiveProfiles());
        List<String> defaultProfiles = Arrays.asList(environment.getDefaultProfiles());

        boolean isDemo = activeProfiles.contains("demo") || (activeProfiles.isEmpty() && defaultProfiles.contains("demo"));

        if (!isDemo) {
            if (verificationSecret == null || verificationSecret.trim().length() < 32) {
                throw new IllegalStateException(
                        "NOC_VERIFICATION_SECRET environment variable is required and must be at least 32 characters long in non-demo profiles.");
            }
        } else {
            if (verificationSecret == null || verificationSecret.trim().isEmpty()) {
                // Default fallback for demo profile if not explicitly populated
                verificationSecret = "demo_noc_verification_secret_min_32_characters_key";
                log.info("[NOC SECURITY] Seeded fallback demo verification secret for prototype environment.");
            }
        }
    }

    /**
     * Computes HMAC-SHA256 over:
     * Request ID, Student Email, Company Name, Issued Timestamp.
     * Returns a 32-character hexadecimal token string.
     */
    public String generateToken(Long requestId, String studentEmail, String companyName, LocalDateTime issuedAt) {
        try {
            String payload = String.format("%s:%s:%s:%s",
                    requestId != null ? requestId.toString() : "",
                    studentEmail != null ? studentEmail.trim().toLowerCase() : "",
                    companyName != null ? companyName.trim() : "",
                    issuedAt != null ? issuedAt.toString() : "");

            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(verificationSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKey);

            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));

            // Format first 16 bytes as 32 hex characters
            StringBuilder hexString = new StringBuilder();
            for (int i = 0; i < 16 && i < hash.length; i++) {
                hexString.append(String.format("%02x", hash[i]));
            }

            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate HMAC-SHA256 NOC verification token", e);
        }
    }

    public String getVerificationBaseUrl() {
        return verificationBaseUrl;
    }

    public String buildVerificationUrl(String token) {
        return verificationBaseUrl + "?token=" + token;
    }
}
