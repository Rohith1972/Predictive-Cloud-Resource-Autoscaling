package com.project.autoscaling.monitoring.dto;

import com.project.autoscaling.monitoring.entity.ResourceMetric;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetricResponse {
    private Long id;
    private Long applicationId;
    private String applicationName;
    private Long instanceId;
    private String instanceIdentifier;
    private LocalDateTime timestamp;
    private Double cpuUtilization;
    private Double memoryUtilization;
    private Double requestRate;
    private Double averageResponseTime;
    private Double errorRate;
    private Integer activeRequests;
    private Long networkInBytes;
    private Long networkOutBytes;

    public static MetricResponse fromEntity(ResourceMetric m) {
        return MetricResponse.builder()
                .id(m.getId())
                .applicationId(m.getApplication().getId())
                .applicationName(m.getApplication().getApplicationName())
                .instanceId(m.getInstance().getId())
                .instanceIdentifier(m.getInstance().getInstanceIdentifier())
                .timestamp(m.getTimestamp())
                .cpuUtilization(m.getCpuUtilization())
                .memoryUtilization(m.getMemoryUtilization())
                .requestRate(m.getRequestRate())
                .averageResponseTime(m.getAverageResponseTime())
                .errorRate(m.getErrorRate())
                .activeRequests(m.getActiveRequests())
                .networkInBytes(m.getNetworkInBytes())
                .networkOutBytes(m.getNetworkOutBytes())
                .build();
    }
}
