package com.placementos.internship.evaluator;

import com.placementos.internship.dto.RuleEvaluationResultDTO;
import com.placementos.internship.entity.InternshipRequest;
import com.placementos.internship.entity.PolicyRule;

import com.placementos.internship.entity.RuleEvaluation;
import com.placementos.internship.repository.PolicyRuleRepository;
import com.placementos.internship.repository.RuleEvaluationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RuleEvaluatorService {

    private final List<RuleEvaluator> evaluators;
    private final PolicyRuleRepository policyRuleRepository;
    private final RuleEvaluationRepository ruleEvaluationRepository;

    public List<RuleEvaluationResultDTO> evaluateAll(InternshipRequest request) {
        List<PolicyRule> activeRules = policyRuleRepository.findByIsActiveTrue();
        List<RuleEvaluationResultDTO> results = new ArrayList<>();

        for (PolicyRule rule : activeRules) {
            RuleEvaluator evaluator = findEvaluator(rule);
            if (evaluator != null) {
                RuleEvaluationResultDTO result = evaluator.evaluate(request, rule);
                results.add(result);

                // Persist evaluation log for auditability
                RuleEvaluation evalRecord = RuleEvaluation.builder()
                        .request(request)
                        .ruleName(rule.getRuleName())
                        .passed(result.isPassed())
                        .failureReason(result.getFailureReason())
                        .build();
                ruleEvaluationRepository.save(evalRecord);
            }
        }
        return results;
    }

    private RuleEvaluator findEvaluator(PolicyRule rule) {
        for (RuleEvaluator evaluator : evaluators) {
            if (evaluator.supports(rule)) {
                return evaluator;
            }
        }
        return null;
    }
}
