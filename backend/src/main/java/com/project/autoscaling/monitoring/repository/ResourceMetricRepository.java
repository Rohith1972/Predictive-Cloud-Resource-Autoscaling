package com.project.autoscaling.monitoring.repository;

import com.project.autoscaling.monitoring.entity.ResourceMetric;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ResourceMetricRepository extends JpaRepository<ResourceMetric, Long> {

    Page<ResourceMetric> findByApplicationId(Long applicationId, Pageable pageable);

    Page<ResourceMetric> findByApplicationIdAndTimestampBetween(
            Long applicationId, LocalDateTime start, LocalDateTime end, Pageable pageable);

    Page<ResourceMetric> findByInstanceId(Long instanceId, Pageable pageable);

    Optional<ResourceMetric> findTopByApplicationIdOrderByTimestampDesc(Long applicationId);

    List<ResourceMetric> findTop20ByApplicationIdOrderByTimestampDesc(Long applicationId);

    @Query("SELECT " +
            "COALESCE(AVG(m.cpuUtilization), 0.0), " +
            "COALESCE(MIN(m.cpuUtilization), 0.0), " +
            "COALESCE(MAX(m.cpuUtilization), 0.0), " +
            "COALESCE(AVG(m.memoryUtilization), 0.0), " +
            "COALESCE(MIN(m.memoryUtilization), 0.0), " +
            "COALESCE(MAX(m.memoryUtilization), 0.0), " +
            "COALESCE(AVG(m.requestRate), 0.0), " +
            "COALESCE(AVG(m.averageResponseTime), 0.0), " +
            "COALESCE(AVG(m.errorRate), 0.0), " +
            "COUNT(m) " +
            "FROM ResourceMetric m " +
            "WHERE m.application.id = :applicationId AND m.timestamp BETWEEN :start AND :end")
    List<Object[]> calculateAggregates(
            @Param("applicationId") Long applicationId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);
}
