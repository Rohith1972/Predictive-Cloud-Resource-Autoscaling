package com.project.autoscaling.scaling.dto;

import com.project.autoscaling.scaling.entity.ScalingPolicy;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScalingPolicyResponse {
    private Long id;
    private Long applicationId;
    private String applicationName;
    private Integer minimumInstances;
    private Integer maximumInstances;
    private Integer desiredInstances;
    private Double cpuScaleOutThreshold;
    private Double cpuScaleInThreshold;
    private Double memoryScaleOutThreshold;
    private Double memoryScaleInThreshold;
    private Double requestRateThreshold;
    private Integer cooldownSeconds;
    private Boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ScalingPolicyResponse fromEntity(ScalingPolicy policy) {
        return ScalingPolicyResponse.builder()
                .id(policy.getId())
                .applicationId(policy.getApplication().getId())
                .applicationName(policy.getApplication().getApplicationName())
                .minimumInstances(policy.getMinimumInstances())
                .maximumInstances(policy.getMaximumInstances())
                .desiredInstances(policy.getDesiredInstances())
                .cpuScaleOutThreshold(policy.getCpuScaleOutThreshold())
                .cpuScaleInThreshold(policy.getCpuScaleInThreshold())
                .memoryScaleOutThreshold(policy.getMemoryScaleOutThreshold())
                .memoryScaleInThreshold(policy.getMemoryScaleInThreshold())
                .requestRateThreshold(policy.getRequestRateThreshold())
                .cooldownSeconds(policy.getCooldownSeconds())
                .enabled(policy.getEnabled())
                .createdAt(policy.getCreatedAt())
                .updatedAt(policy.getUpdatedAt())
                .build();
    }
}
