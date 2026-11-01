package com.project.autoscaling.infrastructure.dto;

import com.project.autoscaling.common.enums.InstanceStatus;
import com.project.autoscaling.common.enums.ProviderType;
import com.project.autoscaling.infrastructure.entity.Instance;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InstanceResponse {
    private Long id;
    private String instanceIdentifier;
    private String instanceName;
    private Long applicationId;
    private String applicationName;
    private String instanceType;
    private Integer cpuCores;
    private Long totalMemoryMb;
    private InstanceStatus status;
    private ProviderType provider;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static InstanceResponse fromEntity(Instance instance) {
        return InstanceResponse.builder()
                .id(instance.getId())
                .instanceIdentifier(instance.getInstanceIdentifier())
                .instanceName(instance.getInstanceName())
                .applicationId(instance.getApplication().getId())
                .applicationName(instance.getApplication().getApplicationName())
                .instanceType(instance.getInstanceType())
                .cpuCores(instance.getCpuCores())
                .totalMemoryMb(instance.getTotalMemoryMb())
                .status(instance.getStatus())
                .provider(instance.getProvider())
                .createdAt(instance.getCreatedAt())
                .updatedAt(instance.getUpdatedAt())
                .build();
    }
}
