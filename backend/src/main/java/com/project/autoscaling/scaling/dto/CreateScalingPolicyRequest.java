package com.project.autoscaling.scaling.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateScalingPolicyRequest {

    @NotNull(message = "Application ID is required")
    private Long applicationId;

    @NotNull(message = "Minimum instances count is required")
    @Min(value = 1, message = "Minimum instances must be at least 1")
    private Integer minimumInstances;

    @NotNull(message = "Maximum instances count is required")
    @Min(value = 1, message = "Maximum instances must be at least 1")
    private Integer maximumInstances;

    @NotNull(message = "Desired instances count is required")
    @Min(value = 1, message = "Desired instances must be at least 1")
    private Integer desiredInstances;

    @NotNull(message = "CPU scale-out threshold is required")
    @DecimalMin(value = "0.0", message = "CPU scale-out threshold must be between 0.0 and 100.0%")
    @DecimalMax(value = "100.0", message = "CPU scale-out threshold must be between 0.0 and 100.0%")
    private Double cpuScaleOutThreshold;

    @NotNull(message = "CPU scale-in threshold is required")
    @DecimalMin(value = "0.0", message = "CPU scale-in threshold must be between 0.0 and 100.0%")
    @DecimalMax(value = "100.0", message = "CPU scale-in threshold must be between 0.0 and 100.0%")
    private Double cpuScaleInThreshold;

    @NotNull(message = "Memory scale-out threshold is required")
    @DecimalMin(value = "0.0", message = "Memory scale-out threshold must be between 0.0 and 100.0%")
    @DecimalMax(value = "100.0", message = "Memory scale-out threshold must be between 0.0 and 100.0%")
    private Double memoryScaleOutThreshold;

    @NotNull(message = "Memory scale-in threshold is required")
    @DecimalMin(value = "0.0", message = "Memory scale-in threshold must be between 0.0 and 100.0%")
    @DecimalMax(value = "100.0", message = "Memory scale-in threshold must be between 0.0 and 100.0%")
    private Double memoryScaleInThreshold;

    @NotNull(message = "Request rate threshold is required")
    @PositiveOrZero(message = "Request rate threshold must be non-negative")
    private Double requestRateThreshold;

    @NotNull(message = "Cooldown period in seconds is required")
    @Min(value = 0, message = "Cooldown period must be non-negative")
    private Integer cooldownSeconds;

    @Builder.Default
    private Boolean enabled = true;
}
