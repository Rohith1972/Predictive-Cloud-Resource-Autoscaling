package com.project.autoscaling.workload.dto;

import com.project.autoscaling.workload.entity.WorkloadMetric;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkloadResponse {
    private Long id;
    private Long applicationId;
    private String applicationName;
    private LocalDateTime timestamp;
    private Long requestCount;
    private Double requestsPerSecond;
    private Long successfulRequests;
    private Long failedRequests;
    private Double averageLatency;
    private Double p95Latency;
    private Double p99Latency;

    public static WorkloadResponse fromEntity(WorkloadMetric w) {
        return WorkloadResponse.builder()
                .id(w.getId())
                .applicationId(w.getApplication().getId())
                .applicationName(w.getApplication().getApplicationName())
                .timestamp(w.getTimestamp())
                .requestCount(w.getRequestCount())
                .requestsPerSecond(w.getRequestsPerSecond())
                .successfulRequests(w.getSuccessfulRequests())
                .failedRequests(w.getFailedRequests())
                .averageLatency(w.getAverageLatency())
                .p95Latency(w.getP95Latency())
                .p99Latency(w.getP99Latency())
                .build();
    }
}
