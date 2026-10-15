package com.placementos.tpms.controller;

import com.placementos.tpms.dto.ApplicationResponseDTO;
import com.placementos.tpms.dto.DriveDTO;
import com.placementos.tpms.dto.RoleProfileDTO;
import com.placementos.tpms.entity.User;
import com.placementos.tpms.service.DriveService;
import com.placementos.tpms.service.JdExtractionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/drives")
@RequiredArgsConstructor
public class DriveController {

    private final DriveService driveService;
    private final JdExtractionService jdExtractionService;

    @PostMapping
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('COORDINATOR', 'ADMIN')")
    public ResponseEntity<DriveDTO> createDrive(
            @Valid @RequestBody DriveDTO driveDTO,
            java.security.Principal principal) {
        String userEmail = principal != null ? principal.getName() : null;
        DriveDTO created = driveService.createDrive(driveDTO, userEmail);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public ResponseEntity<List<DriveDTO>> getAllDrives() {
        return ResponseEntity.ok(driveService.getAllDrives());
    }

    @PostMapping("/extract-jd")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('COORDINATOR', 'ADMIN')")
    public ResponseEntity<RoleProfileDTO> extractJdFromText(@RequestBody Map<String, String> request) throws Exception {
        String jdText = request.getOrDefault("jdText", "");
        RoleProfileDTO preview = jdExtractionService.extractJdFromText(jdText);
        return ResponseEntity.ok(preview);
    }

    @PostMapping(value = "/extract-jd-file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('COORDINATOR', 'ADMIN')")
    public ResponseEntity<RoleProfileDTO> extractJdFromFile(@RequestParam("file") MultipartFile file) throws Exception {
        com.placementos.tpms.util.FileUploadValidator.validateFile(
                file,
                com.placementos.tpms.util.FileUploadValidator.ALLOWED_JD_EXTENSIONS,
                com.placementos.tpms.util.FileUploadValidator.DEFAULT_MAX_SIZE_BYTES
        );
        RoleProfileDTO preview = jdExtractionService.extractJdFromPdf(file);
        return ResponseEntity.ok(preview);
    }

    @GetMapping("/{driveId}")
    public ResponseEntity<DriveDTO> getDrive(@PathVariable Long driveId) {
        return ResponseEntity.ok(driveService.getDrive(driveId));
    }

    @PutMapping("/{driveId}")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('COORDINATOR', 'ADMIN')")
    public ResponseEntity<DriveDTO> updateDrive(
            @PathVariable Long driveId,
            @Valid @RequestBody DriveDTO driveDTO) {
        DriveDTO updated = driveService.updateDrive(driveId, driveDTO);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{driveId}")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('COORDINATOR', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> deleteDrive(@PathVariable Long driveId) {
        driveService.deleteDrive(driveId);
        return ResponseEntity.ok(Map.of("message", "Drive deleted successfully", "driveId", driveId));
    }

    @PostMapping("/{driveId}/apply")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApplicationResponseDTO> applyToDrive(
            @PathVariable Long driveId,
            @RequestParam("studentId") Long studentId) {
        ApplicationResponseDTO response = driveService.applyToDrive(driveId, studentId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{driveId}/rank")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('COORDINATOR', 'ADMIN')")
    public ResponseEntity<List<ApplicationResponseDTO>> rankApplications(@PathVariable Long driveId) {
        List<ApplicationResponseDTO> ranked = driveService.calculateAndRankApplications(driveId);
        return ResponseEntity.ok(ranked);
    }

    @GetMapping("/{driveId}/applications")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('COORDINATOR', 'ADMIN')")
    public ResponseEntity<List<ApplicationResponseDTO>> getDriveApplications(@PathVariable Long driveId) {
        return ResponseEntity.ok(driveService.getDriveApplications(driveId));
    }
}
