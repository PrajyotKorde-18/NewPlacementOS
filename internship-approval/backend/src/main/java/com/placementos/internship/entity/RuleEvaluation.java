package com.placementos.internship.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "rule_evaluations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RuleEvaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", nullable = false)
    private InternshipRequest request;

    @Column(name = "rule_name", nullable = false)
    private String ruleName;

    @Column(nullable = false)
    private Boolean passed;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Column(name = "evaluated_at", updatable = false)
    private LocalDateTime evaluatedAt;

    @PrePersist
    protected void onCreate() {
        evaluatedAt = LocalDateTime.now();
    }
}
