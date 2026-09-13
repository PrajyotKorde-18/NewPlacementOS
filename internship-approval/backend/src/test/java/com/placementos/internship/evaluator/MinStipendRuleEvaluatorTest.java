package com.placementos.internship.evaluator;

import com.placementos.internship.dto.RuleEvaluationResultDTO;
import com.placementos.internship.entity.InternshipRequest;
import com.placementos.internship.entity.PolicyRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class MinStipendRuleEvaluatorTest {

    private MinStipendRuleEvaluator evaluator;
    private PolicyRule stipendRule;

    @BeforeEach
    void setUp() {
        evaluator = new MinStipendRuleEvaluator();
        stipendRule = PolicyRule.builder()
                .ruleName("MINIMUM_STIPEND")
                .ruleType("STIPEND")
                .ruleValue("5000")
                .label("Minimum Monthly Stipend")
                .isActive(true)
                .build();
    }

    @Test
    @DisplayName("Boundary Test: Exactly at minimum threshold (5000) -> PASS")
    void testEvaluate_ExactlyAtMinimumThreshold_Passes() {
        InternshipRequest request = InternshipRequest.builder()
                .stipend(new BigDecimal("5000"))
                .build();

        RuleEvaluationResultDTO result = evaluator.evaluate(request, stipendRule);

        assertTrue(result.isPassed());
        assertEquals("MINIMUM_STIPEND", result.getRuleName());
        assertNull(result.getFailureReason());
    }

    @Test
    @DisplayName("Boundary Test: One rupee below minimum threshold (4999) -> FAIL")
    void testEvaluate_OneRupeeBelowMinimumThreshold_Fails() {
        InternshipRequest request = InternshipRequest.builder()
                .stipend(new BigDecimal("4999"))
                .build();

        RuleEvaluationResultDTO result = evaluator.evaluate(request, stipendRule);

        assertFalse(result.isPassed());
        assertEquals("MINIMUM_STIPEND", result.getRuleName());
        assertNotNull(result.getFailureReason());
        assertTrue(result.getFailureReason().contains("below required minimum threshold"));
    }

    @Test
    @DisplayName("Boundary Test: Well below minimum threshold (2000) -> FAIL")
    void testEvaluate_WellBelowMinimumThreshold_Fails() {
        InternshipRequest request = InternshipRequest.builder()
                .stipend(new BigDecimal("2000"))
                .build();

        RuleEvaluationResultDTO result = evaluator.evaluate(request, stipendRule);

        assertFalse(result.isPassed());
        assertEquals("MINIMUM_STIPEND", result.getRuleName());
        assertNotNull(result.getFailureReason());
        assertTrue(result.getFailureReason().contains("below required minimum threshold"));
    }
}
