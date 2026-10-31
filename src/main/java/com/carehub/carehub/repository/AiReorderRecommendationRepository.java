package com.carehub.carehub.repository;

import com.carehub.carehub.entity.AiReorderRecommendation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AiReorderRecommendationRepository extends JpaRepository<AiReorderRecommendation, Long> {
    List<AiReorderRecommendation> findByStatusOrderByCreatedAtDesc(String status);
    List<AiReorderRecommendation> findByPriorityOrderByCreatedAtDesc(String priority);
}
