package com.carehub.carehub.dto;

import com.carehub.carehub.entity.Appointment;
import com.carehub.carehub.entity.Bill;
import com.carehub.carehub.entity.MedicalHistory;
import com.carehub.carehub.entity.Patient;
import lombok.*;
import java.util.ArrayList;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PatientMedicalHistoryDTO {
    private Patient patient;
    private Integer age;
    @Builder.Default
    private List<MedicalHistory> visits = new ArrayList<>();
    @Builder.Default
    private List<PrescriptionDetail> prescriptions = new ArrayList<>();
    @Builder.Default
    private List<Appointment> appointments = new ArrayList<>();
    @Builder.Default
    private List<Bill> bills = new ArrayList<>();

    private Integer totalVisits;
    private Integer totalPrescriptions;
    private Integer totalAppointments;
    private java.math.BigDecimal totalBilled;
    private java.math.BigDecimal pendingBilled;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class PrescriptionDetail {
        private Long prescriptionId;
        private Long doctorId;
        private String doctorName;
        private String doctorSpecialization;
        private java.time.LocalDate prescriptionDate;
        private String notes;
        @Builder.Default
        private List<PrescriptionItemDetail> items = new ArrayList<>();
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class PrescriptionItemDetail {
        private Long itemId;
        private Long medicineId;
        private String medicineName;
        private String category;
        private String dosage;
        private String frequency;
        private String duration;
        private Integer quantity;
    }
}
