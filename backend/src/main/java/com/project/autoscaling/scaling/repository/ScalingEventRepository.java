package com.project.autoscaling.scaling.repository;

import com.project.autoscaling.common.enums.ScalingAction;
import com.project.autoscaling.scaling.entity.ScalingEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface ScalingEventRepository extends JpaRepository<ScalingEvent, Long> {

    Page<ScalingEvent> findByApplicationId(Long applicationId, Pageable pageable);

    Page<ScalingEvent> findByApplicationIdAndCreatedAtBetween(
            Long applicationId, LocalDateTime start, LocalDateTime end, Pageable pageable);

    Optional<ScalingEvent> findTopByApplicationIdOrderByCreatedAtDesc(Long applicationId);

    long countByApplicationIdAndScalingAction(Long applicationId, ScalingAction action);

    long countByScalingAction(ScalingAction action);
}
