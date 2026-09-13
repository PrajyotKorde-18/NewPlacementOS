package com.placementos.tpms.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.placementos.tpms.dto.RejectionFeedbackDTO;
import com.placementos.tpms.entity.*;
import com.placementos.tpms.repository.ApplicationRepository;
import com.placementos.tpms.repository.RoleProfileRepository;
import com.placementos.tpms.repository.StudentProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeedbackGeneratorServiceTest {

    @Mock
    private ApplicationRepository applicationRepository;
    @Mock
    private RoleProfileRepository roleProfileRepository;
    @Mock
    private StudentProfileRepository studentProfileRepository;
    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private FeedbackGeneratorService feedbackGeneratorService;

    private Application app;
    private Drive drive;
    private Student student;
    private RoleProfile roleProfile;
    private StudentProfile studentProfile;

    @BeforeEach
    void setUp() throws Exception {
        drive = Drive.builder().id(10L).companyName("Amazon").roleTitle("SDE 1").vacancies(2).build();
        User user = User.builder().id(1L).name("John Student").email("john@rknec.edu").build();
        student = Student.builder().id(100L).user(user).collegeId("CS100").build();

        app = Application.builder()
                .id(50L)
                .drive(drive)
                .student(student)
                .status(ApplicationStatus.NOT_SHORTLISTED)
                .rankPosition(5)
                .score(70.0)
                .scoreBreakdown(objectMapper.writeValueAsString(Map.of(
                        "skillMatchScore", 65.0,        // Lowest sub-score (relatively weakest)
                        "projectRelevanceScore", 70.0,
                        "codingActivityScore", 72.0,
                        "certificationsScore", 68.0,
                        "experienceScore", 75.0,
                        "totalWeightedScore", 70.0
                )))
                .build();

        roleProfile = RoleProfile.builder()
                .id(1L)
                .drive(drive)
                .requiredSkills(objectMapper.writeValueAsString(Map.of(
                        "MUST_HAVE", List.of("Java", "AWS", "System Design"),
                        "NICE_TO_HAVE", List.of("Docker"),
                        "BONUS", List.of("Kubernetes")
                )))
                .weights(objectMapper.writeValueAsString(Map.of("skillMatch", 0.4, "projectRelevance", 0.25)))
                .build();

        studentProfile = StudentProfile.builder()
                .studentId(100L)
                .student(student)
                .skills(objectMapper.writeValueAsString(List.of("Java", "MySQL")))
                .build();
    }

    @Test
    @DisplayName("Weakest Category Identification: Always reports relatively weakest category even when all sub-scores > 60")
    void testGenerateFeedback_AlwaysReportsRelativelyWeakestCategoryEvenWithHighScores() throws Exception {
        // All sub-scores > 60.0 (Skill Match: 85, Projects: 90, Coding: 75, Certifications: 80, Experience: 70)
        app.setScoreBreakdown(objectMapper.writeValueAsString(Map.of(
                "skillMatchScore", 85.0,
                "projectRelevanceScore", 90.0,
                "codingActivityScore", 75.0,
                "certificationsScore", 80.0,
                "experienceScore", 70.0, // Minimum sub-score among all > 60
                "totalWeightedScore", 82.0
        )));

        when(applicationRepository.findById(50L)).thenReturn(Optional.of(app));
        when(roleProfileRepository.findByDriveId(10L)).thenReturn(Optional.of(roleProfile));
        when(studentProfileRepository.findByStudentId(100L)).thenReturn(Optional.of(studentProfile));

        RejectionFeedbackDTO feedback = feedbackGeneratorService.generateFeedback(50L);

        assertNotNull(feedback);
        assertEquals(ApplicationStatus.NOT_SHORTLISTED, feedback.getStatus());

        // Assert relatively weakest category is correctly identified as Experience (70.0/100)
        assertNotNull(feedback.getRelativelyWeakestCategory());
        assertEquals("Experience (70.0/100)", feedback.getRelativelyWeakestCategory());
        assertTrue(feedback.getFeedbackSummary().contains("Primary Growth Focus (Relatively Weakest Area): Experience (Score: 70.0/100)"));
        // Assert no categories are listed under absolute weak (< 60)
        assertTrue(feedback.getWeakCategories().isEmpty());
    }
}
