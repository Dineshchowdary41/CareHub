package com.carehub.carehub.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ai_reorder_recommendations")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AiReorderRecommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long recommendationId;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "medicine_id", nullable = false)
    private Medicine medicine;

    @ManyToOne
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    @Column(nullable = false)
    private Integer currentStock;

    @Column(nullable = false)
    private Integer minimumStock;

    @Column(nullable = false)
    private Integer predictedDemand;

    @Column(nullable = false)
    private Integer recommendedQuantity;

    // HIGH, MEDIUM, LOW
    @Column(nullable = false, length = 20)
    private String priority;

    @Column(columnDefinition = "TEXT")
    private String reason;

    // OPEN, ORDERED, DISMISSED
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "OPEN";

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.status == null) {
            this.status = "OPEN";
        }
    }
}
