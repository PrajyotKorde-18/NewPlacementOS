package com.placementos.internship.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "noc_records")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NocRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", nullable = false, unique = true)
    private InternshipRequest request;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by", nullable = false)
    private User approvedBy;

    @Column(name = "approved_at", updatable = false)
    private LocalDateTime approvedAt;

    @Column(name = "pdf_path", nullable = false)
    private String pdfPath;

    @PrePersist
    protected void onCreate() {
        approvedAt = LocalDateTime.now();
    }
}
