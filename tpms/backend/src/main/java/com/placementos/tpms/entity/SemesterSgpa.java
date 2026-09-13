package com.placementos.tpms.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "semester_sgpas")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SemesterSgpa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "academic_profile_id", nullable = false)
    private AcademicProfile academicProfile;

    @Column(name = "semester_number", nullable = false)
    private Integer semesterNumber;

    @Column(name = "sgpa", nullable = false)
    private Double sgpa;

    @Column(name = "is_finalized", nullable = false)
    @Builder.Default
    private Boolean isFinalized = true;
}
