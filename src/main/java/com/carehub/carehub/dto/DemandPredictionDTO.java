package com.carehub.carehub.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DemandPredictionDTO {
    private Long medicineId;
    private String medicineName;
    private String category;
    private String unit;
    private Integer currentStock;
    private Integer historicalUsage30d;
    private Integer historicalUsage60d;
    private Double averageDailyUsage;
    private Double averageMonthlyUsage;
    private Integer predictedNextMonthDemand;
    private Integer predicted7DayDemand;
    private String modelName;
    private String trend; // Surging, Stable, Declining
    private String trendLabel;
    private String riskLevel; // CRITICAL, HIGH, MEDIUM, LOW
    private Integer recommendedStockLevel;
    private Integer daysUntilStockout;
    private String predictionSummary;
}
