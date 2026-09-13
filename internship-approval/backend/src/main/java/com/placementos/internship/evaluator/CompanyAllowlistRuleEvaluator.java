package com.placementos.internship.evaluator;

import com.placementos.internship.dto.RuleEvaluationResultDTO;
import com.placementos.internship.entity.InternshipRequest;
import com.placementos.internship.entity.PolicyRule;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class CompanyAllowlistRuleEvaluator implements RuleEvaluator {

    @Override
    public boolean supports(PolicyRule rule) {
        return "COMPANY_ALLOWLIST".equalsIgnoreCase(rule.getRuleName()) || "ALLOWLIST".equalsIgnoreCase(rule.getRuleType());
    }

    @Override
    public RuleEvaluationResultDTO evaluate(InternshipRequest request, PolicyRule rule) {
        String company = request.getCompanyName() != null ? request.getCompanyName().trim().toLowerCase() : "";
        List<String> allowedCompanies = rule.getRuleValue() != null ?
                Arrays.stream(rule.getRuleValue().split(","))
                        .map(s -> s.trim().toLowerCase())
                        .toList() : List.of();

        boolean passed = allowedCompanies.contains(company);
        String reason = null;
        if (!passed) {
            reason = String.format("Company '%s' is not in the approved company allowlist", request.getCompanyName());
        }

        return RuleEvaluationResultDTO.builder()
                .ruleName(rule.getRuleName())
                .passed(passed)
                .failureReason(reason)
                .build();
    }
}
