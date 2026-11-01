package com.project.autoscaling.scaling.controller;

import com.project.autoscaling.common.response.ApiResponse;
import com.project.autoscaling.common.response.PagedResponse;
import com.project.autoscaling.scaling.dto.CreateScalingPolicyRequest;
import com.project.autoscaling.scaling.dto.ScalingPolicyResponse;
import com.project.autoscaling.scaling.dto.ScalingPolicyStatusUpdateRequest;
import com.project.autoscaling.scaling.dto.UpdateScalingPolicyRequest;
import com.project.autoscaling.scaling.service.ScalingPolicyService;
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
@RequestMapping("/api/v1/scaling/policies")
@RequiredArgsConstructor
@Tag(name = "Scaling Policies", description = "Endpoints for configuring scaling policies, capacity thresholds, and cooldowns")
@SecurityRequirement(name = "Bearer Authentication")
public class ScalingPolicyController {

    private final ScalingPolicyService policyService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new scaling policy for an application (ADMIN only)")
    public ResponseEntity<ApiResponse<ScalingPolicyResponse>> createPolicy(
            @Valid @RequestBody CreateScalingPolicyRequest request) {
        ScalingPolicyResponse response = policyService.createPolicy(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Scaling policy created successfully"));
    }

    @GetMapping
    @Operation(summary = "List all scaling policies with pagination")
    public ResponseEntity<ApiResponse<PagedResponse<ScalingPolicyResponse>>> getAllPolicies(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String direction) {

        Sort.Direction sortDirection = direction.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));

        PagedResponse<ScalingPolicyResponse> response = policyService.getAllPolicies(pageable);
        return ResponseEntity.ok(ApiResponse.success(response, "Scaling policies retrieved"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Retrieve scaling policy by ID")
    public ResponseEntity<ApiResponse<ScalingPolicyResponse>> getPolicyById(@PathVariable Long id) {
        ScalingPolicyResponse response = policyService.getPolicyById(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Scaling policy retrieved"));
    }

    @GetMapping("/application/{applicationId}")
    @Operation(summary = "Retrieve scaling policy for a specific application")
    public ResponseEntity<ApiResponse<ScalingPolicyResponse>> getPolicyByApplicationId(@PathVariable Long applicationId) {
        ScalingPolicyResponse response = policyService.getPolicyByApplicationId(applicationId);
        return ResponseEntity.ok(ApiResponse.success(response, "Application scaling policy retrieved"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update scaling policy thresholds and capacities (ADMIN only)")
    public ResponseEntity<ApiResponse<ScalingPolicyResponse>> updatePolicy(
            @PathVariable Long id,
            @Valid @RequestBody UpdateScalingPolicyRequest request) {
        ScalingPolicyResponse response = policyService.updatePolicy(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Scaling policy updated successfully"));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Enable or disable a scaling policy (ADMIN only)")
    public ResponseEntity<ApiResponse<ScalingPolicyResponse>> updatePolicyStatus(
            @PathVariable Long id,
            @Valid @RequestBody ScalingPolicyStatusUpdateRequest request) {
        ScalingPolicyResponse response = policyService.updatePolicyStatus(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Scaling policy status updated"));
    }
}
