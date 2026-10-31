package com.carehub.carehub.service;

import com.carehub.carehub.config.ResourceNotFoundException;
import com.carehub.carehub.dto.PatientMedicalHistoryDTO;
import com.carehub.carehub.entity.*;
import com.carehub.carehub.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;

@Service
public class MedicalHistoryService {

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private MedicalHistoryRepository medicalHistoryRepository;

    @Autowired
    private PrescriptionRepository prescriptionRepository;

    @Autowired
    private PrescriptionItemRepository prescriptionItemRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private BillRepository billRepository;

    @Autowired
    private AuditLogService auditLogService;

    public PatientMedicalHistoryDTO getPatientFullHistory(Long patientId) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with id " + patientId));

        int age = 0;
        if (patient.getDateOfBirth() != null) {
            age = Period.between(patient.getDateOfBirth(), LocalDate.now()).getYears();
        }

        // Visits and clinical notes
        List<MedicalHistory> visits = medicalHistoryRepository.findByPatient_PatientIdOrderByVisitDateDesc(patientId);

        // Prescriptions with details
        List<Prescription> prescriptions = prescriptionRepository.findByPatient_PatientId(patientId);
        List<PatientMedicalHistoryDTO.PrescriptionDetail> rxDetails = new ArrayList<>();

        for (Prescription p : prescriptions) {
            List<PrescriptionItem> items = prescriptionItemRepository.findByPrescription_PrescriptionId(p.getPrescriptionId());
            List<PatientMedicalHistoryDTO.PrescriptionItemDetail> itemDetails = items.stream().map(it ->
                    PatientMedicalHistoryDTO.PrescriptionItemDetail.builder()
                            .itemId(it.getItemId())
                            .medicineId(it.getMedicine() != null ? it.getMedicine().getMedicineId() : null)
                            .medicineName(it.getMedicine() != null ? it.getMedicine().getMedicineName() : "Unknown")
                            .category(it.getMedicine() != null ? it.getMedicine().getCategory() : "General")
                            .dosage(it.getDosage())
                            .frequency(it.getFrequency())
                            .duration(it.getDuration())
                            .quantity(it.getQuantity())
                            .build()
            ).toList();

            rxDetails.add(PatientMedicalHistoryDTO.PrescriptionDetail.builder()
                    .prescriptionId(p.getPrescriptionId())
                    .doctorId(p.getDoctor() != null ? p.getDoctor().getDoctorId() : null)
                    .doctorName(p.getDoctor() != null ? p.getDoctor().getName() : "Attending Doctor")
                    .doctorSpecialization(p.getDoctor() != null ? p.getDoctor().getSpecialization() : "")
                    .prescriptionDate(p.getPrescriptionDate())
                    .notes(p.getNotes())
                    .items(itemDetails)
                    .build());
        }

        // Appointments
        List<Appointment> appointments = appointmentRepository.findByPatient_PatientIdOrderByAppointmentDateTimeDesc(patientId);

        // Bills
        List<Bill> bills = billRepository.findByPatient_PatientId(patientId);
        BigDecimal totalBilled = BigDecimal.ZERO;
        BigDecimal pendingBilled = BigDecimal.ZERO;
        for (Bill b : bills) {
            if (b.getTotalAmount() != null) {
                totalBilled = totalBilled.add(b.getTotalAmount());
                if (!"Paid".equalsIgnoreCase(b.getPaymentStatus())) {
                    pendingBilled = pendingBilled.add(b.getTotalAmount());
                }
            }
        }

        return PatientMedicalHistoryDTO.builder()
                .patient(patient)
                .age(age)
                .visits(visits)
                .prescriptions(rxDetails)
                .appointments(appointments)
                .bills(bills)
                .totalVisits(visits.size())
                .totalPrescriptions(rxDetails.size())
                .totalAppointments(appointments.size())
                .totalBilled(totalBilled)
                .pendingBilled(pendingBilled)
                .build();
    }

    public MedicalHistory addRecord(Long patientId, MedicalHistory record) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with id " + patientId));
        record.setPatient(patient);

        if (record.getDoctor() != null && record.getDoctor().getDoctorId() != null) {
            Doctor doc = doctorRepository.findById(record.getDoctor().getDoctorId())
                    .orElse(null);
            record.setDoctor(doc);
        }

        if (record.getVisitDate() == null) {
            record.setVisitDate(LocalDate.now());
        }

        MedicalHistory saved = medicalHistoryRepository.save(record);
        auditLogService.log("CREATE", "MedicalHistory",
                "New medical diagnosis recorded for patient " + patient.getName() + ": " + record.getDiagnosis(),
                String.valueOf(saved.getHistoryId()));
        return saved;
    }

    public void deleteRecord(Long historyId) {
        if (!medicalHistoryRepository.existsById(historyId)) {
            throw new ResourceNotFoundException("Medical record not found with id " + historyId);
        }
        medicalHistoryRepository.deleteById(historyId);
        auditLogService.log("DELETE", "MedicalHistory", "Medical history record deleted with ID " + historyId, String.valueOf(historyId));
    }
}
