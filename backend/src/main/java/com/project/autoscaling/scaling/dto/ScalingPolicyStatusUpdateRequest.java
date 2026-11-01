package com.project.autoscaling.scaling.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScalingPolicyStatusUpdateRequest {

    @NotNull(message = "Enabled status must be specified (true or false)")
    private Boolean enabled;
}
