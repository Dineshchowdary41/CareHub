package com.carehub.carehub.repository;
import com.carehub.carehub.entity.Prescription;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PrescriptionRepository extends JpaRepository<Prescription, Long> {
    List<Prescription> findByPatient_PatientId(Long patientId);
    List<Prescription> findByDoctor_DoctorId(Long doctorId);
}
