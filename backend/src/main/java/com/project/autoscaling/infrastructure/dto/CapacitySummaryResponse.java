package com.project.autoscaling.infrastructure.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CapacitySummaryResponse {
    private Long applicationId;
    private String applicationName;
    private int totalInstances;
    private int runningInstances;
    private int stoppedInstances;
    private int terminatedInstances;
    private int unhealthyInstances;
    private int totalActiveCpuCores;
    private long totalActiveMemoryMb;
}
