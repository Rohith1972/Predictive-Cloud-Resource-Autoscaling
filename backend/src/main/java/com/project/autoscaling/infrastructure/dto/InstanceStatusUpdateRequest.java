package com.project.autoscaling.infrastructure.dto;

import com.project.autoscaling.common.enums.InstanceStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InstanceStatusUpdateRequest {

    @NotNull(message = "Status is required (RUNNING, STOPPED, TERMINATED, UNHEALTHY)")
    private InstanceStatus status;
}
