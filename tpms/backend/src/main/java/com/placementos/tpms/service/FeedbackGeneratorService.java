package com.placementos.tpms.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.placementos.tpms.dto.RejectionFeedbackDTO;
import com.placementos.tpms.dto.RoleProfileDTO;
import com.placementos.tpms.dto.ScoreBreakdownDTO;
import com.placementos.tpms.entity.*;
import com.placementos.tpms.exception.ResourceNotFoundException;
import com.placementos.tpms.repository.ApplicationRepository;
import com.placementos.tpms.repository.RoleProfileRepository;
import com.placementos.tpms.repository.StudentProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class FeedbackGeneratorService {

    private final ApplicationRepository applicationRepository;
    private final RoleProfileRepository roleProfileRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public RejectionFeedbackDTO generateFeedback(Long applicationId) {
        Application app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with id: " + applicationId));

        Drive drive = app.getDrive();
        Student student = app.getStudent();
        StudentProfile profile = studentProfileRepository.findByStudentId(student.getId()).orElse(null);
        RoleProfile roleProfile = roleProfileRepository.findByDriveId(drive.getId()).orElse(null);

        RoleProfileDTO roleProfileDTO = parseRoleProfile(roleProfile);
        ScoreBreakdownDTO breakdown = parseBreakdown(app.getScoreBreakdown());
        Set<String> candidateSkills = parseCandidateSkills(profile);

        List<String> missingMustHave = findMissingMustHaveSkills(candidateSkills, roleProfileDTO);
        Map.Entry<String, Double> weakestEntry = findRelativelyWeakestCategory(breakdown);
        String weakestCategoryStr = weakestEntry != null ? String.format("%s (%.1f/100)", weakestEntry.getKey(), weakestEntry.getValue()) : null;
        List<String> weakCategories = findWeakCategories(breakdown);

        String feedbackSummary = buildDeterministicSummary(app, drive, missingMustHave, weakestEntry, weakCategories, breakdown);

        return RejectionFeedbackDTO.builder()
                .applicationId(app.getId())
                .driveId(drive.getId())
                .companyName(drive.getCompanyName())
                .roleTitle(drive.getRoleTitle())
                .studentId(student.getId())
                .studentName(student.getUser() != null ? student.getUser().getName() : null)
                .status(app.getStatus())
                .rankPosition(app.getRankPosition())
                .totalVacancies(drive.getVacancies())
                .totalScore(app.getScore())
                .feedbackSummary(feedbackSummary)
                .missingMustHaveSkills(missingMustHave)
                .relativelyWeakestCategory(weakestCategoryStr)
                .weakCategories(weakCategories)
                .coordinatorOverrideReason(app.getOverrideReason())
                .build();
    }

    private String buildDeterministicSummary(Application app, Drive drive, List<String> missingMustHave, Map.Entry<String, Double> weakestEntry, List<String> weakCategories, ScoreBreakdownDTO breakdown) {
        StringBuilder sb = new StringBuilder();

        // SECTION 1: DECISION VERDICT & DIRECT EXPLANATION
        sb.append("1. DECISION VERDICT & SELECTION/REJECTION EXPLANATION:\n");
        if (app.getStatus() == ApplicationStatus.SHORTLISTED) {
            sb.append("• VERDICT: SHORTLISTED (SELECTED FOR DRIVE)\n");
            sb.append(String.format("• Primary Selection Driver: You achieved a competitive total weighted score of %.1f/100 and ranked #%d out of all applicants, successfully securing 1 of the %d available vacancy slots.\n",
                    app.getScore() != null ? app.getScore() : 0.0,
                    app.getRankPosition() != null ? app.getRankPosition() : 1,
                    drive.getVacancies() != null ? drive.getVacancies() : 1
            ));
            if (missingMustHave.isEmpty()) {
                sb.append("• Prerequisite Compliance: You fulfilled 100% of the mandatory MUST_HAVE technical skills required by ").append(drive.getCompanyName()).append(".\n");
            }
        } else if (app.getStatus() == ApplicationStatus.OVERRIDDEN_INCLUDED) {
            sb.append("• VERDICT: MANUALLY SHORTLISTED (COORDINATOR OVERRIDE)\n");
            sb.append("• Primary Selection Driver: Placement Coordinator manually overrode your application status to INCLUDED.\n");
            sb.append("• Coordinator Justification: ").append(app.getOverrideReason() != null ? app.getOverrideReason() : "Direct candidate inclusion.").append("\n");
        } else if (app.getStatus() == ApplicationStatus.OVERRIDDEN_EXCLUDED) {
            sb.append("• VERDICT: MANUALLY EXCLUDED (COORDINATOR OVERRIDE)\n");
            sb.append("• Primary Rejection Driver: Placement Coordinator manually overrode your application status to EXCLUDED.\n");
            sb.append("• Coordinator Justification: ").append(app.getOverrideReason() != null ? app.getOverrideReason() : "No specific reason logged.").append("\n");
        } else if (app.getStatus() == ApplicationStatus.NOT_SHORTLISTED) {
            sb.append("• VERDICT: NOT SHORTLISTED (REJECTED FOR DRIVE)\n");
            if (app.getRankPosition() != null && drive.getVacancies() != null) {
                sb.append(String.format("• Primary Rejection Driver: You achieved a total score of %.1f/100 and ranked #%d out of all applicants. Since %s offered %d vacancy slot(s), your rank fell outside the shortlist cutoff.\n",
                        app.getScore() != null ? app.getScore() : 0.0,
                        app.getRankPosition(),
                        drive.getCompanyName(),
                        drive.getVacancies()
                ));
            }
            if (!missingMustHave.isEmpty()) {
                sb.append("• Technical Prerequisite Gap: You are missing ").append(missingMustHave.size()).append(" essential MUST_HAVE skill(s): ").append(String.join(", ", missingMustHave)).append(".\n");
            }
        } else {
            sb.append("• VERDICT: PENDING EVALUATION (Status: ").append(app.getStatus()).append(")\n");
        }
        sb.append("\n");

        // SECTION 2: KEY STRENGTHS & HIGHLIGHTS
        sb.append("2. KEY CANDIDATE STRENGTHS & HIGHLIGHTS:\n");
        boolean hasStrength = false;
        if (breakdown != null) {
            if (breakdown.getProjectRelevanceScore() != null && breakdown.getProjectRelevanceScore() >= 60.0) {
                sb.append(String.format("• Strong Project Alignment: Project Relevance score is %.1f/100, demonstrating relevant project work.\n", breakdown.getProjectRelevanceScore()));
                hasStrength = true;
            }
            if (breakdown.getSkillMatchScore() != null && breakdown.getSkillMatchScore() >= 60.0) {
                sb.append(String.format("• High Technical Skill Coverage: Skill Match score is %.1f/100.\n", breakdown.getSkillMatchScore()));
                hasStrength = true;
            }
            if (breakdown.getCodingActivityScore() != null && breakdown.getCodingActivityScore() >= 60.0) {
                sb.append(String.format("• Active Coding Presence: Coding Activity score is %.1f/100 (GitHub/LeetCode profiles verified).\n", breakdown.getCodingActivityScore()));
                hasStrength = true;
            }
            if (breakdown.getCertificationsScore() != null && breakdown.getCertificationsScore() >= 60.0) {
                sb.append(String.format("• Certified Industry Skills: Certifications score is %.1f/100.\n", breakdown.getCertificationsScore()));
                hasStrength = true;
            }
        }
        if (!hasStrength) {
            sb.append("• Candidate has basic eligibility but needs development across core evaluation dimensions.\n");
        }
        sb.append("\n");

        // SECTION 3: PRIMARY GAPS & REJECTION DRIVERS
        sb.append("3. PRIMARY GAPS & REJECTION DRIVERS:\n");
        if (!missingMustHave.isEmpty()) {
            sb.append("• Missing MUST_HAVE Skills: ").append(String.join(", ", missingMustHave)).append(".\n");
        } else {
            sb.append("• Missing MUST_HAVE Skills: None (All mandatory skills present).\n");
        }
        if (weakestEntry != null) {
            String tone = weakestEntry.getValue() < 60.0 ? "Needs Significant Improvement" : "Relatively Weakest Area";
            sb.append(String.format("• Primary Growth Focus (%s): %s (Score: %.1f/100).\n", tone, weakestEntry.getKey(), weakestEntry.getValue()));
        }
        if (!weakCategories.isEmpty()) {
            sb.append("• Sub-Scores Below 60/100 Cutoff: ").append(String.join(", ", weakCategories)).append(".\n");
        }
        sb.append("\n");

        // SECTION 4: ACTIONABLE GROWTH ROADMAP
        sb.append("4. ACTIONABLE GROWTH ROADMAP:\n");
        if (!missingMustHave.isEmpty()) {
            sb.append("1. Skill Acquisition: Learn and build a hands-on project featuring missing skills: ").append(String.join(", ", missingMustHave)).append(".\n");
        } else {
            sb.append("1. Advanced Skill Mastery: Expand knowledge into NICE_TO_HAVE and BONUS skills for this role.\n");
        }
        if (weakestEntry != null) {
            if ("Coding Activity".equalsIgnoreCase(weakestEntry.getKey())) {
                sb.append("2. Coding Profile: Add GitHub repository links and competitive programming handles to your student profile.\n");
            } else if ("Certifications".equalsIgnoreCase(weakestEntry.getKey())) {
                sb.append("2. Professional Certifications: Earn verified industry certifications (e.g. AWS, Oracle, Google Cloud).\n");
            } else if ("Project Relevance".equalsIgnoreCase(weakestEntry.getKey())) {
                sb.append("2. Project Showcase: Add complex end-to-end projects matching tech stack requirements.\n");
            } else {
                sb.append("2. Targeted Practice: Focus on boosting your ").append(weakestEntry.getKey()).append(" score before the next drive.\n");
            }
        }
        sb.append("3. Academic Setup: Keep semester SGPAs and resume profile updated for real-time candidate ranking.");

        return sb.toString();
    }

    private Map.Entry<String, Double> findRelativelyWeakestCategory(ScoreBreakdownDTO breakdown) {
        if (breakdown == null) return null;

        Map<String, Double> categories = new LinkedHashMap<>();
        if (breakdown.getSkillMatchScore() != null) categories.put("Skill Match", breakdown.getSkillMatchScore());
        if (breakdown.getProjectRelevanceScore() != null) categories.put("Project Relevance", breakdown.getProjectRelevanceScore());
        if (breakdown.getCodingActivityScore() != null) categories.put("Coding Activity", breakdown.getCodingActivityScore());
        if (breakdown.getCertificationsScore() != null) categories.put("Certifications", breakdown.getCertificationsScore());
        if (breakdown.getExperienceScore() != null) categories.put("Experience", breakdown.getExperienceScore());

        if (categories.isEmpty()) return null;

        return categories.entrySet().stream()
                .min(Map.Entry.comparingByValue())
                .orElse(null);
    }

    private List<String> findMissingMustHaveSkills(Set<String> candidateSkills, RoleProfileDTO roleProfileDTO) {
        if (roleProfileDTO == null || roleProfileDTO.getRequiredSkills() == null) {
            return Collections.emptyList();
        }
        List<String> mustHave = roleProfileDTO.getRequiredSkills().getOrDefault("MUST_HAVE", Collections.emptyList());
        List<String> missing = new ArrayList<>();
        for (String skill : mustHave) {
            if (!candidateSkills.contains(skill.trim().toLowerCase())) {
                missing.add(skill);
            }
        }
        return missing;
    }

    private List<String> findWeakCategories(ScoreBreakdownDTO breakdown) {
        if (breakdown == null) return Collections.emptyList();

        List<String> weak = new ArrayList<>();
        if (breakdown.getSkillMatchScore() != null && breakdown.getSkillMatchScore() < 60.0) {
            weak.add(String.format("Skill Match (%.1f/100)", breakdown.getSkillMatchScore()));
        }
        if (breakdown.getProjectRelevanceScore() != null && breakdown.getProjectRelevanceScore() < 60.0) {
            weak.add(String.format("Project Relevance (%.1f/100)", breakdown.getProjectRelevanceScore()));
        }
        if (breakdown.getCodingActivityScore() != null && breakdown.getCodingActivityScore() < 60.0) {
            weak.add(String.format("Coding Activity (%.1f/100)", breakdown.getCodingActivityScore()));
        }
        if (breakdown.getCertificationsScore() != null && breakdown.getCertificationsScore() < 60.0) {
            weak.add(String.format("Certifications (%.1f/100)", breakdown.getCertificationsScore()));
        }
        if (breakdown.getExperienceScore() != null && breakdown.getExperienceScore() < 60.0) {
            weak.add(String.format("Experience (%.1f/100)", breakdown.getExperienceScore()));
        }
        return weak;
    }

    private Set<String> parseCandidateSkills(StudentProfile profile) {
        if (profile == null || profile.getSkills() == null) return Collections.emptySet();
        try {
            List<String> skills = objectMapper.readValue(profile.getSkills(), new TypeReference<>() {});
            Set<String> set = new HashSet<>();
            for (String s : skills) {
                set.add(s.trim().toLowerCase());
            }
            return set;
        } catch (Exception e) {
            return Collections.emptySet();
        }
    }

    private ScoreBreakdownDTO parseBreakdown(String json) {
        if (json == null) return null;
        try {
            return objectMapper.readValue(json, ScoreBreakdownDTO.class);
        } catch (Exception e) {
            return null;
        }
    }

    private RoleProfileDTO parseRoleProfile(RoleProfile roleProfile) {
        if (roleProfile == null) return null;
        try {
            Map<String, List<String>> skills = objectMapper.readValue(roleProfile.getRequiredSkills(), new TypeReference<>() {});
            Map<String, Double> weights = objectMapper.readValue(roleProfile.getWeights(), new TypeReference<>() {});
            return RoleProfileDTO.builder()
                    .id(roleProfile.getId())
                    .requiredSkills(skills)
                    .weights(weights)
                    .build();
        } catch (Exception e) {
            return null;
        }
    }
}
