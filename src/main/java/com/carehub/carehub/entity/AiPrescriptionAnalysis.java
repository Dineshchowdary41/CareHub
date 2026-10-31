package com.carehub.carehub.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ai_prescription_analysis")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AiPrescriptionAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long analysisId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prescription_id")
    private Prescription prescription;

    @Column(nullable = false)
    private String fileName;

    @Column(length = 50)
    private String imageQuality;

    @Column
    private Double sharpnessScore;

    @Column(length = 100)
    private String cnnClassification;

    @Column
    private Double cnnConfidence;

    @Column(columnDefinition = "TEXT")
    private String ocrText;

    @Column
    private Integer nlpEntityCount;

    @Column(length = 100)
    private String modelName;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}
