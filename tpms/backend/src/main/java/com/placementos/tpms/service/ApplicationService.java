package com.placementos.tpms.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.placementos.tpms.dto.ApplicationResponseDTO;
import com.placementos.tpms.dto.OverrideStatusRequestDTO;
import com.placementos.tpms.dto.ScoreBreakdownDTO;
import com.placementos.tpms.entity.Application;
import com.placementos.tpms.entity.ApplicationStatus;
import com.placementos.tpms.entity.User;
import com.placementos.tpms.exception.AppException;
import com.placementos.tpms.exception.ResourceNotFoundException;
import com.placementos.tpms.entity.Student;
import com.placementos.tpms.repository.ApplicationRepository;
import com.placementos.tpms.repository.StudentRepository;
import com.placementos.tpms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public ApplicationService(
            ApplicationRepository applicationRepository,
            StudentRepository studentRepository,
            UserRepository userRepository,
            ObjectMapper objectMapper) {
        this.applicationRepository = applicationRepository;
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ApplicationResponseDTO overrideApplicationStatus(Long applicationId, OverrideStatusRequestDTO request, String coordinatorEmail) {
        User coordinator = coordinatorEmail != null ? userRepository.findByEmail(coordinatorEmail).orElse(null) : null;
        return overrideApplicationStatus(applicationId, request, coordinator);
    }

    @Transactional
    public ApplicationResponseDTO overrideApplicationStatus(Long applicationId, OverrideStatusRequestDTO request, User coordinator) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with id: " + applicationId));

        ApplicationStatus targetStatus = request.getStatus();
        if (targetStatus != ApplicationStatus.OVERRIDDEN_INCLUDED && targetStatus != ApplicationStatus.OVERRIDDEN_EXCLUDED) {
            throw new AppException("Invalid override status. Must be OVERRIDDEN_INCLUDED or OVERRIDDEN_EXCLUDED", HttpStatus.BAD_REQUEST);
        }

        if (request.getOverrideReason() == null || request.getOverrideReason().trim().isEmpty()) {
            throw new AppException("Override reason is mandatory and cannot be empty", HttpStatus.BAD_REQUEST);
        }

        application.setStatus(targetStatus);
        application.setOverrideReason(request.getOverrideReason().trim());
        application.setOverriddenBy(coordinator);

        application = applicationRepository.save(application);
        log.info("Coordinator User ID {} overridden Application ID {} status to {} with reason: {}",
                coordinator != null ? coordinator.getId() : null, applicationId, targetStatus, request.getOverrideReason());

        return mapToApplicationResponseDTO(application);
    }

    @Transactional(readOnly = true)
    public java.util.List<ApplicationResponseDTO> getApplicationsByStudent(Long studentId) {
        Student student = resolveStudent(studentId);
        if (student == null) {
            return java.util.Collections.emptyList();
        }
        java.util.List<Application> applications = applicationRepository.findByStudentId(student.getId());
        java.util.List<ApplicationResponseDTO> dtos = new java.util.ArrayList<>();
        for (Application app : applications) {
            dtos.add(mapToApplicationResponseDTO(app));
        }
        return dtos;
    }

    private Student resolveStudent(Long studentId) {
        if (studentId == null) return null;

        java.util.Optional<Student> byUser = studentRepository.findByUserId(studentId);
        if (byUser.isPresent()) return byUser.get();

        java.util.Optional<Student> byId = studentRepository.findById(studentId);
        if (byId.isPresent()) return byId.get();

        java.util.Optional<User> userOpt = userRepository.findById(studentId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            return studentRepository.findByUserId(user.getId()).orElse(null);
        }
        return null;
    }

    private ApplicationResponseDTO mapToApplicationResponseDTO(Application app) {
        ScoreBreakdownDTO breakdown = null;
        if (app.getScoreBreakdown() != null) {
            try {
                breakdown = objectMapper.readValue(app.getScoreBreakdown(), ScoreBreakdownDTO.class);
            } catch (Exception e) {
                log.error("Error parsing Application score breakdown JSON", e);
            }
        }

        return ApplicationResponseDTO.builder()
                .id(app.getId())
                .driveId(app.getDrive().getId())
                .companyName(app.getDrive().getCompanyName())
                .roleTitle(app.getDrive().getRoleTitle())
                .studentId(app.getStudent().getId())
                .collegeId(app.getStudent().getCollegeId())
                .studentName(app.getStudent().getUser() != null ? app.getStudent().getUser().getName() : null)
                .studentEmail(app.getStudent().getUser() != null ? app.getStudent().getUser().getEmail() : null)
                .score(app.getScore())
                .rankPosition(app.getRankPosition())
                .status(app.getStatus())
                .scoreBreakdown(breakdown)
                .overrideReason(app.getOverrideReason())
                .overriddenByUserId(app.getOverriddenBy() != null ? app.getOverriddenBy().getId() : null)
                .overriddenByUserName(app.getOverriddenBy() != null ? app.getOverriddenBy().getName() : null)
                .appliedAt(app.getAppliedAt())
                .build();
    }
}
