package com.placementos.internship.config;

import com.placementos.internship.entity.PolicyRule;
import com.placementos.internship.repository.PolicyRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(10)
@RequiredArgsConstructor
@Slf4j
public class PolicyRuleBootstrapRunner implements CommandLineRunner {

    private final PolicyRuleRepository policyRuleRepository;

    @Value("${app.policy.default-min-stipend:10000}")
    private String defaultMinStipend;

    @Value("${app.policy.default-min-duration-weeks:6}")
    private String defaultMinDurationWeeks;

    @Override
    public void run(String... args) {
        seedDefaultRulesIfAbsent();
    }

    public void seedDefaultRulesIfAbsent() {
        if (policyRuleRepository.findByRuleName("MINIMUM_STIPEND").isEmpty()) {
            PolicyRule stipendRule = PolicyRule.builder()
                    .ruleName("MINIMUM_STIPEND")
                    .label("Minimum Monthly Stipend")
                    .ruleType("STIPEND")
                    .ruleValue(defaultMinStipend)
                    .isActive(true)
                    .description("Institutional minimum required monthly stipend for approval")
                    .build();
            policyRuleRepository.save(stipendRule);
            log.info("[BOOTSTRAP] Seeded default policy rule 'MINIMUM_STIPEND' with threshold: {}", defaultMinStipend);
        } else {
            log.info("[BOOTSTRAP] Policy rule 'MINIMUM_STIPEND' already exists. Skipping default seed.");
        }

        if (policyRuleRepository.findByRuleName("MINIMUM_DURATION").isEmpty()) {
            PolicyRule durationRule = PolicyRule.builder()
                    .ruleName("MINIMUM_DURATION")
                    .label("Minimum Internship Duration")
                    .ruleType("DURATION")
                    .ruleValue(defaultMinDurationWeeks)
                    .isActive(true)
                    .description("Institutional minimum required internship duration in weeks")
                    .build();
            policyRuleRepository.save(durationRule);
            log.info("[BOOTSTRAP] Seeded default policy rule 'MINIMUM_DURATION' with threshold: {} weeks", defaultMinDurationWeeks);
        } else {
            log.info("[BOOTSTRAP] Policy rule 'MINIMUM_DURATION' already exists. Skipping default seed.");
        }
    }
}
