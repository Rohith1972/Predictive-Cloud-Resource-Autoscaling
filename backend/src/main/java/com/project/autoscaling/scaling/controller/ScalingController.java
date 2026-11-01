package com.project.autoscaling.scaling.controller;

import com.project.autoscaling.common.response.ApiResponse;
import com.project.autoscaling.common.response.PagedResponse;
import com.project.autoscaling.scaling.dto.*;
import com.project.autoscaling.scaling.service.ScalingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/scaling")
@RequiredArgsConstructor
@Tag(name = "Autoscaling Engine & History", description = "Endpoints for reactive autoscaling evaluations, manual approval/rejection, and audit logs")
@SecurityRequirement(name = "Bearer Authentication")
public class ScalingController {

    private final ScalingService scalingService;

    @PostMapping("/evaluate/{applicationId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Trigger reactive scaling evaluation for an application based on latest telemetry and policies (ADMIN only)")
    public ResponseEntity<ApiResponse<ScalingRecommendationResponse>> evaluateScaling(
            @PathVariable Long applicationId,
            @RequestBody(required = false) ScalingEvaluationRequest request) {
        ScalingRecommendationResponse response = scalingService.evaluateScaling(applicationId, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Scaling evaluation completed"));
    }

    @GetMapping("/recommendations/{applicationId}")
    @Operation(summary = "List all scaling recommendations for an application")
    public ResponseEntity<ApiResponse<List<ScalingRecommendationResponse>>> getRecommendations(
            @PathVariable Long applicationId) {
        List<ScalingRecommendationResponse> response = scalingService.getRecommendationsByApplication(applicationId);
        return ResponseEntity.ok(ApiResponse.success(response, "Recommendations retrieved"));
    }

    @GetMapping("/recommendation/{recommendationId}")
    @Operation(summary = "Retrieve specific recommendation details by ID")
    public ResponseEntity<ApiResponse<ScalingRecommendationResponse>> getRecommendationById(
            @PathVariable Long recommendationId) {
        ScalingRecommendationResponse response = scalingService.getRecommendationById(recommendationId);
        return ResponseEntity.ok(ApiResponse.success(response, "Recommendation retrieved"));
    }

    @PostMapping("/recommendations/{recommendationId}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Approve a scaling recommendation and apply capacity change (ADMIN only)")
    public ResponseEntity<ApiResponse<ScalingEventResponse>> approveRecommendation(
            @PathVariable Long recommendationId) {
        ScalingEventResponse response = scalingService.approveRecommendation(recommendationId);
        return ResponseEntity.ok(ApiResponse.success(response, "Scaling recommendation approved and capacity updated"));
    }

    @PostMapping("/recommendations/{recommendationId}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Reject a scaling recommendation (ADMIN only)")
    public ResponseEntity<ApiResponse<ScalingRecommendationResponse>> rejectRecommendation(
            @PathVariable Long recommendationId,
            @RequestParam(required = false) String reason) {
        ScalingRecommendationResponse response = scalingService.rejectRecommendation(recommendationId, reason);
        return ResponseEntity.ok(ApiResponse.success(response, "Scaling recommendation rejected"));
    }

    @GetMapping("/history/{applicationId}")
    @Operation(summary = "Retrieve persistent audit trail of scaling evaluations and capacity changes with pagination")
    public ResponseEntity<ApiResponse<PagedResponse<ScalingEventResponse>>> getScalingHistory(
            @PathVariable Long applicationId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String direction) {

        Sort.Direction sortDirection = direction.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));

        PagedResponse<ScalingEventResponse> response = scalingService.getScalingHistory(applicationId, startTime, endTime, pageable);
        return ResponseEntity.ok(ApiResponse.success(response, "Scaling history retrieved"));
    }

    @GetMapping("/history/event/{eventId}")
    @Operation(summary = "Retrieve specific scaling audit event details")
    public ResponseEntity<ApiResponse<ScalingEventResponse>> getScalingEventById(@PathVariable Long eventId) {
        ScalingEventResponse response = scalingService.getScalingEventById(eventId);
        return ResponseEntity.ok(ApiResponse.success(response, "Scaling event details retrieved"));
    }

    @GetMapping("/history/{applicationId}/summary")
    @Operation(summary = "Get aggregate scaling event summary (total scale-outs, scale-ins, last scaled timestamp)")
    public ResponseEntity<ApiResponse<ScalingHistorySummaryResponse>> getScalingHistorySummary(
            @PathVariable Long applicationId) {
        ScalingHistorySummaryResponse response = scalingService.getScalingHistorySummary(applicationId);
        return ResponseEntity.ok(ApiResponse.success(response, "Scaling history summary retrieved"));
    }
}
