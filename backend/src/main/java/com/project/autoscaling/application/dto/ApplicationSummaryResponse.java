package com.project.autoscaling.application.dto;

import com.project.autoscaling.common.enums.ApplicationEnvironment;
import com.project.autoscaling.common.enums.ApplicationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationSummaryResponse {
    private Long applicationId;
    private String applicationName;
    private ApplicationEnvironment environment;
    private ApplicationStatus status;
    private int totalInstances;
    private int runningInstances;
    private double currentCpuUtilization;
    private double currentMemoryUtilization;
    private double currentRequestRate;
    private long activeAlertsCount;
    private LocalDateTime lastUpdated;
}
