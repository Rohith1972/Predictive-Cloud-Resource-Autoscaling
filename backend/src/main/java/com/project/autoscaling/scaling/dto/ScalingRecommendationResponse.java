package com.project.autoscaling.scaling.dto;

import com.project.autoscaling.common.enums.ExecutionMode;
import com.project.autoscaling.common.enums.RecommendationStatus;
import com.project.autoscaling.common.enums.ScalingAction;
import com.project.autoscaling.scaling.entity.ScalingRecommendation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScalingRecommendationResponse {
    private Long id;
    private Long applicationId;
    private String applicationName;
    private Integer currentInstanceCount;
    private Integer recommendedInstanceCount;
    private ScalingAction scalingAction;
    private String triggeringMetric;
    private Double observedMetricValue;
    private Double configuredThreshold;
    private String decisionReason;
    private ExecutionMode executionMode;
    private RecommendationStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime evaluatedAt;

    public static ScalingRecommendationResponse fromEntity(ScalingRecommendation r) {
        return ScalingRecommendationResponse.builder()
                .id(r.getId())
                .applicationId(r.getApplication().getId())
                .applicationName(r.getApplication().getApplicationName())
                .currentInstanceCount(r.getCurrentInstanceCount())
                .recommendedInstanceCount(r.getRecommendedInstanceCount())
                .scalingAction(r.getScalingAction())
                .triggeringMetric(r.getTriggeringMetric())
                .observedMetricValue(r.getObservedMetricValue())
                .configuredThreshold(r.getConfiguredThreshold())
                .decisionReason(r.getDecisionReason())
                .executionMode(r.getExecutionMode())
                .status(r.getStatus())
                .createdAt(r.getCreatedAt())
                .evaluatedAt(r.getEvaluatedAt())
                .build();
    }
}
