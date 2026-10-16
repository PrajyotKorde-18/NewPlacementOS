package com.placementos.internship.controller;

import com.placementos.internship.dto.HrConfirmActionDTO;
import com.placementos.internship.dto.HrConfirmInfoDTO;
import com.placementos.internship.dto.HrConfirmResponseDTO;
import com.placementos.internship.security.HrConfirmationRateLimiter;
import com.placementos.internship.service.HrConfirmationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/public/hr-confirm")
@RequiredArgsConstructor
public class PublicHrConfirmController {

    private final HrConfirmationService hrConfirmationService;
    private final HrConfirmationRateLimiter rateLimiter;

    @GetMapping("/{token}")
    public ResponseEntity<HrConfirmInfoDTO> getConfirmationDetails(
            @PathVariable String token,
            HttpServletRequest request
    ) {
        String clientIp = rateLimiter.getClientIp(request);
        rateLimiter.checkRateLimit(clientIp);

        HrConfirmInfoDTO info = hrConfirmationService.getPublicConfirmationInfo(token);
        return ResponseEntity.ok(info);
    }

    @PostMapping("/{token}")
    public ResponseEntity<HrConfirmResponseDTO> submitConfirmation(
            @PathVariable String token,
            @Valid @RequestBody HrConfirmActionDTO action,
            HttpServletRequest request
    ) {
        String clientIp = rateLimiter.getClientIp(request);
        rateLimiter.checkRateLimit(clientIp);

        HrConfirmResponseDTO response = hrConfirmationService.recordPublicResponse(token, action.getAnswer(), clientIp);
        if (!response.isSuccess()) {
            return ResponseEntity.badRequest().body(response);
        }
        return ResponseEntity.ok(response);
    }
}
