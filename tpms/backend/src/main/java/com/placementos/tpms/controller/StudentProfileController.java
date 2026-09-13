package com.placementos.tpms.controller;

import com.placementos.tpms.dto.StudentFullProfileDTO;
import com.placementos.tpms.dto.StudentProfileUpdateDTO;
import com.placementos.tpms.service.StudentProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/students")
@RequiredArgsConstructor
public class StudentProfileController {

    private final StudentProfileService studentProfileService;

    @GetMapping("/{studentId}/profile")
    public ResponseEntity<StudentFullProfileDTO> getStudentProfile(@PathVariable Long studentId) {
        StudentFullProfileDTO profile = studentProfileService.getStudentProfile(studentId);
        return ResponseEntity.ok(profile);
    }

    @PutMapping("/{studentId}/profile")
    public ResponseEntity<StudentFullProfileDTO> updateStudentProfile(
            @PathVariable Long studentId,
            @Valid @RequestBody StudentProfileUpdateDTO dto
    ) {
        StudentFullProfileDTO updatedProfile = studentProfileService.updateStudentProfile(studentId, dto);
        return ResponseEntity.ok(updatedProfile);
    }
}
