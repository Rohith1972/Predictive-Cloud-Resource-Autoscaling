package com.project.autoscaling.scaling.dto;

import com.project.autoscaling.common.enums.ExecutionMode;
import com.project.autoscaling.common.enums.ScalingAction;
import com.project.autoscaling.common.enums.ScalingEventStatus;
import com.project.autoscaling.scaling.entity.ScalingEvent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScalingEventResponse {
    private Long id;
    private Long applicationId;
    private String applicationName;
    private Long recommendationId;
    private Integer previousInstanceCount;
    private Integer newInstanceCount;
    private ScalingAction scalingAction;
    private String triggeringMetric;
    private Double triggeringMetricValue;
    private String decisionReason;
    private ExecutionMode executionMode;
    private ScalingEventStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;

    public static ScalingEventResponse fromEntity(ScalingEvent e) {
        return ScalingEventResponse.builder()
                .id(e.getId())
                .applicationId(e.getApplication().getId())
                .applicationName(e.getApplication().getApplicationName())
                .recommendationId(e.getRecommendation() != null ? e.getRecommendation().getId() : null)
                .previousInstanceCount(e.getPreviousInstanceCount())
                .newInstanceCount(e.getNewInstanceCount())
                .scalingAction(e.getScalingAction())
                .triggeringMetric(e.getTriggeringMetric())
                .triggeringMetricValue(e.getTriggeringMetricValue())
                .decisionReason(e.getDecisionReason())
                .executionMode(e.getExecutionMode())
                .status(e.getStatus())
                .createdAt(e.getCreatedAt())
                .completedAt(e.getCompletedAt())
                .build();
    }
}
