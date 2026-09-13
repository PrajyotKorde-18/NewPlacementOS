package com.placementos.internship.evaluator;

import com.placementos.internship.dto.RuleEvaluationResultDTO;
import com.placementos.internship.entity.InternshipRequest;
import com.placementos.internship.entity.PolicyRule;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class MinStipendRuleEvaluator implements RuleEvaluator {

    @Override
    public boolean supports(PolicyRule rule) {
        return "MINIMUM_STIPEND".equalsIgnoreCase(rule.getRuleName()) || "STIPEND".equalsIgnoreCase(rule.getRuleType());
    }

    @Override
    public RuleEvaluationResultDTO evaluate(InternshipRequest request, PolicyRule rule) {
        BigDecimal minStipend;
        try {
            minStipend = new BigDecimal(rule.getRuleValue());
        } catch (Exception e) {
            minStipend = BigDecimal.ZERO;
        }

        BigDecimal candidateStipend = request.getStipend() != null ? request.getStipend() : BigDecimal.ZERO;
        boolean passed = candidateStipend.compareTo(minStipend) >= 0;

        String reason = null;
        if (!passed) {
            reason = String.format("Stipend ₹%s is below required minimum threshold of ₹%s", candidateStipend, minStipend);
        }

        return RuleEvaluationResultDTO.builder()
                .ruleName(rule.getRuleName())
                .passed(passed)
                .failureReason(reason)
                .build();
    }
}
