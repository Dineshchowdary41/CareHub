package com.carehub.carehub.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long logId;

    @Column(length = 50)
    private String username;

    @Column(length = 30)
    private String userRole;

    // CREATE, UPDATE, DELETE, LOGIN, LOGOUT, DISPENSE, OCR_SCAN, AI_PREDICT
    @NotBlank
    @Column(nullable = false, length = 30)
    private String action;

    // Medicine, Inventory, Patient, Doctor, Appointment, Prescription, Bill, PurchaseOrder, Auth, AI, OCR
    @NotBlank
    @Column(nullable = false, length = 50)
    private String module;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 50)
    private String entityId;

    @Column(length = 50)
    private String ipAddress;

    @Column(nullable = false, updatable = false)
    private LocalDateTime timestamp;

    @PrePersist
    protected void onCreate() {
        if (this.timestamp == null) {
            this.timestamp = LocalDateTime.now();
        }
    }
}
