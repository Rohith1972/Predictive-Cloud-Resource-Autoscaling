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
public class WorkloadTrendPoint {
    private LocalDateTime timestamp;
    private double requestsPerSecond;
    private double averageLatency;
    private long failedRequests;
}
