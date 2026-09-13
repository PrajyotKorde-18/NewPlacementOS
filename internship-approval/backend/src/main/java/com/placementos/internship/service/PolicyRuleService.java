package com.placementos.internship.service;

import com.placementos.internship.dto.PolicyRuleDTO;
import com.placementos.internship.entity.PolicyRule;
import com.placementos.internship.entity.Role;
import com.placementos.internship.entity.User;
import com.placementos.internship.exception.AppException;
import com.placementos.internship.exception.ResourceNotFoundException;
import com.placementos.internship.repository.PolicyRuleRepository;
import com.placementos.internship.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PolicyRuleService {

    private final PolicyRuleRepository policyRuleRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<PolicyRuleDTO> getAllRules() {
        return policyRuleRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PolicyRuleDTO getRuleById(Long id) {
        PolicyRule rule = policyRuleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Policy rule not found with id: " + id));
        return mapToDTO(rule);
    }

    @Transactional
    public PolicyRuleDTO createRule(PolicyRuleDTO dto) {
        validateCdpcAdminAuthority();

        if (dto.getRuleName() == null || dto.getRuleName().isBlank()) {
            throw new AppException("Rule name is required", HttpStatus.BAD_REQUEST);
        }

        if (policyRuleRepository.findByRuleName(dto.getRuleName()).isPresent()) {
            throw new AppException("Policy rule with name '" + dto.getRuleName() + "' already exists", HttpStatus.CONFLICT);
        }

        PolicyRule rule = PolicyRule.builder()
                .ruleName(dto.getRuleName().trim().toUpperCase())
                .label(dto.getLabel())
                .ruleType(dto.getRuleType())
                .ruleValue(dto.getRuleValue())
                .isActive(dto.getIsActive() != null ? dto.getIsActive() : true)
                .description(dto.getDescription())
                .build();

        rule = policyRuleRepository.save(rule);
        return mapToDTO(rule);
    }

    @Transactional
    public PolicyRuleDTO updateRule(Long id, PolicyRuleDTO dto) {
        validateCdpcAdminAuthority();

        PolicyRule rule = policyRuleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Policy rule not found with id: " + id));

        // CRITICAL: rule_name is an immutable system-managed key. Ignore attempts to change ruleName!
        if (dto.getLabel() != null) rule.setLabel(dto.getLabel());
        if (dto.getRuleType() != null) rule.setRuleType(dto.getRuleType());
        if (dto.getRuleValue() != null) rule.setRuleValue(dto.getRuleValue());
        if (dto.getIsActive() != null) rule.setIsActive(dto.getIsActive());
        if (dto.getDescription() != null) rule.setDescription(dto.getDescription());

        rule = policyRuleRepository.save(rule);
        return mapToDTO(rule);
    }

    @Transactional
    public void deleteRule(Long id) {
        validateCdpcAdminAuthority();

        PolicyRule rule = policyRuleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Policy rule not found with id: " + id));
        policyRuleRepository.delete(rule);
    }

    private User validateCdpcAdminAuthority() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new AppException("Authentication required to manage policy rules", HttpStatus.UNAUTHORIZED);
        }

        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found with email: " + email));

        if (user.getRole() != Role.CDPC_ADMIN) {
            throw new AppException("Access denied: Policy criteria rules can only be modified by CDPC_ADMIN", HttpStatus.FORBIDDEN);
        }
        return user;
    }

    private PolicyRuleDTO mapToDTO(PolicyRule rule) {
        return PolicyRuleDTO.builder()
                .id(rule.getId())
                .ruleName(rule.getRuleName())
                .label(rule.getLabel())
                .ruleType(rule.getRuleType())
                .ruleValue(rule.getRuleValue())
                .isActive(rule.getIsActive())
                .description(rule.getDescription())
                .build();
    }
}
