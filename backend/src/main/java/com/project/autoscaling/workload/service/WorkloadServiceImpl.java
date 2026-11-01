package com.project.autoscaling.workload.service;

import com.project.autoscaling.application.entity.Application;
import com.project.autoscaling.application.repository.ApplicationRepository;
import com.project.autoscaling.common.exception.ResourceNotFoundException;
import com.project.autoscaling.common.response.PagedResponse;
import com.project.autoscaling.workload.dto.WorkloadRecordRequest;
import com.project.autoscaling.workload.dto.WorkloadResponse;
import com.project.autoscaling.workload.dto.WorkloadSummaryResponse;
import com.project.autoscaling.workload.dto.WorkloadTrendResponse;
import com.project.autoscaling.workload.entity.WorkloadMetric;
import com.project.autoscaling.workload.repository.WorkloadMetricRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WorkloadServiceImpl implements WorkloadService {

    private final WorkloadMetricRepository workloadRepository;
    private final ApplicationRepository applicationRepository;

    @Override
    @Transactional
    public WorkloadResponse recordWorkload(WorkloadRecordRequest request) {
        Application application = applicationRepository.findById(request.getApplicationId())
                .orElseThrow(() -> new ResourceNotFoundException("Application", "id", request.getApplicationId()));

        LocalDateTime ts = request.getTimestamp() != null ? request.getTimestamp() : LocalDateTime.now();
        double p95 = request.getP95Latency() != null ? request.getP95Latency() : request.getAverageLatency() * 1.5;
        double p99 = request.getP99Latency() != null ? request.getP99Latency() : request.getAverageLatency() * 2.0;

        WorkloadMetric metric = WorkloadMetric.builder()
                .application(application)
                .timestamp(ts)
                .requestCount(request.getRequestCount())
                .requestsPerSecond(request.getRequestsPerSecond())
                .successfulRequests(request.getSuccessfulRequests())
                .failedRequests(request.getFailedRequests())
                .averageLatency(request.getAverageLatency())
                .p95Latency(p95)
                .p99Latency(p99)
                .build();

        WorkloadMetric saved = workloadRepository.save(metric);
        return WorkloadResponse.fromEntity(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public WorkloadTrendResponse getWorkloadTrend(Long applicationId) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application", "id", applicationId));

        List<WorkloadMetric> recent = workloadRepository.findTop10ByApplicationIdOrderByTimestampDesc(applicationId);

        if (recent.isEmpty()) {
            return WorkloadTrendResponse.builder()
                    .applicationId(applicationId)
                    .applicationName(application.getApplicationName())
                    .trendDirection("NO_DATA")
                    .changePercentage(0.0)
                    .currentRequestsPerSecond(0.0)
                    .previousRequestsPerSecond(0.0)
                    .currentAverageLatency(0.0)
                    .description("No workload observations available")
                    .evaluatedAt(LocalDateTime.now())
                    .build();
        }

        WorkloadMetric current = recent.get(0);
        double currentRps = current.getRequestsPerSecond();
        double prevRps = recent.size() > 1 ? recent.get(1).getRequestsPerSecond() : currentRps;

        double change = 0.0;
        if (prevRps > 0) {
            change = ((currentRps - prevRps) / prevRps) * 100.0;
        }

        String direction;
        String desc;
        if (change >= 50.0) {
            direction = "TRAFFIC_SPIKE";
            desc = String.format("Sudden surge detected: +%.1f%% RPS increase. Potential scale-out trigger.", change);
        } else if (change > 15.0) {
            direction = "INCREASING";
            desc = String.format("Workload trending up: +%.1f%% RPS increase.", change);
        } else if (change < -20.0) {
            direction = "DECREASING";
            desc = String.format("Workload trending down: %.1f%% RPS decrease.", change);
        } else {
            direction = "STABLE";
            desc = "Workload traffic is stable within normal variation tolerances.";
        }

        return WorkloadTrendResponse.builder()
                .applicationId(applicationId)
                .applicationName(application.getApplicationName())
                .trendDirection(direction)
                .changePercentage(Math.round(change * 100.0) / 100.0)
                .currentRequestsPerSecond(currentRps)
                .previousRequestsPerSecond(prevRps)
                .currentAverageLatency(current.getAverageLatency())
                .description(desc)
                .evaluatedAt(LocalDateTime.now())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public WorkloadSummaryResponse getWorkloadSummary(Long applicationId, LocalDateTime start, LocalDateTime end) {
        if (!applicationRepository.existsById(applicationId)) {
            throw new ResourceNotFoundException("Application", "id", applicationId);
        }

        LocalDateTime searchStart = start != null ? start : LocalDateTime.now().minusHours(24);
        LocalDateTime searchEnd = end != null ? end : LocalDateTime.now();

        List<Object[]> rows = workloadRepository.calculateWorkloadSummary(applicationId, searchStart, searchEnd);

        if (rows.isEmpty() || rows.get(0)[7] == null || ((Number) rows.get(0)[7]).longValue() == 0L) {
            return WorkloadSummaryResponse.builder()
                    .applicationId(applicationId)
                    .startTime(searchStart)
                    .endTime(searchEnd)
                    .totalObservations(0)
                    .build();
        }

        Object[] r = rows.get(0);
        long totalReqs = ((Number) r[0]).longValue();
        double avgRps = ((Number) r[1]).doubleValue();
        long success = ((Number) r[2]).longValue();
        long failed = ((Number) r[3]).longValue();
        double avgLat = ((Number) r[4]).doubleValue();
        double p95 = ((Number) r[5]).doubleValue();
        double p99 = ((Number) r[6]).doubleValue();
        long count = ((Number) r[7]).longValue();

        double successRate = totalReqs > 0 ? ((double) success / totalReqs) * 100.0 : 100.0;

        return WorkloadSummaryResponse.builder()
                .applicationId(applicationId)
                .startTime(searchStart)
                .endTime(searchEnd)
                .totalRequests(totalReqs)
                .averageRequestsPerSecond(avgRps)
                .totalSuccessfulRequests(success)
                .totalFailedRequests(failed)
                .successRatePercentage(Math.round(successRate * 100.0) / 100.0)
                .averageLatencyMs(avgLat)
                .averageP95LatencyMs(p95)
                .averageP99LatencyMs(p99)
                .totalObservations(count)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<WorkloadResponse> getWorkloadHistory(Long applicationId, LocalDateTime start, LocalDateTime end, Pageable pageable) {
        if (!applicationRepository.existsById(applicationId)) {
            throw new ResourceNotFoundException("Application", "id", applicationId);
        }

        Page<WorkloadMetric> page;
        if (start != null && end != null) {
            page = workloadRepository.findByApplicationIdAndTimestampBetween(applicationId, start, end, pageable);
        } else {
            page = workloadRepository.findByApplicationId(applicationId, pageable);
        }

        return PagedResponse.of(page.map(WorkloadResponse::fromEntity));
    }
}
