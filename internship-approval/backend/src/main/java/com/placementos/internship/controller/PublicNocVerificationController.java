package com.placementos.internship.controller;

import com.placementos.internship.dto.NocVerificationResponseDTO;
import com.placementos.internship.exception.AppException;
import com.placementos.internship.security.NocVerificationRateLimiter;
import com.placementos.internship.service.NocVerificationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/public/noc")
@RequiredArgsConstructor
@Slf4j
public class PublicNocVerificationController {

    private final NocVerificationService verificationService;
    private final NocVerificationRateLimiter rateLimiter;

    @GetMapping("/verify/{token}")
    public ResponseEntity<NocVerificationResponseDTO> verifyNocToken(
            @PathVariable String token,
            HttpServletRequest request) {

        String clientIp = extractClientIp(request);
        if (!rateLimiter.tryAcquire(clientIp)) {
            log.warn("[NOC RATE LIMIT] Verification request throttled for IP: {}", clientIp);
            throw new AppException("Too many verification attempts from this IP address. Please wait a minute and try again.", HttpStatus.TOO_MANY_REQUESTS);
        }

        NocVerificationResponseDTO response = verificationService.verifyToken(token);
        return ResponseEntity.ok(response);
    }

    private String extractClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
