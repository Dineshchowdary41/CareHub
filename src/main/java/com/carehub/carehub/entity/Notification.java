package com.carehub.carehub.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long notificationId;

    @NotBlank
    @Column(nullable = false, length = 150)
    private String title;

    @NotBlank
    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    // LOW_STOCK, EXPIRY, APPOINTMENT, PRESCRIPTION, AI_RECOMMENDATION, BILLING, SYSTEM
    @NotBlank
    @Column(nullable = false, length = 30)
    private String type;

    // INFO, WARNING, DANGER, SUCCESS
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String severity = "INFO";

    // Admin, Doctor, Pharmacist, Receptionist, or ALL
    @Column(length = 30)
    private String targetRole;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isRead = false;

    @Column(length = 255)
    private String linkUrl;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.isRead == null) {
            this.isRead = false;
        }
        if (this.severity == null) {
            this.severity = "INFO";
        }
    }
}
