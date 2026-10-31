package com.carehub.carehub.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OcrPrescriptionResultDTO {
    private String fileName;
    private String rawText;
    private String imageQuality;
    private Double sharpnessScore;
    private String cnnClassification;
    private Double cnnConfidence;
    private String cnnModelName;
    private String nlpModelName;
    private String qualityDescription;
    @Builder.Default
    private List<DetectedMedicineItem> detectedMedicines = new ArrayList<>();
    private LocalDateTime scannedAt;
    private Boolean success;
    private String message;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class DetectedMedicineItem {
        private Long medicineId;
        private String medicineName;
        private String dosage;     // e.g. "500 mg", "10 ml"
        private String frequency;  // e.g. "2 times/day", "1-0-1", "TID"
        private String duration;   // e.g. "5 days", "1 week"
        private Integer quantity;  // estimated required tablets/bottles
        private Double confidence; // e.g. 0.95
        private Boolean matchedInCatalog;
    }
}
