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
public class WorkloadTrendResponse {
    private Long applicationId;
    private String applicationName;
    private String trendDirection; // INCREASING, DECREASING, STABLE, SPIKE
    private double changePercentage;
    private double currentRequestsPerSecond;
    private double previousRequestsPerSecond;
    private double currentAverageLatency;
    private String description;
    private LocalDateTime evaluatedAt;
}
