package com.project.autoscaling.application.controller;

import com.project.autoscaling.application.dto.*;
import com.project.autoscaling.application.service.ApplicationService;
import com.project.autoscaling.common.enums.ApplicationStatus;
import com.project.autoscaling.common.response.ApiResponse;
import com.project.autoscaling.common.response.PagedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/applications")
@RequiredArgsConstructor
@Tag(name = "Application Management", description = "Endpoints for registering, configuring, and managing monitored applications")
@SecurityRequirement(name = "Bearer Authentication")
public class ApplicationController {

    private final ApplicationService applicationService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Register a new monitored application (ADMIN only)")
    public ResponseEntity<ApiResponse<ApplicationResponse>> createApplication(
            @Valid @RequestBody CreateApplicationRequest request) {
        ApplicationResponse response = applicationService.createApplication(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Application registered successfully"));
    }

    @GetMapping
    @Operation(summary = "Retrieve all registered applications with optional status filtering and pagination")
    public ResponseEntity<ApiResponse<PagedResponse<ApplicationResponse>>> getAllApplications(
            @RequestParam(required = false) ApplicationStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String direction) {

        Sort.Direction sortDirection = direction.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));

        PagedResponse<ApplicationResponse> response = applicationService.getAllApplications(status, pageable);
        return ResponseEntity.ok(ApiResponse.success(response, "Applications retrieved successfully"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Retrieve application details by ID")
    public ResponseEntity<ApiResponse<ApplicationResponse>> getApplicationById(@PathVariable Long id) {
        ApplicationResponse response = applicationService.getApplicationById(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Application details retrieved"));
    }

    @GetMapping("/{id}/summary")
    @Operation(summary = "Retrieve application monitoring and capacity summary")
    public ResponseEntity<ApiResponse<ApplicationSummaryResponse>> getApplicationSummary(@PathVariable Long id) {
        ApplicationSummaryResponse response = applicationService.getApplicationSummary(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Application summary retrieved"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update application details (ADMIN only)")
    public ResponseEntity<ApiResponse<ApplicationResponse>> updateApplication(
            @PathVariable Long id,
            @Valid @RequestBody UpdateApplicationRequest request) {
        ApplicationResponse response = applicationService.updateApplication(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Application updated successfully"));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Activate or deactivate an application (ADMIN only)")
    public ResponseEntity<ApiResponse<ApplicationResponse>> updateApplicationStatus(
            @PathVariable Long id,
            @Valid @RequestBody ApplicationStatusUpdateRequest request) {
        ApplicationResponse response = applicationService.updateApplicationStatus(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Application status updated successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete an application if safe (no active instances) (ADMIN only)")
    public ResponseEntity<ApiResponse<Void>> deleteApplication(@PathVariable Long id) {
        applicationService.deleteApplication(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Application deleted successfully"));
    }
}
