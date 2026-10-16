package com.placementos.tpms.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.placementos.tpms.dto.*;
import com.placementos.tpms.entity.*;
import com.placementos.tpms.exception.AppException;
import com.placementos.tpms.exception.ResourceNotFoundException;
import com.placementos.tpms.repository.*;
import com.placementos.tpms.scoring.ScoringEngineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import com.placementos.tpms.event.DrivePublishedEvent;
import com.placementos.tpms.event.RejectionFeedbackAvailableEvent;
import com.placementos.tpms.event.StudentShortlistedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class DriveService {

    private final DriveRepository driveRepository;
    private final RoleProfileRepository roleProfileRepository;
    private final ApplicationRepository applicationRepository;
    private final StudentRepository studentRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final AcademicProfileRepository academicProfileRepository;
    private final UserRepository userRepository;
    private final ScoringEngineService scoringEngineService;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public DriveDTO createDrive(DriveDTO driveDTO, String userEmail) {
        User creator = null;
        if (userEmail != null && !userEmail.trim().isEmpty()) {
            creator = userRepository.findByEmail(userEmail).orElse(null);
        }
        if (creator == null) {
            creator = userRepository.findByEmail("coord_user@rknec.edu")
                    .orElseGet(() -> userRepository.findAll().stream().findFirst()
                            .orElseThrow(() -> new ResourceNotFoundException("No coordinator user found in system")));
        }

        Drive drive = Drive.builder()
                .collegeId(driveDTO.getCollegeId())
                .companyName(driveDTO.getCompanyName())
                .roleTitle(driveDTO.getRoleTitle())
                .vacancies(driveDTO.getVacancies() != null ? driveDTO.getVacancies() : 1)
                .ctc(driveDTO.getCtc() != null ? driveDTO.getCtc() : "Not Specified")
                .createdBy(creator)
                .build();

        drive = driveRepository.save(drive);

        RoleProfileDTO roleProfileDTO = driveDTO.getRoleProfile();
        if (roleProfileDTO != null) {
            saveRoleProfile(drive, roleProfileDTO);
        }

        // Publish event for transactional notification listener
        eventPublisher.publishEvent(new DrivePublishedEvent(drive.getId()));

        return mapToDriveDTO(drive);
    }

    @Transactional(readOnly = true)
    public List<DriveDTO> getAllDrives() {
        List<Drive> drives = driveRepository.findAll();
        List<DriveDTO> dtos = new ArrayList<>();
        for (Drive d : drives) {
            dtos.add(mapToDriveDTO(d));
        }
        return dtos;
    }

    @Transactional(readOnly = true)
    public DriveDTO getDrive(Long driveId) {
        Drive drive = driveRepository.findById(driveId)
                .orElseThrow(() -> new ResourceNotFoundException("Drive not found with id: " + driveId));
        return mapToDriveDTO(drive);
    }

    @Transactional
    public DriveDTO updateDrive(Long driveId, DriveDTO driveDTO) {
        Drive drive = driveRepository.findById(driveId)
                .orElseThrow(() -> new ResourceNotFoundException("Drive not found with id: " + driveId));
        if (driveDTO.getCompanyName() != null && !driveDTO.getCompanyName().trim().isEmpty()) {
            drive.setCompanyName(driveDTO.getCompanyName());
        }
        if (driveDTO.getRoleTitle() != null && !driveDTO.getRoleTitle().trim().isEmpty()) {
            drive.setRoleTitle(driveDTO.getRoleTitle());
        }
        if (driveDTO.getVacancies() != null && driveDTO.getVacancies() >= 1) {
            drive.setVacancies(driveDTO.getVacancies());
        }
        if (driveDTO.getCtc() != null) {
            drive.setCtc(driveDTO.getCtc());
        }
        drive = driveRepository.save(drive);
        return mapToDriveDTO(drive);
    }

    @Transactional
    public void deleteDrive(Long driveId) {
        Drive drive = driveRepository.findById(driveId)
                .orElseThrow(() -> new ResourceNotFoundException("Drive not found with id: " + driveId));
        
        roleProfileRepository.findByDriveId(driveId).ifPresent(roleProfileRepository::delete);
        
        List<Application> applications = applicationRepository.findByDriveIdOrderByScoreDesc(driveId);
        if (!applications.isEmpty()) {
            applicationRepository.deleteAll(applications);
        }

        driveRepository.delete(drive);
        log.info("Drive #{} ('{}') successfully deleted from database.", driveId, drive.getCompanyName());
    }

    @Transactional
    public ApplicationResponseDTO applyToDrive(Long driveId, Long studentId) {
        Drive drive = driveRepository.findById(driveId)
                .orElseThrow(() -> new ResourceNotFoundException("Drive not found with id: " + driveId));

        Student student = resolveStudent(studentId);

        if (applicationRepository.existsByDriveIdAndStudentId(driveId, student.getId())) {
            throw new AppException("Student has already applied to this drive", HttpStatus.CONFLICT);
        }

        // Academic Eligibility Validation
        RoleProfileDTO roleProfileDTO = getRoleProfileDTO(driveId);
        if (roleProfileDTO != null) {
            AcademicProfile academicProfile = academicProfileRepository.findByStudentId(student.getId()).orElse(null);
            validateAcademicEligibility(student, academicProfile, roleProfileDTO);
        }

        Application application = Application.builder()
                .drive(drive)
                .student(student)
                .status(ApplicationStatus.PENDING)
                .build();

        application = applicationRepository.save(application);
        return mapToApplicationResponseDTO(application);
    }

    private void validateAcademicEligibility(Student student, AcademicProfile academic, RoleProfileDTO roleProfile) {
        if (roleProfile == null) return;

        // 1. Min CGPA Check
        if (roleProfile.getMinCgpa() != null && roleProfile.getMinCgpa() > 0) {
            Double currentCgpa = academic != null ? academic.getCurrentCgpa() : null;
            if (currentCgpa == null || currentCgpa < roleProfile.getMinCgpa()) {
                throw new AppException(String.format(
                        "Academic Eligibility Check Failed: Drive requires minimum CGPA of %.2f (Your CGPA: %s)",
                        roleProfile.getMinCgpa(),
                        currentCgpa != null ? String.format("%.2f", currentCgpa) : "Not Set"
                ), HttpStatus.BAD_REQUEST);
            }
        }

        // 2. Min 10th % Check
        if (roleProfile.getMinTenthPercentage() != null && roleProfile.getMinTenthPercentage() > 0) {
            Double tenth = academic != null ? academic.getTenthPercentage() : null;
            if (tenth == null || tenth < roleProfile.getMinTenthPercentage()) {
                throw new AppException(String.format(
                        "Academic Eligibility Check Failed: Drive requires minimum 10th Marks of %.1f%% (Your 10th: %s)",
                        roleProfile.getMinTenthPercentage(),
                        tenth != null ? String.format("%.1f%%", tenth) : "Not Set"
                ), HttpStatus.BAD_REQUEST);
            }
        }

        // 3. Min 12th % Check
        if (roleProfile.getMinTwelfthPercentage() != null && roleProfile.getMinTwelfthPercentage() > 0) {
            Double twelfth = academic != null ? academic.getTwelfthPercentage() : null;
            if (twelfth == null || twelfth < roleProfile.getMinTwelfthPercentage()) {
                throw new AppException(String.format(
                        "Academic Eligibility Check Failed: Drive requires minimum 12th Marks of %.1f%% (Your 12th: %s)",
                        roleProfile.getMinTwelfthPercentage(),
                        twelfth != null ? String.format("%.1f%%", twelfth) : "Not Set"
                ), HttpStatus.BAD_REQUEST);
            }
        }

        // 4. Max Backlogs Check
        if (roleProfile.getMaxActiveBacklogs() != null && roleProfile.getMaxActiveBacklogs() >= 0) {
            long activeBacklogs = academic != null ? academic.getActiveBacklogCount() : 0;
            if (activeBacklogs > roleProfile.getMaxActiveBacklogs()) {
                throw new AppException(String.format(
                        "Academic Eligibility Check Failed: Drive permits maximum %d active backlog(s) (Your active backlogs: %d)",
                        roleProfile.getMaxActiveBacklogs(),
                        activeBacklogs
                ), HttpStatus.BAD_REQUEST);
            }
        }
    }

    private Student resolveStudent(Long studentId) {
        Optional<Student> byUser = studentRepository.findByUserId(studentId);
        if (byUser.isPresent()) return byUser.get();

        Optional<Student> byId = studentRepository.findById(studentId);
        if (byId.isPresent()) return byId.get();

        User user = userRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student user not found with id: " + studentId));

        return studentRepository.save(Student.builder()
                .user(user)
                .collegeId(user.getEmail() != null ? user.getEmail().split("@")[0].toUpperCase() : "STUDENT_" + user.getId())
                .build());
    }

    @Transactional
    public List<ApplicationResponseDTO> calculateAndRankApplications(Long driveId) {
        Drive drive = driveRepository.findById(driveId)
                .orElseThrow(() -> new ResourceNotFoundException("Drive not found with id: " + driveId));

        RoleProfileDTO roleProfileDTO = getRoleProfileDTO(driveId);
        List<Application> applications = applicationRepository.findByDriveIdOrderByScoreDesc(driveId);

        if (applications.isEmpty()) {
            return Collections.emptyList();
        }

        // 1. Calculate score & score breakdown for each application
        for (Application app : applications) {
            StudentProfile profile = studentProfileRepository.findByStudentId(app.getStudent().getId()).orElse(null);
            ParsedResumeDTO parsedResume = parseStudentProfile(profile);

            ScoreBreakdownDTO breakdown = scoringEngineService.computeScoreBreakdown(parsedResume, roleProfileDTO);

            app.setScore(breakdown.getTotalWeightedScore());
            try {
                app.setScoreBreakdown(objectMapper.writeValueAsString(breakdown));
            } catch (Exception e) {
                log.error("Failed to serialize score breakdown for app ID {}", app.getId(), e);
            }
        }

        // 2. Sort applications by score descending
        applications.sort((a, b) -> Double.compare(
                b.getScore() != null ? b.getScore() : 0.0,
                a.getScore() != null ? a.getScore() : 0.0
        ));

        // 3. Assign rank position and auto-shortlist top N vacancies
        int vacancies = drive.getVacancies() != null ? drive.getVacancies() : 1;

        for (int i = 0; i < applications.size(); i++) {
            Application app = applications.get(i);
            int rank = i + 1;
            app.setRankPosition(rank);

            // Preserve manual coordinator overrides
            if (app.getStatus() != ApplicationStatus.OVERRIDDEN_INCLUDED && app.getStatus() != ApplicationStatus.OVERRIDDEN_EXCLUDED) {
                // Hard Prerequisite Guard: Candidates with 0 skill match score cannot be auto-shortlisted
                double skillMatchScore = 0.0;
                if (app.getScoreBreakdown() != null) {
                    try {
                        ScoreBreakdownDTO bd = objectMapper.readValue(app.getScoreBreakdown(), ScoreBreakdownDTO.class);
                        if (bd.getSkillMatchScore() != null) skillMatchScore = bd.getSkillMatchScore();
                    } catch (Exception ignored) {}
                }

                if (rank <= vacancies && skillMatchScore > 0.0) {
                    app.setStatus(ApplicationStatus.SHORTLISTED);
                } else {
                    app.setStatus(ApplicationStatus.NOT_SHORTLISTED);
                }
            }
        }

        List<Application> savedApps = applicationRepository.saveAll(applications);

        // Publish events for transactional notification listeners
        for (Application app : savedApps) {
            if (app.getStatus() == ApplicationStatus.SHORTLISTED || app.getStatus() == ApplicationStatus.OVERRIDDEN_INCLUDED) {
                eventPublisher.publishEvent(new StudentShortlistedEvent(app.getId(), drive.getId(), app.getStudent().getId()));
            } else if (app.getStatus() == ApplicationStatus.NOT_SHORTLISTED || app.getStatus() == ApplicationStatus.OVERRIDDEN_EXCLUDED) {
                eventPublisher.publishEvent(new RejectionFeedbackAvailableEvent(app.getId(), drive.getId(), app.getStudent().getId()));
            }
        }

        List<ApplicationResponseDTO> dtos = new ArrayList<>();
        for (Application app : savedApps) {
            dtos.add(mapToApplicationResponseDTO(app));
        }
        return dtos;
    }

    @Transactional(readOnly = true)
    public List<ApplicationResponseDTO> getDriveApplications(Long driveId) {
        List<Application> applications = applicationRepository.findByDriveIdOrderByScoreDesc(driveId);
        List<ApplicationResponseDTO> dtos = new ArrayList<>();
        for (Application app : applications) {
            dtos.add(mapToApplicationResponseDTO(app));
        }
        return dtos;
    }

    private void saveRoleProfile(Drive drive, RoleProfileDTO dto) {
        try {
            String skillsJson = objectMapper.writeValueAsString(dto.getRequiredSkills() != null ? dto.getRequiredSkills() : Collections.emptyMap());
            String weightsJson = objectMapper.writeValueAsString(dto.getWeights() != null ? dto.getWeights() : Collections.emptyMap());

            RoleProfile roleProfile = roleProfileRepository.findByDriveId(drive.getId())
                    .orElseGet(() -> RoleProfile.builder().drive(drive).build());

            roleProfile.setRequiredSkills(skillsJson);
            roleProfile.setWeights(weightsJson);
            roleProfile.setMinCgpa(dto.getMinCgpa());
            roleProfile.setMinTenthPercentage(dto.getMinTenthPercentage());
            roleProfile.setMinTwelfthPercentage(dto.getMinTwelfthPercentage());
            roleProfile.setMaxActiveBacklogs(dto.getMaxActiveBacklogs());
            roleProfile.setExtractionSource(dto.getExtractionSource());

            roleProfileRepository.save(roleProfile);
        } catch (Exception e) {
            log.error("Failed to save RoleProfile for drive ID {}", drive.getId(), e);
        }
    }

    private RoleProfileDTO getRoleProfileDTO(Long driveId) {
        RoleProfile roleProfile = roleProfileRepository.findByDriveId(driveId).orElse(null);
        if (roleProfile == null) return null;

        try {
            Map<String, List<String>> skills = objectMapper.readValue(
                    roleProfile.getRequiredSkills(), new TypeReference<>() {}
            );
            Map<String, Double> weights = objectMapper.readValue(
                    roleProfile.getWeights(), new TypeReference<>() {}
            );

            return RoleProfileDTO.builder()
                    .id(roleProfile.getId())
                    .driveId(driveId)
                    .requiredSkills(skills)
                    .weights(weights)
                    .minCgpa(roleProfile.getMinCgpa())
                    .minTenthPercentage(roleProfile.getMinTenthPercentage())
                    .minTwelfthPercentage(roleProfile.getMinTwelfthPercentage())
                    .maxActiveBacklogs(roleProfile.getMaxActiveBacklogs())
                    .extractionSource(roleProfile.getExtractionSource())
                    .build();
        } catch (Exception e) {
            log.error("Failed to parse RoleProfile JSON for drive ID {}", driveId, e);
            return null;
        }
    }

    private ParsedResumeDTO parseStudentProfile(StudentProfile profile) {
        if (profile == null) {
            return ParsedResumeDTO.builder()
                    .skills(Collections.emptyList())
                    .projects(Collections.emptyList())
                    .internships(Collections.emptyList())
                    .certifications(Collections.emptyList())
                    .codingProfiles(Collections.emptyMap())
                    .summary("")
                    .achievements("")
                    .rawText("")
                    .build();
        }

        try {
            List<String> skills = profile.getSkills() != null ? objectMapper.readValue(profile.getSkills(), new TypeReference<>() {}) : Collections.emptyList();
            List<Map<String, Object>> projects = profile.getProjects() != null ? objectMapper.readValue(profile.getProjects(), new TypeReference<>() {}) : Collections.emptyList();
            List<Map<String, Object>> internships = profile.getInternships() != null ? objectMapper.readValue(profile.getInternships(), new TypeReference<>() {}) : Collections.emptyList();
            List<Map<String, Object>> certifications = profile.getCertifications() != null ? objectMapper.readValue(profile.getCertifications(), new TypeReference<>() {}) : Collections.emptyList();
            Map<String, String> codingProfiles = profile.getCodingProfiles() != null ? objectMapper.readValue(profile.getCodingProfiles(), new TypeReference<>() {}) : Collections.emptyMap();
            String summary = profile.getSummary() != null ? profile.getSummary() : "";
            String achievements = profile.getAchievements() != null ? profile.getAchievements() : "";

            return ParsedResumeDTO.builder()
                    .skills(skills)
                    .projects(projects)
                    .internships(internships)
                    .certifications(certifications)
                    .codingProfiles(codingProfiles)
                    .summary(summary)
                    .achievements(achievements)
                    .build();
        } catch (Exception e) {
            log.error("Failed to parse StudentProfile JSON", e);
            return ParsedResumeDTO.builder()
                    .skills(Collections.emptyList())
                    .projects(Collections.emptyList())
                    .internships(Collections.emptyList())
                    .certifications(Collections.emptyList())
                    .codingProfiles(Collections.emptyMap())
                    .summary("")
                    .achievements("")
                    .build();
        }
    }

    private DriveDTO mapToDriveDTO(Drive drive) {
        RoleProfileDTO roleProfileDTO = getRoleProfileDTO(drive.getId());

        return DriveDTO.builder()
                .id(drive.getId())
                .collegeId(drive.getCollegeId())
                .companyName(drive.getCompanyName())
                .roleTitle(drive.getRoleTitle())
                .vacancies(drive.getVacancies())
                .ctc(drive.getCtc())
                .createdBy(drive.getCreatedBy() != null ? drive.getCreatedBy().getId() : null)
                .createdByName(drive.getCreatedBy() != null ? drive.getCreatedBy().getName() : null)
                .createdAt(drive.getCreatedAt())
                .roleProfile(roleProfileDTO)
                .build();
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
