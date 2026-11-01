package com.project.autoscaling.simulation.dto;

import com.project.autoscaling.common.enums.WorkloadProfile;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StartSimulationRequest {

    @Builder.Default
    @Schema(description = "Workload simulation profile", example = "TRAFFIC_SPIKE")
    private WorkloadProfile workloadProfile = WorkloadProfile.NORMAL;

    @Builder.Default
    @Min(value = 1, message = "Interval must be at least 1 second")
    @Max(value = 300, message = "Interval cannot exceed 300 seconds")
    @Schema(description = "Interval in seconds between telemetry generations", example = "5")
    private int intervalSeconds = 5;

    @Schema(description = "Optional random seed for deterministic metrics generation", example = "42")
    private Long randomSeed;

    @Schema(description = "Optional duration limit in seconds; if omitted, runs until manually stopped", example = "120")
    private Integer durationSeconds;

    @Schema(description = "Baseline CPU utilization percentage (0-100)", example = "45.0")
    private Double baseCpu;

    @Schema(description = "Baseline memory utilization percentage (0-100)", example = "50.0")
    private Double baseMemory;

    @Schema(description = "Baseline request rate (requests/sec)", example = "100.0")
    private Double baseRequestRate;
}
