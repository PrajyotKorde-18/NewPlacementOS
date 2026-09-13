package com.placementos.tpms.service;

import com.placementos.tpms.dto.ResumeUploadResponse;
import com.placementos.tpms.entity.ExtractionStatus;
import com.placementos.tpms.entity.ResumeVersion;
import com.placementos.tpms.entity.Student;
import com.placementos.tpms.entity.User;
import com.placementos.tpms.exception.ResourceNotFoundException;
import com.placementos.tpms.repository.ResumeVersionRepository;
import com.placementos.tpms.repository.StudentRepository;
import com.placementos.tpms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResumeService {

    private final ResumeVersionRepository resumeVersionRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final AsyncResumeProcessor asyncResumeProcessor;

    @Value("${app.upload.dir:uploads/resumes}")
    private String uploadDir;

    @Transactional
    public ResumeUploadResponse uploadResume(Long studentId, MultipartFile file) {
        Student student = resolveStudent(studentId);

        String savedFilePath = saveFile(file);

        // 1. Create ResumeVersion record in PENDING status
        ResumeVersion resumeVersion = ResumeVersion.builder()
                .student(student)
                .filePath(savedFilePath)
                .extractionStatus(ExtractionStatus.PENDING)
                .build();

        resumeVersion = resumeVersionRepository.saveAndFlush(resumeVersion);

        // Process extraction for fast instant UI response
        asyncResumeProcessor.processResumeSync(resumeVersion.getId());

        ResumeVersion updated = resumeVersionRepository.findById(resumeVersion.getId()).orElse(resumeVersion);

        return ResumeUploadResponse.builder()
                .resumeId(updated.getId())
                .versionId(updated.getId())
                .studentId(student.getId())
                .filePath(savedFilePath)
                .extractionStatus(updated.getExtractionStatus())
                .message("Resume uploaded and parsed successfully.")
                .extractedData(null)
                .build();
    }

    private String saveFile(MultipartFile file) {
        try {
            String effectiveUploadDir = (uploadDir != null && !uploadDir.isEmpty()) ? uploadDir : "uploads/resumes";
            Path targetDir = Paths.get(effectiveUploadDir).toAbsolutePath().normalize();
            Files.createDirectories(targetDir);

            String originalName = file.getOriginalFilename();
            String fileName = UUID.randomUUID() + "_" + (originalName != null ? originalName : "resume.pdf");
            Path targetPath = targetDir.resolve(fileName);

            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            return targetPath.toString();
        } catch (Exception e) {
            throw new RuntimeException("Could not store file: " + e.getMessage(), e);
        }
    }

    private Student resolveStudent(Long studentId) {
        java.util.Optional<Student> byUser = studentRepository.findByUserId(studentId);
        if (byUser.isPresent()) return byUser.get();

        java.util.Optional<Student> byId = studentRepository.findById(studentId);
        if (byId.isPresent()) return byId.get();

        User user = userRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student user not found with id: " + studentId));

        return studentRepository.save(Student.builder()
                .user(user)
                .collegeId(user.getEmail() != null ? user.getEmail().split("@")[0].toUpperCase() : "STUDENT_" + user.getId())
                .build());
    }
}
