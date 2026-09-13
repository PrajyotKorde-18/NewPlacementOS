package com.placementos.tpms.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.domain.Persistable;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "academic_profiles")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AcademicProfile implements Persistable<Long> {

    @Id
    @Column(name = "student_id")
    private Long studentId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "student_id")
    private Student student;

    @Column(name = "tenth_percentage")
    private Double tenthPercentage;

    @Column(name = "tenth_cgpa")
    private Double tenthCgpa;

    @Column(name = "tenth_board")
    private String tenthBoard;

    @Column(name = "tenth_year_of_passing")
    private Integer tenthYearOfPassing;

    @Column(name = "twelfth_percentage")
    private Double twelfthPercentage;

    @Column(name = "twelfth_cgpa")
    private Double twelfthCgpa;

    @Column(name = "twelfth_board")
    private String twelfthBoard;

    @Column(name = "twelfth_stream")
    private String twelfthStream;

    @Column(name = "twelfth_year_of_passing")
    private Integer twelfthYearOfPassing;

    @Column(name = "branch")
    private String branch;

    @Column(name = "admission_year")
    private Integer admissionYear;

    @Column(name = "expected_graduation_year")
    private Integer expectedGraduationYear;

    @OneToMany(mappedBy = "academicProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<SemesterSgpa> semesterSgpas = new ArrayList<>();

    @OneToMany(mappedBy = "academicProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<BacklogRecord> backlogRecords = new ArrayList<>();

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

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
    protected void onCreate() {
        if (student != null && studentId == null) {
            this.studentId = student.getId();
        }
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        if (student != null && studentId == null) {
            this.studentId = student.getId();
        }
        this.updatedAt = LocalDateTime.now();
    }

    public Double getCurrentCgpa() {
        if (semesterSgpas == null || semesterSgpas.isEmpty()) return null;
        List<SemesterSgpa> finalized = semesterSgpas.stream()
                .filter(SemesterSgpa::getIsFinalized)
                .toList();
        if (finalized.isEmpty()) return null;
        double sum = finalized.stream().mapToDouble(SemesterSgpa::getSgpa).sum();
        return Math.round((sum / finalized.size()) * 100.0) / 100.0;
    }

    public Double getCgpaTillSemester(int semesterNumber) {
        if (semesterSgpas == null || semesterSgpas.isEmpty()) return null;
        List<SemesterSgpa> eligible = semesterSgpas.stream()
                .filter(SemesterSgpa::getIsFinalized)
                .filter(s -> s.getSemesterNumber() <= semesterNumber)
                .toList();
        if (eligible.isEmpty()) return null;
        double sum = eligible.stream().mapToDouble(SemesterSgpa::getSgpa).sum();
        return Math.round((sum / eligible.size()) * 100.0) / 100.0;
    }

    public long getActiveBacklogCount() {
        if (backlogRecords == null) return 0;
        return backlogRecords.stream().filter(b -> !Boolean.TRUE.equals(b.getCleared())).count();
    }

    public long getTotalEverBacklogCount() {
        if (backlogRecords == null) return 0;
        return backlogRecords.size();
    }
}
