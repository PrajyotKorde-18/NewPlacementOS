package com.placementos.internship.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "policy_rules")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PolicyRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rule_name", nullable = false, unique = true)
    private String ruleName;

    @Column(nullable = false)
    private String label;

    @Column(name = "rule_type", nullable = false)
    private String ruleType;

    @Column(name = "rule_value", nullable = false)
    private String ruleValue;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (isActive == null) {
            isActive = true;
        }
    }
}
