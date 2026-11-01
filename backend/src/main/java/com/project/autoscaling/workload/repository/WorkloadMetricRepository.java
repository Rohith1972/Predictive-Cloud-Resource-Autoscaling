package com.project.autoscaling.workload.repository;

import com.project.autoscaling.workload.entity.WorkloadMetric;
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
public interface WorkloadMetricRepository extends JpaRepository<WorkloadMetric, Long> {

    Page<WorkloadMetric> findByApplicationId(Long applicationId, Pageable pageable);

    Page<WorkloadMetric> findByApplicationIdAndTimestampBetween(
            Long applicationId, LocalDateTime start, LocalDateTime end, Pageable pageable);

    Optional<WorkloadMetric> findTopByApplicationIdOrderByTimestampDesc(Long applicationId);

    List<WorkloadMetric> findTop10ByApplicationIdOrderByTimestampDesc(Long applicationId);

    @Query("SELECT " +
            "COALESCE(SUM(w.requestCount), 0), " +
            "COALESCE(AVG(w.requestsPerSecond), 0.0), " +
            "COALESCE(SUM(w.successfulRequests), 0), " +
            "COALESCE(SUM(w.failedRequests), 0), " +
            "COALESCE(AVG(w.averageLatency), 0.0), " +
            "COALESCE(AVG(w.p95Latency), 0.0), " +
            "COALESCE(AVG(w.p99Latency), 0.0), " +
            "COUNT(w) " +
            "FROM WorkloadMetric w " +
            "WHERE w.application.id = :applicationId AND w.timestamp BETWEEN :start AND :end")
    List<Object[]> calculateWorkloadSummary(
            @Param("applicationId") Long applicationId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);
}
