package com.project.autoscaling.scaling.repository;

import com.project.autoscaling.common.enums.RecommendationStatus;
import com.project.autoscaling.scaling.entity.ScalingRecommendation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ScalingRecommendationRepository extends JpaRepository<ScalingRecommendation, Long> {

    List<ScalingRecommendation> findByApplicationIdOrderByCreatedAtDesc(Long applicationId);

    Page<ScalingRecommendation> findByApplicationId(Long applicationId, Pageable pageable);

    Optional<ScalingRecommendation> findTopByApplicationIdAndStatusOrderByCreatedAtDesc(
            Long applicationId, RecommendationStatus status);
}
