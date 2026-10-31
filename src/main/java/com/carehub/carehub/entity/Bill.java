package com.carehub.carehub.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "bills")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Bill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long billId;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @NotNull
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    // Paid, Pending, Partial
    @NotNull
    @Column(nullable = false, length = 20)
    private String paymentStatus;

    @Column(nullable = false)
    private LocalDateTime billDate;

    @PrePersist
    protected void onCreate() {
        if (this.billDate == null) {
            this.billDate = LocalDateTime.now();
        }
    }
}
