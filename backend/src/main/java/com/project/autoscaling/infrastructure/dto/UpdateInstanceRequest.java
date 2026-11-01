package com.project.autoscaling.infrastructure.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateInstanceRequest {

    @Size(min = 2, max = 150, message = "Instance name must be between 2 and 150 characters")
    private String instanceName;

    private String instanceType;

    @Min(value = 1, message = "CPU cores must be at least 1")
    private Integer cpuCores;

    @Min(value = 256, message = "Total memory must be at least 256 MB")
    private Long totalMemoryMb;
}
