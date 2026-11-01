package com.project.autoscaling.infrastructure.dto;

import com.project.autoscaling.common.enums.ProviderType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateInstanceRequest {

    @NotBlank(message = "Instance identifier is required (e.g., i-0123456789abcdef0 or i-sim-01)")
    @Size(min = 3, max = 100, message = "Identifier must be between 3 and 100 characters")
    private String instanceIdentifier;

    @NotBlank(message = "Instance name is required")
    @Size(min = 2, max = 150, message = "Instance name must be between 2 and 150 characters")
    private String instanceName;

    @NotNull(message = "Application ID is required")
    private Long applicationId;

    @NotBlank(message = "Instance type is required (e.g., t3.medium, c5.xlarge)")
    private String instanceType;

    @NotNull(message = "CPU cores count is required")
    @Min(value = 1, message = "CPU cores must be at least 1")
    private Integer cpuCores;

    @NotNull(message = "Total memory in MB is required")
    @Min(value = 256, message = "Total memory must be at least 256 MB")
    private Long totalMemoryMb;

    @Builder.Default
    private ProviderType provider = ProviderType.LOCAL_SIMULATED;
}
