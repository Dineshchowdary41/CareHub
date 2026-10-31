package com.carehub.carehub.repository;

import com.carehub.carehub.entity.AiPrescriptionAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AiPrescriptionAnalysisRepository extends JpaRepository<AiPrescriptionAnalysis, Long> {
    List<AiPrescriptionAnalysis> findTop10ByOrderByCreatedAtDesc();
}
