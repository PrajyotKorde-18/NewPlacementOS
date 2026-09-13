package com.placementos.tpms.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "role_profiles")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "drive_id", nullable = false, unique = true)
    private Drive drive;

    @Column(name = "required_skills", nullable = false, columnDefinition = "LONGTEXT")
    private String requiredSkills; // JSON: skill -> weight / bucket (MUST_HAVE, NICE_TO_HAVE, BONUS)

    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String weights; // JSON: category weights (skill match, project relevance, coding activity, certs, experience)

    @Column(name = "min_cgpa")
    private Double minCgpa;

    @Column(name = "min_tenth_percentage")
    private Double minTenthPercentage;

    @Column(name = "min_twelfth_percentage")
    private Double minTwelfthPercentage;

    @Column(name = "max_active_backlogs")
    private Integer maxActiveBacklogs;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
