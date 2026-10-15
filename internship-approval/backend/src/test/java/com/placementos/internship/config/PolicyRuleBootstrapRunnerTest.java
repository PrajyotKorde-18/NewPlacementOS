package com.placementos.internship.config;

import com.placementos.internship.entity.PolicyRule;
import com.placementos.internship.repository.PolicyRuleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PolicyRuleBootstrapRunnerTest {

    @Mock
    private PolicyRuleRepository policyRuleRepository;

    @InjectMocks
    private PolicyRuleBootstrapRunner runner;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(runner, "defaultMinStipend", "10000");
        ReflectionTestUtils.setField(runner, "defaultMinDurationWeeks", "6");
    }

    @Test
    void shouldSeedDefaultRulesWhenDatabaseIsEmpty() {
        when(policyRuleRepository.findByRuleName("MINIMUM_STIPEND")).thenReturn(Optional.empty());
        when(policyRuleRepository.findByRuleName("MINIMUM_DURATION")).thenReturn(Optional.empty());

        runner.run();

        ArgumentCaptor<PolicyRule> captor = ArgumentCaptor.forClass(PolicyRule.class);
        verify(policyRuleRepository, times(2)).save(captor.capture());

        var savedRules = captor.getAllValues();

        var stipendRule = savedRules.stream()
                .filter(r -> "MINIMUM_STIPEND".equals(r.getRuleName()))
                .findFirst()
                .orElse(null);
        assertThat(stipendRule).isNotNull();
        assertThat(stipendRule.getRuleValue()).isEqualTo("10000");
        assertThat(stipendRule.getRuleType()).isEqualTo("STIPEND");
        assertThat(stipendRule.getIsActive()).isTrue();

        var durationRule = savedRules.stream()
                .filter(r -> "MINIMUM_DURATION".equals(r.getRuleName()))
                .findFirst()
                .orElse(null);
        assertThat(durationRule).isNotNull();
        assertThat(durationRule.getRuleValue()).isEqualTo("6");
        assertThat(durationRule.getRuleType()).isEqualTo("DURATION");
        assertThat(durationRule.getIsActive()).isTrue();
    }

    @Test
    void shouldNotOverwriteExistingRules() {
        PolicyRule existingStipend = PolicyRule.builder()
                .ruleName("MINIMUM_STIPEND")
                .ruleValue("15000")
                .isActive(true)
                .build();
        PolicyRule existingDuration = PolicyRule.builder()
                .ruleName("MINIMUM_DURATION")
                .ruleValue("8")
                .isActive(true)
                .build();

        when(policyRuleRepository.findByRuleName("MINIMUM_STIPEND")).thenReturn(Optional.of(existingStipend));
        when(policyRuleRepository.findByRuleName("MINIMUM_DURATION")).thenReturn(Optional.of(existingDuration));

        runner.run();

        verify(policyRuleRepository, never()).save(any(PolicyRule.class));
    }
}
