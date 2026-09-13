package com.placementos.internship.evaluator;

import com.placementos.internship.dto.RuleEvaluationResultDTO;
import com.placementos.internship.entity.InternshipRequest;
import com.placementos.internship.entity.PolicyRule;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class CompanyDenylistRuleEvaluator implements RuleEvaluator {

    @Override
    public boolean supports(PolicyRule rule) {
        return "COMPANY_DENYLIST".equalsIgnoreCase(rule.getRuleName()) || "DENYLIST".equalsIgnoreCase(rule.getRuleType());
    }

    @Override
    public RuleEvaluationResultDTO evaluate(InternshipRequest request, PolicyRule rule) {
        String company = request.getCompanyName() != null ? request.getCompanyName().trim().toLowerCase() : "";
        List<String> deniedCompanies = rule.getRuleValue() != null ?
                Arrays.stream(rule.getRuleValue().split(","))
                        .map(s -> s.trim().toLowerCase())
                        .toList() : List.of();

        boolean passed = !deniedCompanies.contains(company);
        String reason = null;
        if (!passed) {
            reason = String.format("Company '%s' is on the blacklisted company denylist", request.getCompanyName());
        }

        return RuleEvaluationResultDTO.builder()
                .ruleName(rule.getRuleName())
                .passed(passed)
                .failureReason(reason)
                .build();
    }
}
