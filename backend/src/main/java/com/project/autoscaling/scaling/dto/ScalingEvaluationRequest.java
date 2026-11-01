package com.project.autoscaling.scaling.dto;

import com.project.autoscaling.common.enums.ExecutionMode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScalingEvaluationRequest {

    @Builder.Default
    private ExecutionMode executionMode = ExecutionMode.MANUAL_APPROVAL;
}
