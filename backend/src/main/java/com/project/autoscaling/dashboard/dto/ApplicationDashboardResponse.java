package com.project.autoscaling.dashboard.dto;

import com.project.autoscaling.alerts.dto.AlertResponse;
import com.project.autoscaling.application.dto.ApplicationResponse;
import com.project.autoscaling.infrastructure.dto.CapacitySummaryResponse;
import com.project.autoscaling.monitoring.dto.MonitoringSummaryResponse;
import com.project.autoscaling.scaling.dto.ScalingHistorySummaryResponse;
import com.project.autoscaling.scaling.dto.ScalingPolicyResponse;
import com.project.autoscaling.scaling.dto.ScalingRecommendationResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationDashboardResponse {
    private ApplicationResponse application;
    private CapacitySummaryResponse capacity;
    private MonitoringSummaryResponse monitoring;
    private ScalingPolicyResponse policy;
    private List<ScalingRecommendationResponse> recentRecommendations;
    private ScalingHistorySummaryResponse scalingSummary;
    private List<AlertResponse> activeAlerts;
}
