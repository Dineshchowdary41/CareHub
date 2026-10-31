package com.carehub.carehub.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ReorderRecommendationDTO {
    private Long medicineId;
    private String medicineName;
    private String category;
    private Long supplierId;
    private String supplierName;
    private String supplierEmail;
    private Integer currentStock;
    private Integer minimumStock;
    private Integer predictedDemand;
    private Integer recommendedQuantity;
    private String priority; // HIGH, MEDIUM, LOW
    private String reason;
    private Integer leadTimeDays;
    private String reorderStatus; // REORDER REQUIRED, REORDER RECOMMENDED, STOCK OPTIMAL
    private Integer daysUntilStockout;
}
