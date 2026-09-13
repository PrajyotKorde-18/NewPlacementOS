package com.placementos.internship.repository;

import com.placementos.internship.entity.PolicyRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PolicyRuleRepository extends JpaRepository<PolicyRule, Long> {
    Optional<PolicyRule> findByRuleName(String ruleName);
    List<PolicyRule> findByIsActiveTrue();
}
