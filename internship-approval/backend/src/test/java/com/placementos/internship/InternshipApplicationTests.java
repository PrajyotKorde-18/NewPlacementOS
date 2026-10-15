package com.placementos.internship;

import com.placementos.internship.repository.PolicyRuleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class InternshipApplicationTests {

    @Autowired
    private PolicyRuleRepository policyRuleRepository;

    @Test
    void contextLoads() {
        assertThat(policyRuleRepository.findByRuleName("MINIMUM_STIPEND")).isPresent();
        assertThat(policyRuleRepository.findByRuleName("MINIMUM_DURATION")).isPresent();
    }
}
