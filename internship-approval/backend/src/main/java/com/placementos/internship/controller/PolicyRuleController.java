package com.placementos.internship.controller;

import com.placementos.internship.dto.PolicyRuleDTO;
import com.placementos.internship.service.PolicyRuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/policy-rules")
@RequiredArgsConstructor
public class PolicyRuleController {

    private final PolicyRuleService policyRuleService;

    @GetMapping
    @PreAuthorize("hasAnyRole('CDPC_STAFF', 'CDPC_ADMIN')")
    public ResponseEntity<List<PolicyRuleDTO>> getAllRules() {
        return ResponseEntity.ok(policyRuleService.getAllRules());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CDPC_STAFF', 'CDPC_ADMIN')")
    public ResponseEntity<PolicyRuleDTO> getRuleById(@PathVariable Long id) {
        return ResponseEntity.ok(policyRuleService.getRuleById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('CDPC_ADMIN')")
    public ResponseEntity<PolicyRuleDTO> createRule(@Valid @RequestBody PolicyRuleDTO dto) {
        PolicyRuleDTO created = policyRuleService.createRule(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('CDPC_ADMIN')")
    public ResponseEntity<PolicyRuleDTO> updateRule(@PathVariable Long id, @Valid @RequestBody PolicyRuleDTO dto) {
        PolicyRuleDTO updated = policyRuleService.updateRule(id, dto);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('CDPC_ADMIN')")
    public ResponseEntity<Void> deleteRule(@PathVariable Long id) {
        policyRuleService.deleteRule(id);
        return ResponseEntity.noContent().build();
    }
}
