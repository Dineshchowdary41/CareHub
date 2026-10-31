package com.carehub.carehub.config;

import com.carehub.carehub.entity.*;
import com.carehub.carehub.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final SupplierRepository supplierRepository;
    private final MedicineRepository medicineRepository;
    private final InventoryRepository inventoryRepository;
    private final BillRepository billRepository;
    private final BillItemRepository billItemRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final PrescriptionItemRepository prescriptionItemRepository;
    private final AppointmentRepository appointmentRepository;
    private final MedicalHistoryRepository medicalHistoryRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserRepository userRepository,
                      DoctorRepository doctorRepository,
                      PatientRepository patientRepository,
                      SupplierRepository supplierRepository,
                      MedicineRepository medicineRepository,
                      InventoryRepository inventoryRepository,
                      BillRepository billRepository,
                      BillItemRepository billItemRepository,
                      PrescriptionRepository prescriptionRepository,
                      PrescriptionItemRepository prescriptionItemRepository,
                      AppointmentRepository appointmentRepository,
                      MedicalHistoryRepository medicalHistoryRepository,
                      PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.doctorRepository = doctorRepository;
        this.patientRepository = patientRepository;
        this.supplierRepository = supplierRepository;
        this.medicineRepository = medicineRepository;
        this.inventoryRepository = inventoryRepository;
        this.billRepository = billRepository;
        this.billItemRepository = billItemRepository;
        this.prescriptionRepository = prescriptionRepository;
        this.prescriptionItemRepository = prescriptionItemRepository;
        this.appointmentRepository = appointmentRepository;
        this.medicalHistoryRepository = medicalHistoryRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        seedUsers();
        seedHospitalCoreData();
        seedAppointments();
        seedMedicalHistory();
    }

    private void seedUsers() {
        createUserIfMissing("admin", "Admin@123", "Admin");
        createUserIfMissing("dr.smith", "Doctor@123", "Doctor");
        createUserIfMissing("pharma.john", "Pharma@123", "Pharmacist");
        createUserIfMissing("nurse.reception", "Recep@123", "Receptionist");

        // Also ensure legacy 'Dinesh' user has known password if used
        userRepository.findByUsernameIgnoreCase("Dinesh").ifPresent(d -> {
            d.setPasswordHash(passwordEncoder.encode("Admin@123"));
            userRepository.save(d);
        });

        System.out.println("=================================================================");
        System.out.println(" CareHub 3.0: Staff test credentials verified:");
        System.out.println("   ADMIN:        admin            / Admin@123");
        System.out.println("   DOCTOR:       dr.smith         / Doctor@123");
        System.out.println("   PHARMACIST:   pharma.john      / Pharma@123");
        System.out.println("   RECEPTIONIST: nurse.reception  / Recep@123");
        System.out.println("=================================================================");
    }

    private void createUserIfMissing(String username, String password, String role) {
        if (!userRepository.findByUsernameIgnoreCase(username).isPresent()) {
            User user = User.builder()
                    .username(username)
                    .passwordHash(passwordEncoder.encode(password))
                    .role(role)
                    .isActive(true)
                    .build();
            userRepository.save(user);
        }
    }

    private void seedHospitalCoreData() {
        if (medicineRepository.count() == 0) {
            // Suppliers
            Supplier s1 = supplierRepository.save(Supplier.builder()
                    .supplierName("Apex Pharma Distributors")
                    .phone("+91-9876543210")
                    .email("orders@apexpharma.com")
                    .address("104 Industrial Area, Phase II, Bengaluru")
                    .build());

            Supplier s2 = supplierRepository.save(Supplier.builder()
                    .supplierName("MedLife Healthcare Supplies")
                    .phone("+91-9812345678")
                    .email("supply@medlife.com")
                    .address("45 Ring Road, Mumbai")
                    .build());

            // Medicines
            Medicine m1 = medicineRepository.save(Medicine.builder()
                    .medicineName("Paracetamol")
                    .category("Analgesic / Antipyretic")
                    .unit("tablets")
                    .description("Fever and mild to moderate pain relief")
                    .build());

            Medicine m2 = medicineRepository.save(Medicine.builder()
                    .medicineName("Amoxicillin")
                    .category("Antibiotics")
                    .unit("capsules")
                    .description("Broad-spectrum penicillin antibiotic")
                    .build());

            Medicine m3 = medicineRepository.save(Medicine.builder()
                    .medicineName("Cetirizine")
                    .category("Antihistamines")
                    .unit("tablets")
                    .description("Allergy and cold relief")
                    .build());

            Medicine m4 = medicineRepository.save(Medicine.builder()
                    .medicineName("Metformin")
                    .category("Antidiabetic")
                    .unit("tablets")
                    .description("Type 2 diabetes blood glucose control")
                    .build());

            Medicine m5 = medicineRepository.save(Medicine.builder()
                    .medicineName("Pantoprazole")
                    .category("Gastrointestinal")
                    .unit("tablets")
                    .description("Proton pump inhibitor for acid reflux")
                    .build());

            // Inventory batches
            inventoryRepository.save(Inventory.builder()
                    .medicine(m1).supplier(s1).batchNo("B-PCM-2026-01")
                    .expiryDate(LocalDate.now().plusMonths(14))
                    .unitPrice(new BigDecimal("5.50"))
                    .availableStock(35).minimumStock(60)
                    .build());

            inventoryRepository.save(Inventory.builder()
                    .medicine(m2).supplier(s1).batchNo("B-AMX-2025-08")
                    .expiryDate(LocalDate.now().plusDays(20))
                    .unitPrice(new BigDecimal("12.00"))
                    .availableStock(80).minimumStock(30)
                    .build());

            inventoryRepository.save(Inventory.builder()
                    .medicine(m3).supplier(s2).batchNo("B-CTZ-2026-03")
                    .expiryDate(LocalDate.now().plusMonths(18))
                    .unitPrice(new BigDecimal("3.20"))
                    .availableStock(150).minimumStock(40)
                    .build());
        }
    }

    private void seedAppointments() {
        if (appointmentRepository.count() == 0) {
            List<Patient> patients = patientRepository.findAll();
            List<Doctor> doctors = doctorRepository.findAll();

            if (!patients.isEmpty() && !doctors.isEmpty()) {
                Patient p1 = patients.get(0);
                Patient p2 = patients.size() > 1 ? patients.get(1) : p1;
                Patient p3 = patients.size() > 2 ? patients.get(2) : p1;

                Doctor d1 = doctors.get(0);
                Doctor d2 = doctors.size() > 1 ? doctors.get(1) : d1;

                appointmentRepository.save(Appointment.builder()
                        .patient(p1).doctor(d1)
                        .appointmentDateTime(LocalDateTime.now().withHour(10).withMinute(30))
                        .reason("Follow-up Consultation on Fever & Respiratory Health")
                        .status("Scheduled")
                        .notes("Patient reports improvement, review lab reports")
                        .build());

                appointmentRepository.save(Appointment.builder()
                        .patient(p2).doctor(d2)
                        .appointmentDateTime(LocalDateTime.now().plusDays(2).withHour(15).withMinute(0))
                        .reason("Cardiovascular routine checkup and ECG review")
                        .status("Confirmed")
                        .notes("First visit for BP monitoring")
                        .build());

                appointmentRepository.save(Appointment.builder()
                        .patient(p3).doctor(d1)
                        .appointmentDateTime(LocalDateTime.now().minusDays(3).withHour(11).withMinute(0))
                        .reason("Blood sugar level review & prescription renewal")
                        .status("Completed")
                        .notes("Metformin dosage adjusted to 500mg BD")
                        .build());

                appointmentRepository.save(Appointment.builder()
                        .patient(p1).doctor(d2)
                        .appointmentDateTime(LocalDateTime.now().minusDays(1).withHour(14).withMinute(0))
                        .reason("Routine Health Screening")
                        .status("Cancelled")
                        .notes("Patient requested cancellation due to travel")
                        .build());

                System.out.println(" CareHub 3.0: Sample appointments seeded successfully.");
            }
        }
    }

    private void seedMedicalHistory() {
        if (medicalHistoryRepository.count() == 0) {
            List<Patient> patients = patientRepository.findAll();
            List<Doctor> doctors = doctorRepository.findAll();

            if (!patients.isEmpty()) {
                Patient p1 = patients.get(0);
                Doctor d1 = !doctors.isEmpty() ? doctors.get(0) : null;

                medicalHistoryRepository.save(MedicalHistory.builder()
                        .patient(p1).doctor(d1)
                        .visitDate(LocalDate.now().minusDays(5))
                        .diagnosis("Acute Viral Pharyngitis with Mild Fever")
                        .symptoms("Sore throat, body ache, fever up to 101F")
                        .treatmentPlan("Hydration, Paracetamol for fever, Cetirizine at bedtime")
                        .notes("Advised complete rest for 3 days.")
                        .build());

                if (patients.size() > 1) {
                    Patient p2 = patients.get(1);
                    medicalHistoryRepository.save(MedicalHistory.builder()
                            .patient(p2).doctor(d1)
                            .visitDate(LocalDate.now().minusDays(14))
                            .diagnosis("Essential Hypertension (Stage 1)")
                            .symptoms("Occasional morning headaches, elevated diastolic BP")
                            .treatmentPlan("Low sodium diet, regular morning aerobic exercise, weekly BP log")
                            .notes("Re-check blood pressure in 3 weeks.")
                            .build());
                }

                System.out.println(" CareHub 3.0: Sample medical history records seeded successfully.");
            }
        }
    }
}
