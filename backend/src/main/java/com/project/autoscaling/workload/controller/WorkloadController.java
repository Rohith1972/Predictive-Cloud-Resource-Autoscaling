package com.project.autoscaling.workload.controller;

import com.project.autoscaling.common.response.ApiResponse;
import com.project.autoscaling.common.response.PagedResponse;
import com.project.autoscaling.workload.dto.WorkloadRecordRequest;
import com.project.autoscaling.workload.dto.WorkloadResponse;
import com.project.autoscaling.workload.dto.WorkloadSummaryResponse;
import com.project.autoscaling.workload.dto.WorkloadTrendResponse;
import com.project.autoscaling.workload.service.WorkloadService;
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
@RequestMapping("/api/v1/workload")
@RequiredArgsConstructor
@Tag(name = "Workload Management", description = "Endpoints for request throughput, latency percentiles, and traffic surge detection")
@SecurityRequirement(name = "Bearer Authentication")
public class WorkloadController {

    private final WorkloadService workloadService;

    @PostMapping("/records")
    @Operation(summary = "Record a new workload observation (RPS, total requests, latency percentiles)")
    public ResponseEntity<ApiResponse<WorkloadResponse>> recordWorkload(
            @Valid @RequestBody WorkloadRecordRequest request) {
        WorkloadResponse response = workloadService.recordWorkload(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Workload record saved successfully"));
    }

    @GetMapping("/{applicationId}")
    @Operation(summary = "Evaluate workload trend (INCREASING, DECREASING, STABLE, TRAFFIC_SPIKE)")
    public ResponseEntity<ApiResponse<WorkloadTrendResponse>> getWorkloadTrend(@PathVariable Long applicationId) {
        WorkloadTrendResponse response = workloadService.getWorkloadTrend(applicationId);
        return ResponseEntity.ok(ApiResponse.success(response, "Workload trend evaluated"));
    }

    @GetMapping("/{applicationId}/summary")
    @Operation(summary = "Get aggregated workload summary over time range (average RPS, success rate, latency)")
    public ResponseEntity<ApiResponse<WorkloadSummaryResponse>> getWorkloadSummary(
            @PathVariable Long applicationId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {

        WorkloadSummaryResponse response = workloadService.getWorkloadSummary(applicationId, startTime, endTime);
        return ResponseEntity.ok(ApiResponse.success(response, "Workload summary retrieved"));
    }

    @GetMapping("/{applicationId}/history")
    @Operation(summary = "Retrieve historical workload observations with pagination and time filtering")
    public ResponseEntity<ApiResponse<PagedResponse<WorkloadResponse>>> getWorkloadHistory(
            @PathVariable Long applicationId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "timestamp") String sort,
            @RequestParam(defaultValue = "desc") String direction) {

        Sort.Direction sortDirection = direction.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));

        PagedResponse<WorkloadResponse> response = workloadService.getWorkloadHistory(applicationId, startTime, endTime, pageable);
        return ResponseEntity.ok(ApiResponse.success(response, "Workload history retrieved"));
    }
}
