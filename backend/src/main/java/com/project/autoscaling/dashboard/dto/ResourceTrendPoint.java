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
public class ResourceTrendPoint {
    private LocalDateTime timestamp;
    private double cpuUtilization;
    private double memoryUtilization;
    private double requestRate;
}
