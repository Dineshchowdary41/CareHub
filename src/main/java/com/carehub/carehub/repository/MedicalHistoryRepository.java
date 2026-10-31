package com.carehub.carehub.repository;

import com.carehub.carehub.entity.MedicalHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MedicalHistoryRepository extends JpaRepository<MedicalHistory, Long> {
    List<MedicalHistory> findByPatient_PatientIdOrderByVisitDateDesc(Long patientId);
    List<MedicalHistory> findByDoctor_DoctorIdOrderByVisitDateDesc(Long doctorId);
}
