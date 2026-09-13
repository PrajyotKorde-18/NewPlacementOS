package com.placementos.internship.controller;

import com.placementos.internship.dto.ApprovalRequestDTO;
import com.placementos.internship.dto.InternshipResponseDTO;
import com.placementos.internship.dto.NocRecordDTO;
import com.placementos.internship.dto.RejectionRequestDTO;
import com.placementos.internship.service.CdpcReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/internships")
@RequiredArgsConstructor
public class CdpcReviewController {

    private final CdpcReviewService cdpcReviewService;

    @GetMapping("/review-queue")
    public ResponseEntity<List<InternshipResponseDTO>> getReviewQueue() {
        return ResponseEntity.ok(cdpcReviewService.getReviewQueue());
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<InternshipResponseDTO> approveRequest(
            @PathVariable Long id,
            @RequestBody(required = false) ApprovalRequestDTO dto
    ) throws IOException {
        return ResponseEntity.ok(cdpcReviewService.approveRequest(id));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<InternshipResponseDTO> rejectRequest(
            @PathVariable Long id,
            @RequestBody RejectionRequestDTO dto
    ) {
        String reason = dto != null ? dto.getRejectionReason() : null;
        return ResponseEntity.ok(cdpcReviewService.rejectRequest(id, reason));
    }

    @GetMapping("/{id}/noc")
    public ResponseEntity<NocRecordDTO> getNocRecord(@PathVariable Long id) {
        return ResponseEntity.ok(cdpcReviewService.getNocRecordByRequestId(id));
    }
}
