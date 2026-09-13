package com.placementos.tpms.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.domain.Persistable;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "student_profiles")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentProfile implements Persistable<Long> {

    @Id
    @Column(name = "student_id")
    private Long studentId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "student_id")
    private Student student;

    @Column(columnDefinition = "LONGTEXT")
    private String skills;

    @Column(columnDefinition = "LONGTEXT")
    private String projects;

    @Column(columnDefinition = "LONGTEXT")
    private String internships;

    @Column(columnDefinition = "LONGTEXT")
    private String certifications; // Legacy JSON String

    @OneToMany(mappedBy = "studentProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<StudentCertification> structuredCertifications = new ArrayList<>();

    @Column(name = "coding_profiles", columnDefinition = "LONGTEXT")
    private String codingProfiles;

    @Column(columnDefinition = "LONGTEXT")
    private String summary;

    @Column(columnDefinition = "LONGTEXT")
    private String achievements;

    @Column(name = "linkedin_url")
    private String linkedinUrl;

    @Column(name = "github_url")
    private String githubUrl;

    @Column(name = "portfolio_url")
    private String portfolioUrl;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Transient
    @Builder.Default
    private boolean isNewEntity = true;

    @Override
    public Long getId() {
        return studentId;
    }

    @Override
    public boolean isNew() {
        return isNewEntity || studentId == null;
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        this.isNewEntity = false;
    }

    public void setStudent(Student student) {
        this.student = student;
        if (student != null) {
            this.studentId = student.getId();
        }
    }

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        if (student != null && studentId == null) {
            this.studentId = student.getId();
        }
        this.updatedAt = LocalDateTime.now();
    }
}
