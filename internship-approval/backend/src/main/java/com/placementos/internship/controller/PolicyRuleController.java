package com.placementos.internship.controller;

import com.placementos.internship.dto.PolicyRuleDTO;
import com.placementos.internship.service.PolicyRuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/policy-rules")
@RequiredArgsConstructor
public class PolicyRuleController {

    private final PolicyRuleService policyRuleService;

    @GetMapping
    public ResponseEntity<List<PolicyRuleDTO>> getAllRules() {
        return ResponseEntity.ok(policyRuleService.getAllRules());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PolicyRuleDTO> getRuleById(@PathVariable Long id) {
        return ResponseEntity.ok(policyRuleService.getRuleById(id));
    }

    @PostMapping
    public ResponseEntity<PolicyRuleDTO> createRule(@RequestBody PolicyRuleDTO dto) {
        PolicyRuleDTO created = policyRuleService.createRule(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PolicyRuleDTO> updateRule(@PathVariable Long id, @RequestBody PolicyRuleDTO dto) {
        PolicyRuleDTO updated = policyRuleService.updateRule(id, dto);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRule(@PathVariable Long id) {
        policyRuleService.deleteRule(id);
        return ResponseEntity.noContent().build();
    }
}
