package com.project.autoscaling.workload.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkloadRecordRequest {

    @NotNull(message = "Application ID is required")
    private Long applicationId;

    private LocalDateTime timestamp;

    @NotNull(message = "Request count is required")
    @Min(value = 0, message = "Request count must be non-negative")
    private Long requestCount;

    @NotNull(message = "Requests per second (RPS) is required")
    @PositiveOrZero(message = "Requests per second must be non-negative")
    private Double requestsPerSecond;

    @NotNull(message = "Successful requests count is required")
    @Min(value = 0, message = "Successful requests must be non-negative")
    private Long successfulRequests;

    @NotNull(message = "Failed requests count is required")
    @Min(value = 0, message = "Failed requests must be non-negative")
    private Long failedRequests;

    @NotNull(message = "Average latency is required")
    @PositiveOrZero(message = "Average latency must be non-negative")
    private Double averageLatency;

    @PositiveOrZero(message = "P95 latency must be non-negative")
    private Double p95Latency;

    @PositiveOrZero(message = "P99 latency must be non-negative")
    private Double p99Latency;
}
