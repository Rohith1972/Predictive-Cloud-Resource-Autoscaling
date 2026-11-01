package com.project.autoscaling.monitoring.controller;

import com.project.autoscaling.common.response.ApiResponse;
import com.project.autoscaling.common.response.PagedResponse;
import com.project.autoscaling.monitoring.dto.AggregateMetricsResponse;
import com.project.autoscaling.monitoring.dto.MetricIngestionRequest;
import com.project.autoscaling.monitoring.dto.MetricResponse;
import com.project.autoscaling.monitoring.dto.MonitoringSummaryResponse;
import com.project.autoscaling.monitoring.service.MonitoringService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/monitoring")
@RequiredArgsConstructor
@Tag(name = "Monitoring & Telemetry", description = "Endpoints for telemetry metric ingestion, history, aggregation, and resource utilization")
@SecurityRequirement(name = "Bearer Authentication")
public class MonitoringController {

    private final MonitoringService monitoringService;

    @PostMapping("/metrics")
    @Operation(summary = "Ingest a new telemetry data point (CPU, memory, request rate, latency, errors)")
    public ResponseEntity<ApiResponse<MetricResponse>> ingestMetric(
            @Valid @RequestBody MetricIngestionRequest request) {
        MetricResponse response = monitoringService.recordMetric(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Telemetry metric recorded successfully"));
    }

    @GetMapping("/latest/{applicationId}")
    @Operation(summary = "Get the latest recorded telemetry metric for an application")
    public ResponseEntity<ApiResponse<MetricResponse>> getLatestMetric(@PathVariable Long applicationId) {
        MetricResponse response = monitoringService.getLatestMetric(applicationId);
        return ResponseEntity.ok(ApiResponse.success(response, "Latest metric retrieved"));
    }

    @GetMapping("/instance/{instanceId}")
    @Operation(summary = "Get historical metrics for a specific instance with pagination")
    public ResponseEntity<ApiResponse<PagedResponse<MetricResponse>>> getMetricsByInstance(
            @PathVariable Long instanceId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "timestamp") String sort,
            @RequestParam(defaultValue = "desc") String direction) {

        Sort.Direction sortDirection = direction.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));

        PagedResponse<MetricResponse> response = monitoringService.getMetricsByInstance(instanceId, pageable);
        return ResponseEntity.ok(ApiResponse.success(response, "Instance metrics retrieved"));
    }

    @GetMapping("/history/{applicationId}")
    @Operation(summary = "Get historical metrics for an application within a time range with pagination")
    public ResponseEntity<ApiResponse<PagedResponse<MetricResponse>>> getHistoricalMetrics(
            @PathVariable Long applicationId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "timestamp") String sort,
            @RequestParam(defaultValue = "desc") String direction) {

        Sort.Direction sortDirection = direction.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));

        PagedResponse<MetricResponse> response = monitoringService.getHistoricalMetrics(applicationId, startTime, endTime, pageable);
        return ResponseEntity.ok(ApiResponse.success(response, "Historical metrics retrieved"));
    }

    @GetMapping("/aggregates/{applicationId}")
    @Operation(summary = "Get statistical aggregates (avg, min, max) for CPU, memory, request rate, error rate")
    public ResponseEntity<ApiResponse<AggregateMetricsResponse>> getAggregateMetrics(
            @PathVariable Long applicationId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {

        AggregateMetricsResponse response = monitoringService.getAggregateMetrics(applicationId, startTime, endTime);
        return ResponseEntity.ok(ApiResponse.success(response, "Aggregate metrics calculated"));
    }

    @GetMapping("/summary/{applicationId}")
    @Operation(summary = "Get current application-level resource utilization summary and health status")
    public ResponseEntity<ApiResponse<MonitoringSummaryResponse>> getMonitoringSummary(@PathVariable Long applicationId) {
        MonitoringSummaryResponse response = monitoringService.getMonitoringSummary(applicationId);
        return ResponseEntity.ok(ApiResponse.success(response, "Monitoring summary retrieved"));
    }
}
