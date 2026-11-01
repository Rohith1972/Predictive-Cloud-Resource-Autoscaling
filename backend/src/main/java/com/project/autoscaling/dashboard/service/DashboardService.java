package com.project.autoscaling.dashboard.service;

import com.project.autoscaling.dashboard.dto.ApplicationDashboardResponse;
import com.project.autoscaling.dashboard.dto.DashboardOverviewResponse;
import com.project.autoscaling.dashboard.dto.ResourceTrendPoint;
import com.project.autoscaling.dashboard.dto.WorkloadTrendPoint;

import java.time.LocalDateTime;
import java.util.List;

public interface DashboardService {
    DashboardOverviewResponse getOverview();
    ApplicationDashboardResponse getApplicationDashboard(Long applicationId);
    List<ResourceTrendPoint> getResourceTrends(Long applicationId, LocalDateTime start, LocalDateTime end);
    List<WorkloadTrendPoint> getWorkloadTrends(Long applicationId, LocalDateTime start, LocalDateTime end);
    com.project.autoscaling.scaling.dto.ScalingHistorySummaryResponse getScalingSummary(Long applicationId);
    com.project.autoscaling.infrastructure.dto.CapacitySummaryResponse getInstanceSummary(Long applicationId);
}
