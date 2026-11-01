package com.project.autoscaling.infrastructure.provider;

import com.project.autoscaling.common.enums.InstanceStatus;
import com.project.autoscaling.common.enums.ProviderType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InstanceCapacityDetails {
    private String instanceIdentifier;
    private String instanceName;
    private String instanceType;
    private int cpuCores;
    private long totalMemoryMb;
    private InstanceStatus status;
    private ProviderType provider;
}
