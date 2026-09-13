package com.placementos.tpms.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "backlog_records")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BacklogRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "academic_profile_id", nullable = false)
    private AcademicProfile academicProfile;

    @Column(name = "subject_name", nullable = false)
    private String subjectName;

    @Column(name = "semester", nullable = false)
    private Integer semester;

    @Column(name = "cleared", nullable = false)
    @Builder.Default
    private Boolean cleared = false;

    @Column(name = "cleared_on")
    private LocalDate clearedOn;
}
