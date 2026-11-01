package com.project.autoscaling.infrastructure.controller;

import com.project.autoscaling.common.enums.InstanceStatus;
import com.project.autoscaling.common.response.ApiResponse;
import com.project.autoscaling.common.response.PagedResponse;
import com.project.autoscaling.infrastructure.dto.*;
import com.project.autoscaling.infrastructure.service.InstanceService;
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

import java.util.List;

@RestController
@RequestMapping("/api/v1/instances")
@RequiredArgsConstructor
@Tag(name = "Infrastructure Management", description = "Endpoints for simulated/cloud compute instances and capacity management")
@SecurityRequirement(name = "Bearer Authentication")
public class InstanceController {

    private final InstanceService instanceService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Register a new infrastructure instance (ADMIN only)")
    public ResponseEntity<ApiResponse<InstanceResponse>> createInstance(
            @Valid @RequestBody CreateInstanceRequest request) {
        InstanceResponse response = instanceService.createInstance(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Instance registered successfully"));
    }

    @GetMapping
    @Operation(summary = "List all infrastructure instances with optional status filter and pagination")
    public ResponseEntity<ApiResponse<PagedResponse<InstanceResponse>>> getAllInstances(
            @RequestParam(required = false) InstanceStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String direction) {

        Sort.Direction sortDirection = direction.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));

        PagedResponse<InstanceResponse> response = instanceService.getAllInstances(status, pageable);
        return ResponseEntity.ok(ApiResponse.success(response, "Instances retrieved successfully"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Retrieve instance details by ID")
    public ResponseEntity<ApiResponse<InstanceResponse>> getInstanceById(@PathVariable Long id) {
        InstanceResponse response = instanceService.getInstanceById(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Instance details retrieved"));
    }

    @GetMapping("/application/{applicationId}")
    @Operation(summary = "Retrieve all instances assigned to an application")
    public ResponseEntity<ApiResponse<List<InstanceResponse>>> getInstancesByApplicationId(@PathVariable Long applicationId) {
        List<InstanceResponse> response = instanceService.getInstancesByApplicationId(applicationId);
        return ResponseEntity.ok(ApiResponse.success(response, "Application instances retrieved"));
    }

    @GetMapping("/application/{applicationId}/capacity")
    @Operation(summary = "Calculate active compute capacity (cores, memory, counts) for an application")
    public ResponseEntity<ApiResponse<CapacitySummaryResponse>> getCapacitySummary(@PathVariable Long applicationId) {
        CapacitySummaryResponse response = instanceService.getCapacitySummary(applicationId);
        return ResponseEntity.ok(ApiResponse.success(response, "Capacity summary calculated"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update instance configuration (ADMIN only)")
    public ResponseEntity<ApiResponse<InstanceResponse>> updateInstance(
            @PathVariable Long id,
            @Valid @RequestBody UpdateInstanceRequest request) {
        InstanceResponse response = instanceService.updateInstance(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Instance configuration updated"));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Change instance status (RUNNING, STOPPED, TERMINATED, UNHEALTHY) (ADMIN only)")
    public ResponseEntity<ApiResponse<InstanceResponse>> updateInstanceStatus(
            @PathVariable Long id,
            @Valid @RequestBody InstanceStatusUpdateRequest request) {
        InstanceResponse response = instanceService.updateInstanceStatus(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Instance status updated"));
    }
}
