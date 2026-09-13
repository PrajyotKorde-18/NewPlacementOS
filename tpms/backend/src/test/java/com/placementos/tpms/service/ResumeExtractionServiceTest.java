package com.placementos.tpms.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.placementos.tpms.dto.ParsedResumeDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class ResumeExtractionServiceTest {

    private ResumeExtractionService resumeExtractionService;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        resumeExtractionService = new ResumeExtractionService(objectMapper);
        ReflectionTestUtils.setField(resumeExtractionService, "mockMode", true);
        ReflectionTestUtils.setField(resumeExtractionService, "apiKey", "mock_key");
    }

    @Test
    void testExtractStructuredData_IntentionalMockLlmMode() throws Exception {
        String sampleResumeText = "John Doe\nSkills: Java, Spring Boot, MySQL\nGitHub: https://github.com/mockstudent";

        ParsedResumeDTO result = resumeExtractionService.extractStructuredData(sampleResumeText);

        assertNotNull(result);
        assertTrue(result.getSkills().contains("Java"));
        assertTrue(result.getSkills().contains("Spring Boot"));
        assertEquals("https://github.com/mockstudent", result.getCodingProfiles().get("github"));
        assertNotNull(result.getSummary());
        assertTrue(result.getSummary().contains("DSA"));
        assertNotNull(result.getAchievements());
        assertTrue(result.getAchievements().contains("DSA"));
    }

    @Test
    void testExtractStructuredData_RealApiFailure_ThrowsExceptionWithoutMockFallback() {
        // Configure for Real API mode with invalid URL to trigger genuine API failure
        ReflectionTestUtils.setField(resumeExtractionService, "mockMode", false);
        ReflectionTestUtils.setField(resumeExtractionService, "apiKey", "real_api_key_xyz");
        ReflectionTestUtils.setField(resumeExtractionService, "groqUrl", "http://invalid-host-unreachable:9999/chat");

        String sampleText = "Sample Resume Text long enough to exceed twenty characters limit";

        // Must throw RuntimeException on real API failure and NEVER fall back to mock data
        assertThrows(RuntimeException.class, () -> resumeExtractionService.extractStructuredData(sampleText));
    }
}
