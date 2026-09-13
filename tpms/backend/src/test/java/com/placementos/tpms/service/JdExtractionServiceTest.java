package com.placementos.tpms.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.placementos.tpms.dto.RoleProfileDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class JdExtractionServiceTest {

    private JdExtractionService jdExtractionService;

    @BeforeEach
    void setUp() {
        PdfParserService pdfParserService = new PdfParserService();
        ObjectMapper objectMapper = new ObjectMapper();
        jdExtractionService = new JdExtractionService(pdfParserService, objectMapper);
        ReflectionTestUtils.setField(jdExtractionService, "mockMode", true);
        ReflectionTestUtils.setField(jdExtractionService, "apiKey", "mock_key");
    }

    @Test
    void testExtractJdFromText_NormalizesWeightsAndFallbackSkillKeys() throws Exception {
        RoleProfileDTO profile = jdExtractionService.extractJdFromText("We are looking for a Senior Java Backend Engineer with Spring Boot and AWS.");

        assertNotNull(profile);
        assertNotNull(profile.getRequiredSkills());
        assertTrue(profile.getRequiredSkills().containsKey("MUST_HAVE"));
        assertTrue(profile.getRequiredSkills().containsKey("NICE_TO_HAVE"));
        assertTrue(profile.getRequiredSkills().containsKey("BONUS"));

        assertNotNull(profile.getWeights());
        double weightSum = profile.getWeights().values().stream().mapToDouble(Double::doubleValue).sum();
        assertTrue(weightSum >= 0.98 && weightSum <= 1.02, "Weights sum should normalize close to 1.0");
    }
}
