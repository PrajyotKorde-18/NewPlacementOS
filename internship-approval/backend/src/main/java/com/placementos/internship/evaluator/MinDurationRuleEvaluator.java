package com.placementos.internship.evaluator;

import com.placementos.internship.dto.RuleEvaluationResultDTO;
import com.placementos.internship.entity.InternshipRequest;
import com.placementos.internship.entity.PolicyRule;
import org.springframework.stereotype.Component;

@Component
public class MinDurationRuleEvaluator implements RuleEvaluator {

    @Override
    public boolean supports(PolicyRule rule) {
        return "MINIMUM_DURATION".equalsIgnoreCase(rule.getRuleName()) || "DURATION".equalsIgnoreCase(rule.getRuleType());
    }

    @Override
    public RuleEvaluationResultDTO evaluate(InternshipRequest request, PolicyRule rule) {
        int minWeeks = 4;
        try {
            minWeeks = Integer.parseInt(rule.getRuleValue());
        } catch (Exception ignored) {
        }

        int candidateWeeks = request.getDurationWeeks() != null ? request.getDurationWeeks() : 0;
        boolean passed = candidateWeeks >= minWeeks;

        String reason = null;
        if (!passed) {
            reason = String.format("Duration of %d weeks is below required minimum of %d weeks", candidateWeeks, minWeeks);
        }

        return RuleEvaluationResultDTO.builder()
                .ruleName(rule.getRuleName())
                .passed(passed)
                .failureReason(reason)
                .build();
    }
}
