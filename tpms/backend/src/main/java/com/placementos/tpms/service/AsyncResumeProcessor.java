package com.placementos.tpms.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.placementos.tpms.dto.ParsedResumeDTO;
import com.placementos.tpms.entity.ExtractionStatus;
import com.placementos.tpms.entity.ResumeVersion;
import com.placementos.tpms.entity.Student;
import com.placementos.tpms.entity.StudentProfile;
import com.placementos.tpms.repository.ResumeVersionRepository;
import com.placementos.tpms.repository.StudentProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;

@Service
@RequiredArgsConstructor
@Slf4j
public class AsyncResumeProcessor {

    private final ResumeVersionRepository resumeVersionRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final com.placementos.tpms.repository.StudentRepository studentRepository;
    private final PdfParserService pdfParserService;
    private final ResumeExtractionService resumeExtractionService;
    private final ObjectMapper objectMapper;

    public void processResumeSync(Long resumeVersionId) {
        processResume(resumeVersionId);
    }

    @Async("resumeTaskExecutor")
    public void processResumeAsync(Long resumeVersionId) {
        processResume(resumeVersionId);
    }

    @Transactional
    public void processResume(Long resumeVersionId) {
        log.info("[Resume Processor] Starting extraction for ResumeVersion ID: {}", resumeVersionId);

        ResumeVersion resumeVersion = resumeVersionRepository.findById(resumeVersionId).orElse(null);
        if (resumeVersion == null) {
            log.error("[Resume Processor] ResumeVersion with ID {} not found", resumeVersionId);
            return;
        }

        // 1. Transition state from PENDING to PROCESSING
        resumeVersion.setExtractionStatus(ExtractionStatus.PROCESSING);
        resumeVersion = resumeVersionRepository.saveAndFlush(resumeVersion);

        try {
            File pdfFile = new File(resumeVersion.getFilePath());
            String rawText = pdfParserService.extractTextFromPdf(pdfFile);

            // Calls Groq API (or deliberate Mock Mode). If Groq API fails twice, an exception is thrown.
            ParsedResumeDTO parsedDTO = resumeExtractionService.extractStructuredData(rawText);

            ExtractionStatus status = determineStatus(parsedDTO);

            // Save student profile (throws Exception on JSON serialization or DB error)
            updateStudentProfile(resumeVersion.getStudent(), parsedDTO);

            // 2. Only if profile write succeeds, update resume version status to COMPLETED / FLAGGED_FOR_REVIEW
            resumeVersion.setExtractionStatus(status);
            resumeVersion.setExtractionSource(parsedDTO.getExtractionSource());
            if (status == ExtractionStatus.FLAGGED_FOR_REVIEW) {
                resumeVersion.setErrorLog("Ambiguous or minimal data extracted from resume text.");
            } else {
                resumeVersion.setErrorLog(null);
            }
            resumeVersionRepository.save(resumeVersion);

            log.info("[Async Queue] Successfully processed resume ID: {} with status: {}", resumeVersionId, status);

        } catch (Exception e) {
            // Failure Path: If extraction OR profile saving fails, mark FAILED and log error!
            log.error("[Async Queue] Extraction or profile persistence failed for resume ID: {}", resumeVersionId, e);
            resumeVersion.setExtractionStatus(ExtractionStatus.FAILED);
            resumeVersion.setErrorLog("Resume processing failed: " + e.getMessage());
            resumeVersionRepository.save(resumeVersion);
        }
    }

    private ExtractionStatus determineStatus(ParsedResumeDTO dto) {
        if (dto.getSkills().isEmpty() && dto.getProjects().isEmpty() && dto.getInternships().isEmpty()) {
            return ExtractionStatus.FLAGGED_FOR_REVIEW;
        }
        return ExtractionStatus.COMPLETED;
    }

    private void updateStudentProfile(Student studentRef, ParsedResumeDTO parsedDTO) throws Exception {
        Long studentId = studentRef.getId();
        Student student = studentRepository.findById(studentId)
                .orElse(studentRef);

        StudentProfile profile = studentProfileRepository.findByStudentId(studentId)
                .orElseGet(() -> {
                    StudentProfile p = StudentProfile.builder()
                            .student(student)
                            .studentId(student.getId())
                            .isNewEntity(false)
                            .build();
                    p.setStudent(student);
                    return p;
                });

        profile.setStudent(student);
        profile.setSkills(objectMapper.writeValueAsString(parsedDTO.getSkills()));
        profile.setProjects(objectMapper.writeValueAsString(parsedDTO.getProjects()));
        profile.setInternships(objectMapper.writeValueAsString(parsedDTO.getInternships()));
        profile.setCertifications(objectMapper.writeValueAsString(parsedDTO.getCertifications()));
        profile.setCodingProfiles(objectMapper.writeValueAsString(parsedDTO.getCodingProfiles()));
        profile.setSummary(parsedDTO.getSummary() != null ? parsedDTO.getSummary() : "");
        profile.setAchievements(parsedDTO.getAchievements() != null ? parsedDTO.getAchievements() : "");

        studentProfileRepository.save(profile);
    }
}
