package com.placementos.internship.controller;

import com.placementos.internship.dto.InternshipResponseDTO;
import com.placementos.internship.dto.InternshipSubmissionDTO;
import com.placementos.internship.service.InternshipSubmissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/internships")
@RequiredArgsConstructor
public class InternshipSubmissionController {

    private final InternshipSubmissionService submissionService;

    @PostMapping(value = "/submit", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<InternshipResponseDTO> submitOffer(
            @jakarta.validation.Valid @RequestPart("data") InternshipSubmissionDTO dto,
            @RequestPart(value = "file", required = false) MultipartFile offerLetterFile
    ) throws IOException {
        InternshipResponseDTO response = submissionService.submitOffer(dto, offerLetterFile);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<InternshipResponseDTO> getRequestById(@PathVariable Long id) {
        return ResponseEntity.ok(submissionService.getRequestById(id));
    }

    @GetMapping("/student/{studentId}")
    public ResponseEntity<List<InternshipResponseDTO>> getRequestsByStudent(@PathVariable Long studentId) {
        return ResponseEntity.ok(submissionService.getRequestsByStudent(studentId));
    }

    @GetMapping("/{id}/offer-letter")
    public ResponseEntity<byte[]> downloadOfferLetter(@PathVariable Long id, java.security.Principal principal) throws IOException {
        byte[] bytes = submissionService.downloadOfferLetter(id, principal);
        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"offer_letter_" + id + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(bytes);
    }

    @GetMapping("/{id}/noc-file")
    public ResponseEntity<byte[]> downloadNocFile(@PathVariable Long id, java.security.Principal principal) throws IOException {
        byte[] bytes = submissionService.downloadNocFile(id, principal);
        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"NOC_Request_" + id + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(bytes);
    }
}
