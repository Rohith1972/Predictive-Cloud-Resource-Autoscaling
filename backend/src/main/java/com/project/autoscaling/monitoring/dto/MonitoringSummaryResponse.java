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
public class MonitoringSummaryResponse {
    private Long applicationId;
    private String applicationName;
    private int activeInstancesCount;
    private double currentCpuUtilization;
    private double currentMemoryUtilization;
    private double currentRequestRate;
    private double currentResponseTime;
    private double currentErrorRate;
    private boolean isOverloaded;
    private int unhealthyInstancesCount;
    private LocalDateTime timestamp;
}
