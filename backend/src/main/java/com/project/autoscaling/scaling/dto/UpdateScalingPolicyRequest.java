package com.project.autoscaling.scaling.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateScalingPolicyRequest {

    @Min(value = 1, message = "Minimum instances must be at least 1")
    private Integer minimumInstances;

    @Min(value = 1, message = "Maximum instances must be at least 1")
    private Integer maximumInstances;

    @Min(value = 1, message = "Desired instances must be at least 1")
    private Integer desiredInstances;

    @DecimalMin(value = "0.0", message = "CPU scale-out threshold must be between 0.0 and 100.0%")
    @DecimalMax(value = "100.0", message = "CPU scale-out threshold must be between 0.0 and 100.0%")
    private Double cpuScaleOutThreshold;

    @DecimalMin(value = "0.0", message = "CPU scale-in threshold must be between 0.0 and 100.0%")
    @DecimalMax(value = "100.0", message = "CPU scale-in threshold must be between 0.0 and 100.0%")
    private Double cpuScaleInThreshold;

    @DecimalMin(value = "0.0", message = "Memory scale-out threshold must be between 0.0 and 100.0%")
    @DecimalMax(value = "100.0", message = "Memory scale-out threshold must be between 0.0 and 100.0%")
    private Double memoryScaleOutThreshold;

    @DecimalMin(value = "0.0", message = "Memory scale-in threshold must be between 0.0 and 100.0%")
    @DecimalMax(value = "100.0", message = "Memory scale-in threshold must be between 0.0 and 100.0%")
    private Double memoryScaleInThreshold;

    @PositiveOrZero(message = "Request rate threshold must be non-negative")
    private Double requestRateThreshold;

    @Min(value = 0, message = "Cooldown period must be non-negative")
    private Integer cooldownSeconds;

    private Boolean enabled;
}
