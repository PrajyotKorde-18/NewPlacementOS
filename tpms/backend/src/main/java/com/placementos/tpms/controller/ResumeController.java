package com.placementos.tpms.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.placementos.tpms.dto.ResumeUploadResponse;
import com.placementos.tpms.entity.ResumeVersion;
import com.placementos.tpms.entity.Student;
import com.placementos.tpms.entity.StudentProfile;
import com.placementos.tpms.exception.ResourceNotFoundException;
import com.placementos.tpms.repository.ResumeVersionRepository;
import com.placementos.tpms.repository.StudentProfileRepository;
import com.placementos.tpms.repository.StudentRepository;
import com.placementos.tpms.repository.UserRepository;
import com.placementos.tpms.service.ResumeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@RestController
@RequiredArgsConstructor
public class ResumeController {

    private final ResumeService resumeService;
    private final ResumeVersionRepository resumeVersionRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @PostMapping(value = "/resumes/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResumeUploadResponse> uploadResumeDirect(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "studentId", required = false) Long studentId) {
        Long targetStudentId = studentId != null ? studentId : 1L;
        ResumeUploadResponse response = resumeService.uploadResume(targetStudentId, file);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @PostMapping(value = "/students/{studentId}/resumes", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResumeUploadResponse> uploadResumePath(
            @PathVariable Long studentId,
            @RequestParam("file") MultipartFile file) {
        ResumeUploadResponse response = resumeService.uploadResume(studentId, file);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @GetMapping("/resumes/{versionId}/status")
    public ResponseEntity<?> getResumeStatus(@PathVariable Long versionId) {
        ResumeVersion version = resumeVersionRepository.findById(versionId).orElse(null);
        if (version == null) {
            return ResponseEntity.ok(Map.of("extractionStatus", "PROCESSED", "versionId", versionId));
        }
        return ResponseEntity.ok(Map.of(
                "versionId", version.getId(),
                "extractionStatus", version.getExtractionStatus() != null ? version.getExtractionStatus().name() : "PROCESSED",
                "errorLog", version.getErrorLog() != null ? version.getErrorLog() : ""
        ));
    }

    @GetMapping("/resumes/student/{studentId}/profile")
    public ResponseEntity<?> getStudentProfile(@PathVariable Long studentId) {
        // Bug 3 Fix: Read-only GET lookup. Throws ResourceNotFoundException if student missing (NO save() performed!)
        Student student = findExistingStudent(studentId);
        StudentProfile profile = studentProfileRepository.findByStudentId(student.getId()).orElse(null);

        Map<String, Object> res = new HashMap<>();
        res.put("studentId", student.getId());
        res.put("collegeId", student.getCollegeId());

        if (profile != null) {
            res.put("skills", parseJsonList(profile.getSkills()));
            res.put("projects", parseJsonList(profile.getProjects()));
            res.put("certifications", parseJsonList(profile.getCertifications()));
            res.put("codingProfiles", parseJsonMap(profile.getCodingProfiles()));
        } else {
            res.put("skills", Collections.emptyList());
            res.put("projects", Collections.emptyList());
            res.put("certifications", Collections.emptyList());
            res.put("codingProfiles", Collections.emptyMap());
        }

        return ResponseEntity.ok(res);
    }

    private Student findExistingStudent(Long studentId) {
        Optional<Student> byUser = studentRepository.findByUserId(studentId);
        if (byUser.isPresent()) return byUser.get();

        Optional<Student> byId = studentRepository.findById(studentId);
        if (byId.isPresent()) return byId.get();

        userRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student user not found with id: " + studentId));

        return studentRepository.findByUserId(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student profile not found for student id: " + studentId));
    }

    private List<?> parseJsonList(String json) {
        if (json == null || json.trim().isEmpty()) return Collections.emptyList();
        try {
            return objectMapper.readValue(json, List.class);
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private Map<?, ?> parseJsonMap(String json) {
        if (json == null || json.trim().isEmpty()) return Collections.emptyMap();
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }
}
