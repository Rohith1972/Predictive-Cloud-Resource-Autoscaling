package com.project.autoscaling.scaling.engine;

import com.project.autoscaling.common.enums.ScalingAction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScalingEvaluationResult {
    private ScalingAction action;
    private int currentInstanceCount;
    private int recommendedInstanceCount;
    private String triggeringMetric;
    private double observedMetricValue;
    private double configuredThreshold;
    private String decisionReason;
}
