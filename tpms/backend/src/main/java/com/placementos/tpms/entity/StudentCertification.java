package com.placementos.tpms.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "student_certifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentCertification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_profile_id", nullable = false)
    private StudentProfile studentProfile;

    @Column(nullable = false)
    private String name;

    @Column(name = "issuing_body", nullable = false)
    private String issuingBody;

    @Column(name = "issue_date")
    private LocalDate issueDate;

    @Column(name = "credential_url")
    private String credentialUrl;
}
