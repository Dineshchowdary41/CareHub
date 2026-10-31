package com.carehub.carehub.repository;

import com.carehub.carehub.entity.AiPrediction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface AiPredictionRepository extends JpaRepository<AiPrediction, Long> {
    List<AiPrediction> findByOrderByPredictedAtDesc();
    Optional<AiPrediction> findTopByMedicine_MedicineIdOrderByPredictedAtDesc(Long medicineId);
}
