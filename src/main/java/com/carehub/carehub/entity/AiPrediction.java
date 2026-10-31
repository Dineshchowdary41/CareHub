package com.carehub.carehub.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ai_predictions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AiPrediction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long predictionId;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "medicine_id", nullable = false)
    private Medicine medicine;

    @Column(nullable = false)
    private Integer currentStock;

    @Column(nullable = false)
    private Integer historicalUsageMonthly;

    @Column(nullable = false)
    private Double averageDailyUsage;

    @Column(nullable = false)
    private Integer predictedNextMonthDemand;

    @Column
    private Integer predicted7DayDemand;

    @Column(length = 60)
    private String modelName;

    // Surging, Stable, Declining
    @Column(length = 30)
    private String trendDirection;

    // CRITICAL, HIGH, MEDIUM, LOW
    @Column(nullable = false, length = 20)
    private String riskLevel;

    @Column(nullable = false)
    private Integer recommendedStockLevel;

    @Column
    private Integer daysUntilStockout;

    @Column(nullable = false)
    private LocalDateTime predictedAt;

    @PrePersist
    protected void onCreate() {
        if (this.predictedAt == null) {
            this.predictedAt = LocalDateTime.now();
        }
    }
}
