package com.project.autoscaling.monitoring.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetricIngestionRequest {

    @NotNull(message = "Application ID is required")
    private Long applicationId;

    @NotNull(message = "Instance ID is required")
    private Long instanceId;

    private LocalDateTime timestamp;

    @NotNull(message = "CPU utilization is required")
    @DecimalMin(value = "0.0", message = "CPU utilization must be between 0.0 and 100.0%")
    @DecimalMax(value = "100.0", message = "CPU utilization must be between 0.0 and 100.0%")
    private Double cpuUtilization;

    @NotNull(message = "Memory utilization is required")
    @DecimalMin(value = "0.0", message = "Memory utilization must be between 0.0 and 100.0%")
    @DecimalMax(value = "100.0", message = "Memory utilization must be between 0.0 and 100.0%")
    private Double memoryUtilization;

    @NotNull(message = "Request rate is required")
    @PositiveOrZero(message = "Request rate must be non-negative")
    private Double requestRate;

    @NotNull(message = "Average response time is required")
    @PositiveOrZero(message = "Average response time must be non-negative")
    private Double averageResponseTime;

    @NotNull(message = "Error rate is required")
    @DecimalMin(value = "0.0", message = "Error rate must be between 0.0 and 100.0%")
    @DecimalMax(value = "100.0", message = "Error rate must be between 0.0 and 100.0%")
    private Double errorRate;

    @PositiveOrZero(message = "Active requests must be non-negative")
    @Builder.Default
    private Integer activeRequests = 0;

    @PositiveOrZero(message = "Network in bytes must be non-negative")
    @Builder.Default
    private Long networkInBytes = 0L;

    @PositiveOrZero(message = "Network out bytes must be non-negative")
    @Builder.Default
    private Long networkOutBytes = 0L;
}
