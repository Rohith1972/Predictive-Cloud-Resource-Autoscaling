package com.project.autoscaling.simulation.controller;

import com.project.autoscaling.common.response.ApiResponse;
import com.project.autoscaling.simulation.dto.SimulationStatusResponse;
import com.project.autoscaling.simulation.dto.StartSimulationRequest;
import com.project.autoscaling.simulation.service.WorkloadSimulationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/simulation")
@RequiredArgsConstructor
@Tag(name = "Workload Simulation", description = "Controlled telemetry generation engine for local testing without real AWS cloud infrastructure")
@SecurityRequirement(name = "Bearer Authentication")
public class SimulationController {

    private final WorkloadSimulationService simulationService;

    @PostMapping("/start/{applicationId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Start scheduled telemetry and workload simulation for an application (ADMIN only)")
    public ResponseEntity<ApiResponse<SimulationStatusResponse>> startSimulation(
            @PathVariable Long applicationId,
            @Valid @RequestBody(required = false) StartSimulationRequest request) {

        StartSimulationRequest simRequest = request != null ? request : new StartSimulationRequest();
        SimulationStatusResponse response = simulationService.startSimulation(applicationId, simRequest);
        return ResponseEntity.ok(ApiResponse.success(
                response,
                "Workload simulation started successfully"
        ));
    }

    @PostMapping("/stop/{applicationId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Stop actively running workload simulation for an application (ADMIN only)")
    public ResponseEntity<ApiResponse<SimulationStatusResponse>> stopSimulation(
            @PathVariable Long applicationId) {

        SimulationStatusResponse response = simulationService.stopSimulation(applicationId);
        return ResponseEntity.ok(ApiResponse.success(
                response,
                "Workload simulation stopped successfully"
        ));
    }

    @GetMapping("/status/{applicationId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'VIEWER')")
    @Operation(summary = "Get current workload simulation status for an application")
    public ResponseEntity<ApiResponse<SimulationStatusResponse>> getSimulationStatus(
            @PathVariable Long applicationId) {

        SimulationStatusResponse response = simulationService.getSimulationStatus(applicationId);
        return ResponseEntity.ok(ApiResponse.success(
                response,
                "Simulation status retrieved successfully"
        ));
    }
}
