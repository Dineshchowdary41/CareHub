package com.carehub.carehub.repository;
import com.carehub.carehub.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PatientRepository extends JpaRepository<Patient, Long> {
    @Query("SELECT FUNCTION('DATE_FORMAT', p.createdAt, '%Y-%m') AS ym, COUNT(p) " +
           "FROM Patient p WHERE p.createdAt >= :since GROUP BY ym ORDER BY ym")
    List<Object[]> monthlyGrowthSince(LocalDate since);

    Optional<Patient> findByPhone(String phone);
}
