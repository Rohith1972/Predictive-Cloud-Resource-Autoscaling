package com.project.autoscaling.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardOverviewResponse {
    private long totalApplications;
    private long activeApplications;
    private long totalInstances;
    private long runningInstances;
    private double averageCpuUtilization;
    private double averageMemoryUtilization;
    private double currentRequestRate;
    private double averageResponseTime;
    private long activeAlerts;
    private long totalScaleOutOperations;
    private long totalScaleInOperations;
    private LocalDateTime generatedAt;
}
