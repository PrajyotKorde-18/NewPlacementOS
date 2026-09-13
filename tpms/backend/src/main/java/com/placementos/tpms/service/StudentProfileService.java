package com.placementos.tpms.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.placementos.tpms.dto.*;
import com.placementos.tpms.entity.*;
import com.placementos.tpms.exception.AppException;
import com.placementos.tpms.exception.ResourceNotFoundException;
import com.placementos.tpms.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class StudentProfileService {

    private final StudentRepository studentRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final AcademicProfileRepository academicProfileRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public StudentFullProfileDTO getStudentProfile(Long studentId) {
        // Bug 3 Fix: Read-only GET lookup. If student does not exist, throw 404. ZERO INSERTs occur!
        Student student = findExistingStudent(studentId);
        StudentProfile profile = studentProfileRepository.findByStudentId(student.getId()).orElse(null);
        AcademicProfile academicProfile = academicProfileRepository.findByStudentId(student.getId()).orElse(null);

        return mapToFullProfileDTO(student, profile, academicProfile);
    }

    @Transactional
    public StudentFullProfileDTO updateStudentProfile(Long studentId, StudentProfileUpdateDTO dto) {
        Student student = findExistingStudent(studentId);

        // 1. Roll number pre-check
        if (dto.getCollegeRollNumber() != null && !dto.getCollegeRollNumber().isBlank()) {
            String trimmedRoll = dto.getCollegeRollNumber().trim();
            Optional<Student> existing = studentRepository.findByCollegeRollNumber(trimmedRoll);
            if (existing.isPresent() && !existing.get().getId().equals(student.getId())) {
                throw new AppException("College roll number is already in use by another student: " + trimmedRoll, HttpStatus.BAD_REQUEST);
            }
            student.setCollegeRollNumber(trimmedRoll);
        }

        if (dto.getPhoneNumber() != null) {
            student.setPhoneNumber(dto.getPhoneNumber().trim());
        }
        if (dto.getPlacementStatus() != null) {
            student.setPlacementStatus(dto.getPlacementStatus());
        }

        // 2. Wrap DB save to handle concurrent unique constraint race conditions
        try {
            studentRepository.save(student);
        } catch (DataIntegrityViolationException ex) {
            if (ex.getMessage() != null && ex.getMessage().contains("uk_students_college_roll_number")) {
                throw new AppException("College roll number is already in use by another student", HttpStatus.BAD_REQUEST);
            }
            throw ex;
        }

        // 3. Update StudentProfile
        StudentProfile profile = studentProfileRepository.findByStudentId(student.getId())
                .orElseGet(() -> StudentProfile.builder().student(student).studentId(student.getId()).build());

        if (dto.getLinkedinUrl() != null) profile.setLinkedinUrl(dto.getLinkedinUrl());
        if (dto.getGithubUrl() != null) profile.setGithubUrl(dto.getGithubUrl());
        if (dto.getPortfolioUrl() != null) profile.setPortfolioUrl(dto.getPortfolioUrl());
        if (dto.getSummary() != null) profile.setSummary(dto.getSummary());
        if (dto.getAchievements() != null) profile.setAchievements(dto.getAchievements());

        // Bug 2 Fix: Only update structuredCertifications if explicitly provided (non-null in DTO)
        if (dto.getStructuredCertifications() != null) {
            profile.getStructuredCertifications().clear();
            for (CertificationDTO certDTO : dto.getStructuredCertifications()) {
                StudentCertification cert = StudentCertification.builder()
                        .studentProfile(profile)
                        .name(certDTO.getName())
                        .issuingBody(certDTO.getIssuingBody())
                        .issueDate(certDTO.getIssueDate())
                        .credentialUrl(certDTO.getCredentialUrl())
                        .build();
                profile.getStructuredCertifications().add(cert);
            }
            try {
                profile.setCertifications(objectMapper.writeValueAsString(dto.getStructuredCertifications()));
            } catch (Exception ignored) {}
        }

        studentProfileRepository.save(profile);

        // 4. Update AcademicProfile
        AcademicProfile academicProfile = academicProfileRepository.findByStudentId(student.getId())
                .orElseGet(() -> AcademicProfile.builder().student(student).studentId(student.getId()).build());

        if (dto.getAcademicProfile() != null) {
            AcademicProfileDTO acDto = dto.getAcademicProfile();
            if (acDto.getTenthPercentage() != null) academicProfile.setTenthPercentage(acDto.getTenthPercentage());
            if (acDto.getTenthCgpa() != null) academicProfile.setTenthCgpa(acDto.getTenthCgpa());
            if (acDto.getTenthBoard() != null) academicProfile.setTenthBoard(acDto.getTenthBoard());
            if (acDto.getTenthYearOfPassing() != null) academicProfile.setTenthYearOfPassing(acDto.getTenthYearOfPassing());
            if (acDto.getTwelfthPercentage() != null) academicProfile.setTwelfthPercentage(acDto.getTwelfthPercentage());
            if (acDto.getTwelfthCgpa() != null) academicProfile.setTwelfthCgpa(acDto.getTwelfthCgpa());
            if (acDto.getTwelfthBoard() != null) academicProfile.setTwelfthBoard(acDto.getTwelfthBoard());
            if (acDto.getTwelfthStream() != null) academicProfile.setTwelfthStream(acDto.getTwelfthStream());
            if (acDto.getTwelfthYearOfPassing() != null) academicProfile.setTwelfthYearOfPassing(acDto.getTwelfthYearOfPassing());
            if (acDto.getBranch() != null) academicProfile.setBranch(acDto.getBranch());
            if (acDto.getAdmissionYear() != null) academicProfile.setAdmissionYear(acDto.getAdmissionYear());
            if (acDto.getExpectedGraduationYear() != null) academicProfile.setExpectedGraduationYear(acDto.getExpectedGraduationYear());

            // Bug 2 Fix: Only update semesterSgpas if explicitly provided (non-null in DTO)
            if (acDto.getSemesterSgpas() != null) {
                academicProfile.getSemesterSgpas().clear();
                for (SemesterSgpaDTO sDto : acDto.getSemesterSgpas()) {
                    SemesterSgpa sgpaEntity = SemesterSgpa.builder()
                            .academicProfile(academicProfile)
                            .semesterNumber(sDto.getSemesterNumber())
                            .sgpa(sDto.getSgpa())
                            .isFinalized(sDto.getIsFinalized() != null ? sDto.getIsFinalized() : true)
                            .build();
                    academicProfile.getSemesterSgpas().add(sgpaEntity);
                }
            }

            // Bug 2 Fix: Only update backlogRecords if explicitly provided (non-null in DTO)
            if (acDto.getBacklogRecords() != null) {
                academicProfile.getBacklogRecords().clear();
                for (BacklogRecordDTO bDto : acDto.getBacklogRecords()) {
                    BacklogRecord bEntity = BacklogRecord.builder()
                            .academicProfile(academicProfile)
                            .subjectName(bDto.getSubjectName())
                            .semester(bDto.getSemester())
                            .cleared(bDto.getCleared() != null ? bDto.getCleared() : false)
                            .clearedOn(bDto.getClearedOn())
                            .build();
                    academicProfile.getBacklogRecords().add(bEntity);
                }
            }
        }

        academicProfileRepository.save(academicProfile);

        return mapToFullProfileDTO(student, profile, academicProfile);
    }

    private Student findExistingStudent(Long studentId) {
        Optional<Student> byUser = studentRepository.findByUserId(studentId);
        if (byUser.isPresent()) return byUser.get();

        Optional<Student> byId = studentRepository.findById(studentId);
        if (byId.isPresent()) return byId.get();

        // Read-only check: if user doesn't exist or student entity doesn't exist, throw 404 without mutation
        User user = userRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + studentId));

        return studentRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Student profile not found for user id: " + studentId));
    }

    private StudentFullProfileDTO mapToFullProfileDTO(Student student, StudentProfile profile, AcademicProfile academic) {
        AcademicProfileDTO acDto = null;
        if (academic != null) {
            acDto = AcademicProfileDTO.builder()
                    .tenthPercentage(academic.getTenthPercentage())
                    .tenthCgpa(academic.getTenthCgpa())
                    .tenthBoard(academic.getTenthBoard())
                    .tenthYearOfPassing(academic.getTenthYearOfPassing())
                    .twelfthPercentage(academic.getTwelfthPercentage())
                    .twelfthCgpa(academic.getTwelfthCgpa())
                    .twelfthBoard(academic.getTwelfthBoard())
                    .twelfthStream(academic.getTwelfthStream())
                    .twelfthYearOfPassing(academic.getTwelfthYearOfPassing())
                    .branch(academic.getBranch())
                    .admissionYear(academic.getAdmissionYear())
                    .expectedGraduationYear(academic.getExpectedGraduationYear())
                    .currentCgpa(academic.getCurrentCgpa())
                    .activeBacklogCount(academic.getActiveBacklogCount())
                    .totalEverBacklogCount(academic.getTotalEverBacklogCount())
                    .semesterSgpas(academic.getSemesterSgpas().stream().map(s -> SemesterSgpaDTO.builder()
                            .id(s.getId())
                            .semesterNumber(s.getSemesterNumber())
                            .sgpa(s.getSgpa())
                            .isFinalized(s.getIsFinalized())
                            .build()).toList())
                    .backlogRecords(academic.getBacklogRecords().stream().map(b -> BacklogRecordDTO.builder()
                            .id(b.getId())
                            .subjectName(b.getSubjectName())
                            .semester(b.getSemester())
                            .cleared(b.getCleared())
                            .clearedOn(b.getClearedOn())
                            .build()).toList())
                    .build();
        }

        // Dual-read fallback logic for Certifications
        List<CertificationDTO> certList = new ArrayList<>();
        if (profile != null && profile.getStructuredCertifications() != null && !profile.getStructuredCertifications().isEmpty()) {
            certList = profile.getStructuredCertifications().stream().map(c -> CertificationDTO.builder()
                    .id(c.getId())
                    .name(c.getName())
                    .issuingBody(c.getIssuingBody())
                    .issueDate(c.getIssueDate())
                    .credentialUrl(c.getCredentialUrl())
                    .build()).toList();
        } else if (profile != null && profile.getCertifications() != null && !profile.getCertifications().isBlank()) {
            certList = parseLegacyCertifications(profile.getCertifications());
        }

        return StudentFullProfileDTO.builder()
                .studentId(student.getId())
                .collegeId(student.getCollegeId())
                .collegeRollNumber(student.getCollegeRollNumber())
                .phoneNumber(student.getPhoneNumber())
                .placementStatus(student.getPlacementStatus())
                .email(student.getUser() != null ? student.getUser().getEmail() : null)
                .linkedinUrl(profile != null ? profile.getLinkedinUrl() : null)
                .githubUrl(profile != null ? profile.getGithubUrl() : null)
                .portfolioUrl(profile != null ? profile.getPortfolioUrl() : null)
                .summary(profile != null ? profile.getSummary() : null)
                .achievements(profile != null ? profile.getAchievements() : null)
                .academicProfile(acDto)
                .certifications(certList)
                .skills(profile != null ? parseJson(profile.getSkills()) : Collections.emptyList())
                .projects(profile != null ? parseJson(profile.getProjects()) : Collections.emptyList())
                .internships(profile != null ? parseJson(profile.getInternships()) : Collections.emptyList())
                .codingProfiles(profile != null ? parseJson(profile.getCodingProfiles()) : Collections.emptyMap())
                .build();
    }

    private List<CertificationDTO> parseLegacyCertifications(String json) {
        try {
            List<Map<String, Object>> rawList = objectMapper.readValue(json, new TypeReference<>() {});
            List<CertificationDTO> list = new ArrayList<>();
            for (Map<String, Object> map : rawList) {
                String name = (String) map.getOrDefault("name", map.getOrDefault("title", "Certification"));
                String issuer = (String) map.getOrDefault("issuingBody", map.getOrDefault("issuer", "Unknown"));
                String credUrl = (String) map.get("credentialUrl");
                list.add(CertificationDTO.builder()
                        .name(name)
                        .issuingBody(issuer)
                        .credentialUrl(credUrl)
                        .build());
            }
            return list;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private Object parseJson(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            return objectMapper.readValue(json, Object.class);
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
