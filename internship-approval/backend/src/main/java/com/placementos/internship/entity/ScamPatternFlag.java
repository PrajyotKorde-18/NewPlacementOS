package com.placementos.internship.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "scam_pattern_flags")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScamPatternFlag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "flag_type", nullable = false)
    private String flagType;

    @Column(name = "match_key")
    private String matchKey;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String detail;

    @Column(name = "detected_at", updatable = false)
    private LocalDateTime detectedAt;

    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @ManyToMany(mappedBy = "scamFlags", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<InternshipRequest> internshipRequests = new HashSet<>();

    @PrePersist
    protected void onCreate() {
        detectedAt = LocalDateTime.now();
    }
}
