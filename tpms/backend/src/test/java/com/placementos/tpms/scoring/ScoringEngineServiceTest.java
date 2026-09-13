package com.placementos.tpms.scoring;

import com.placementos.tpms.dto.ParsedResumeDTO;
import com.placementos.tpms.dto.RoleProfileDTO;
import com.placementos.tpms.dto.ScoreBreakdownDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ScoringEngineServiceTest {

    private ScoringEngineService scoringEngineService;

    @BeforeEach
    void setUp() {
        List<ScoringStrategy> strategies = List.of(
                new SkillMatchScoringStrategy(),
                new ProjectRelevanceScoringStrategy(),
                new CodingActivityScoringStrategy(),
                new CertificationsScoringStrategy(),
                new ExperienceScoringStrategy()
        );
        scoringEngineService = new ScoringEngineService(strategies);
    }

    @Test
    @DisplayName("Must-Have Skills: Candidate meeting all MUST_HAVE skills scores higher than candidate missing them")
    void testMustHaveSkills_CandidateMeetingMustHaveScoresHigher() {
        RoleProfileDTO roleProfile = RoleProfileDTO.builder()
                .requiredSkills(Map.of(
                        "MUST_HAVE", List.of("Java", "Spring Boot", "MySQL"),
                        "NICE_TO_HAVE", List.of("Docker")
                ))
                .build();

        ParsedResumeDTO candidateWithMustHave = ParsedResumeDTO.builder()
                .skills(List.of("Java", "Spring Boot", "MySQL", "Docker"))
                .build();

        ParsedResumeDTO candidateMissingMustHave = ParsedResumeDTO.builder()
                .skills(List.of("Docker", "Python"))
                .build();

        ScoreBreakdownDTO scoreWithMustHave = scoringEngineService.computeScoreBreakdown(candidateWithMustHave, roleProfile);
        ScoreBreakdownDTO scoreMissingMustHave = scoringEngineService.computeScoreBreakdown(candidateMissingMustHave, roleProfile);

        assertTrue(scoreWithMustHave.getSkillMatchScore() > scoreMissingMustHave.getSkillMatchScore(),
                "Candidate meeting MUST_HAVE skills should score higher than one missing them");
        assertEquals(100.0, scoreWithMustHave.getSkillMatchScore(), 0.01);
        assertEquals(18.18, scoreMissingMustHave.getSkillMatchScore(), 0.1); // 2 pts earned / 11 total pts = 18.18%
    }

    @Test
    @DisplayName("Weighted Sum Arithmetic: Computes total score matching hand-calculated expected value")
    void testWeightedSumArithmetic_MatchesHandComputedValue() {
        RoleProfileDTO roleProfile = RoleProfileDTO.builder()
                .weights(Map.of(
                        "skillMatch", 0.40,
                        "projectRelevance", 0.30,
                        "codingActivity", 0.10,
                        "certifications", 0.10,
                        "experience", 0.10
                ))
                .build();

        // SkillMatch: 3 MUST_HAVE skills out of 3 = 100.0 * 0.40 = 40.0
        // ProjectRelevance: 2 projects out of max = 80.0 * 0.30 = 24.0
        // CodingActivity: 1 profile = 50.0 * 0.10 = 5.0
        // Certifications: 1 cert = 40.0 * 0.10 = 4.0
        // Experience: 1 internship = 50.0 * 0.10 = 5.0
        // Total Expected = 40.0 + 24.0 + 5.0 + 4.0 + 5.0 = 78.0
        ParsedResumeDTO studentProfile = ParsedResumeDTO.builder()
                .skills(List.of("Java", "Spring Boot", "React"))
                .projects(List.of(
                        Map.of("title", "Project 1", "description", "Built a fullstack web app with Java Spring Boot and React"),
                        Map.of("title", "Project 2", "description", "Data pipeline in Java")
                ))
                .codingProfiles(Map.of("github", "https://github.com/user"))
                .certifications(List.of(Map.of("name", "AWS Certified Cloud Practitioner")))
                .internships(List.of(Map.of("title", "Backend Intern", "durationMonths", 3)))
                .build();

        RoleProfileDTO roleWithSkills = RoleProfileDTO.builder()
                .requiredSkills(Map.of("MUST_HAVE", List.of("Java", "Spring Boot", "React")))
                .weights(roleProfile.getWeights())
                .build();

        ScoreBreakdownDTO breakdown = scoringEngineService.computeScoreBreakdown(studentProfile, roleWithSkills);

        assertEquals(100.0, breakdown.getSkillMatchScore(), 0.01);
        assertEquals(60.0, breakdown.getProjectRelevanceScore(), 0.01);
        assertEquals(40.0, breakdown.getCodingActivityScore(), 0.01);
        assertEquals(40.0, breakdown.getCertificationsScore(), 0.01);
        assertEquals(50.0, breakdown.getExperienceScore(), 0.01);

        // Hand-computed expected: (100*0.40) + (60*0.30) + (40*0.10) + (40*0.10) + (50*0.10) = 40 + 18 + 4 + 4 + 5 = 71.0
        assertEquals(71.0, breakdown.getTotalWeightedScore(), 0.01);
    }

    @Test
    @DisplayName("Role Weight Normalization: Weights summing to non-1.0 get normalized correctly")
    void testRoleWeightNormalization_NonOneSumWeightsGetNormalized() {
        // Weights sum to 100.0 instead of 1.0
        RoleProfileDTO roleProfile = RoleProfileDTO.builder()
                .weights(Map.of(
                        "skillMatch", 50.0,
                        "projectRelevance", 50.0,
                        "codingActivity", 0.0,
                        "certifications", 0.0,
                        "experience", 0.0
                ))
                .requiredSkills(Map.of("MUST_HAVE", List.of("Java")))
                .build();

        ParsedResumeDTO studentProfile = ParsedResumeDTO.builder()
                .skills(List.of("Java"))
                .projects(Collections.emptyList())
                .build();

        ScoreBreakdownDTO breakdown = scoringEngineService.computeScoreBreakdown(studentProfile, roleProfile);

        assertEquals(100.0, breakdown.getSkillMatchScore(), 0.01);
        assertEquals(0.0, breakdown.getProjectRelevanceScore(), 0.01);

        // Weighted sum = (100.0 * 50.0) + (0.0 * 50.0) = 5000.0
        // Total weight = 100.0
        // Normalized score = 5000.0 / 100.0 = 50.0
        assertEquals(50.0, breakdown.getTotalWeightedScore(), 0.01);
    }

    @Test
    @DisplayName("Regression Test: Candidate with DSA absent from skills but present in achievements text scores full MUST_HAVE match")
    void testDsaInAchievementsFallback_ScoresFullSkillMatch() {
        RoleProfileDTO roleProfile = RoleProfileDTO.builder()
                .requiredSkills(Map.of("MUST_HAVE", List.of("DSA")))
                .build();

        ParsedResumeDTO candidateWithDsaInAchievements = ParsedResumeDTO.builder()
                .skills(List.of("Java", "Spring Boot", "MySQL")) // "DSA" absent from declared skills
                .achievements("Solved 500+ DSA problems on competitive programming platforms.")
                .summary("Backend and AI Engineer.")
                .build();

        ScoreBreakdownDTO scoreBreakdown = scoringEngineService.computeScoreBreakdown(candidateWithDsaInAchievements, roleProfile);

        assertEquals(100.0, scoreBreakdown.getSkillMatchScore(), 0.01,
                "Candidate with DSA in achievements text should score full skill match when DSA is required as MUST_HAVE");
    }
}
