package com.project.autoscaling.scaling.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScalingHistorySummaryResponse {
    private Long applicationId;
    private String applicationName;
    private long totalEvents;
    private long scaleOutEvents;
    private long scaleInEvents;
    private long noActionEvaluations;
    private LocalDateTime lastScaledAt;
}
