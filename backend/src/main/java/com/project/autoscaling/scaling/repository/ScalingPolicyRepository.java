package com.project.autoscaling.scaling.repository;

import com.project.autoscaling.scaling.entity.ScalingPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ScalingPolicyRepository extends JpaRepository<ScalingPolicy, Long> {
    Optional<ScalingPolicy> findByApplicationId(Long applicationId);
    boolean existsByApplicationId(Long applicationId);
}
