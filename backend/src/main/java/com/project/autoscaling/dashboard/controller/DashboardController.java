package com.project.autoscaling.dashboard.controller;

import com.project.autoscaling.common.response.ApiResponse;
import com.project.autoscaling.dashboard.dto.ApplicationDashboardResponse;
import com.project.autoscaling.dashboard.dto.DashboardOverviewResponse;
import com.project.autoscaling.dashboard.dto.ResourceTrendPoint;
import com.project.autoscaling.dashboard.dto.WorkloadTrendPoint;
import com.project.autoscaling.dashboard.service.DashboardService;
import com.project.autoscaling.infrastructure.dto.CapacitySummaryResponse;
import com.project.autoscaling.scaling.dto.ScalingHistorySummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "High-performance aggregated dashboard analytics for cloud infrastructure")
@PreAuthorize("hasAnyRole('ADMIN', 'VIEWER')")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/overview")
    @Operation(summary = "Get global dashboard overview across all applications and instances")
    public ResponseEntity<ApiResponse<DashboardOverviewResponse>> getOverview() {
        return ResponseEntity.ok(ApiResponse.success(
                dashboardService.getOverview(),
                "Dashboard overview retrieved successfully"
        ));
    }

    @GetMapping("/application/{applicationId}")
    @Operation(summary = "Get comprehensive single-application dashboard snapshot")
    public ResponseEntity<ApiResponse<ApplicationDashboardResponse>> getApplicationDashboard(
            @PathVariable Long applicationId) {
        return ResponseEntity.ok(ApiResponse.success(
                dashboardService.getApplicationDashboard(applicationId),
                "Application dashboard retrieved successfully"
        ));
    }

    @GetMapping("/resource-trends/{applicationId}")
    @Operation(summary = "Get historical resource utilization trends for charts")
    public ResponseEntity<ApiResponse<List<ResourceTrendPoint>>> getResourceTrends(
            @PathVariable Long applicationId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        return ResponseEntity.ok(ApiResponse.success(
                dashboardService.getResourceTrends(applicationId, startTime, endTime),
                "Resource trends retrieved successfully"
        ));
    }

    @GetMapping("/workload-trends/{applicationId}")
    @Operation(summary = "Get historical workload and latency trends for charts")
    public ResponseEntity<ApiResponse<List<WorkloadTrendPoint>>> getWorkloadTrends(
            @PathVariable Long applicationId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        return ResponseEntity.ok(ApiResponse.success(
                dashboardService.getWorkloadTrends(applicationId, startTime, endTime),
                "Workload trends retrieved successfully"
        ));
    }

    @GetMapping("/scaling-summary/{applicationId}")
    @Operation(summary = "Get scaling history summary for application")
    public ResponseEntity<ApiResponse<ScalingHistorySummaryResponse>> getScalingSummary(
            @PathVariable Long applicationId) {
        return ResponseEntity.ok(ApiResponse.success(
                dashboardService.getScalingSummary(applicationId),
                "Scaling summary retrieved successfully"
        ));
    }

    @GetMapping("/instance-summary/{applicationId}")
    @Operation(summary = "Get instance capacity summary for application")
    public ResponseEntity<ApiResponse<CapacitySummaryResponse>> getInstanceSummary(
            @PathVariable Long applicationId) {
        return ResponseEntity.ok(ApiResponse.success(
                dashboardService.getInstanceSummary(applicationId),
                "Instance capacity summary retrieved successfully"
        ));
    }
}
