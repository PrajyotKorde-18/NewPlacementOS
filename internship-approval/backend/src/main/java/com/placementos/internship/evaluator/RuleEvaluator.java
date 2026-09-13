package com.placementos.internship.evaluator;

import com.placementos.internship.dto.RuleEvaluationResultDTO;
import com.placementos.internship.entity.InternshipRequest;
import com.placementos.internship.entity.PolicyRule;

public interface RuleEvaluator {
    boolean supports(PolicyRule rule);
    RuleEvaluationResultDTO evaluate(InternshipRequest request, PolicyRule rule);
}
