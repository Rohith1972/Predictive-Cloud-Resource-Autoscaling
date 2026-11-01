package com.project.autoscaling.alerts.controller;

import com.project.autoscaling.alerts.dto.AlertResponse;
import com.project.autoscaling.alerts.service.AlertService;
import com.project.autoscaling.common.enums.AlertSeverity;
import com.project.autoscaling.common.enums.AlertStatus;
import com.project.autoscaling.common.response.ApiResponse;
import com.project.autoscaling.common.response.PagedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/alerts")
@RequiredArgsConstructor
@Tag(name = "Alert Management", description = "Endpoints for infrastructure alarms, threshold breach detection, and incident resolution")
@SecurityRequirement(name = "Bearer Authentication")
public class AlertController {

    private final AlertService alertService;

    @GetMapping
    @Operation(summary = "List all alerts with optional status/severity filters and pagination")
    public ResponseEntity<ApiResponse<PagedResponse<AlertResponse>>> getAlerts(
            @RequestParam(required = false) AlertStatus status,
            @RequestParam(required = false) AlertSeverity severity,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String direction) {

        Sort.Direction sortDirection = direction.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));

        PagedResponse<AlertResponse> response = alertService.getAlerts(status, severity, pageable);
        return ResponseEntity.ok(ApiResponse.success(response, "Alerts retrieved"));
    }

    @GetMapping("/active")
    @Operation(summary = "Retrieve all currently active un-resolved alerts")
    public ResponseEntity<ApiResponse<List<AlertResponse>>> getActiveAlerts() {
        List<AlertResponse> response = alertService.getActiveAlerts();
        return ResponseEntity.ok(ApiResponse.success(response, "Active alerts retrieved"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Retrieve specific alert details by ID")
    public ResponseEntity<ApiResponse<AlertResponse>> getAlertById(@PathVariable Long id) {
        AlertResponse response = alertService.getAlertById(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Alert retrieved"));
    }

    @PatchMapping("/{id}/acknowledge")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Mark an alert as acknowledged (ADMIN only)")
    public ResponseEntity<ApiResponse<AlertResponse>> acknowledgeAlert(@PathVariable Long id) {
        AlertResponse response = alertService.acknowledgeAlert(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Alert acknowledged successfully"));
    }

    @PatchMapping("/{id}/resolve")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Mark an alert as resolved (ADMIN only)")
    public ResponseEntity<ApiResponse<AlertResponse>> resolveAlert(@PathVariable Long id) {
        AlertResponse response = alertService.resolveAlert(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Alert resolved successfully"));
    }
}
