package com.placementos.tpms.controller;

import com.placementos.tpms.dto.ApplicationResponseDTO;
import com.placementos.tpms.dto.OverrideStatusRequestDTO;
import com.placementos.tpms.dto.RejectionFeedbackDTO;
import com.placementos.tpms.entity.User;
import com.placementos.tpms.service.ApplicationService;
import com.placementos.tpms.service.FeedbackGeneratorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;
    private final FeedbackGeneratorService feedbackGeneratorService;

    @PostMapping("/{applicationId}/override")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('COORDINATOR', 'ADMIN')")
    public ResponseEntity<ApplicationResponseDTO> overrideStatus(
            @PathVariable Long applicationId,
            @Valid @RequestBody OverrideStatusRequestDTO request,
            java.security.Principal principal) {
        String email = principal != null ? principal.getName() : null;
        ApplicationResponseDTO response = applicationService.overrideApplicationStatus(applicationId, request, email);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/student/{studentId}")
    public ResponseEntity<java.util.List<ApplicationResponseDTO>> getStudentApplications(@PathVariable Long studentId) {
        return ResponseEntity.ok(applicationService.getApplicationsByStudent(studentId));
    }

    @GetMapping("/{applicationId}/feedback")
    public ResponseEntity<RejectionFeedbackDTO> getRejectionFeedback(@PathVariable Long applicationId) {
        RejectionFeedbackDTO feedback = feedbackGeneratorService.generateFeedback(applicationId);
        return ResponseEntity.ok(feedback);
    }
}
