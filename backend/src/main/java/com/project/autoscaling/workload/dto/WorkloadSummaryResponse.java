package com.project.autoscaling.workload.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkloadSummaryResponse {
    private Long applicationId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private long totalRequests;
    private double averageRequestsPerSecond;
    private long totalSuccessfulRequests;
    private long totalFailedRequests;
    private double successRatePercentage;
    private double averageLatencyMs;
    private double averageP95LatencyMs;
    private double averageP99LatencyMs;
    private long totalObservations;
}
