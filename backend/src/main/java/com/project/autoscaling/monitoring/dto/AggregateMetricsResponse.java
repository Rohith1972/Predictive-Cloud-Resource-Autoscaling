package com.project.autoscaling.monitoring.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AggregateMetricsResponse {
    private Long applicationId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private double averageCpuUtilization;
    private double minCpuUtilization;
    private double maxCpuUtilization;
    private double averageMemoryUtilization;
    private double minMemoryUtilization;
    private double maxMemoryUtilization;
    private double averageRequestRate;
    private double averageResponseTime;
    private double averageErrorRate;
    private long totalDataPoints;
}
