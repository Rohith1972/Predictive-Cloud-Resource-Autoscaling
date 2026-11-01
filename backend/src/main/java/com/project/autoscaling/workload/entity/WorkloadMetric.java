package com.project.autoscaling.workload.entity;

import com.project.autoscaling.application.entity.Application;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "workload_metrics")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkloadMetric {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false)
    private Application application;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "request_count", nullable = false)
    private Long requestCount;

    @Column(name = "requests_per_second", nullable = false)
    private Double requestsPerSecond;

    @Column(name = "successful_requests", nullable = false)
    private Long successfulRequests;

    @Column(name = "failed_requests", nullable = false)
    private Long failedRequests;

    @Column(name = "average_latency", nullable = false)
    private Double averageLatency;

    @Column(name = "p95_latency", nullable = false)
    private Double p95Latency;

    @Column(name = "p99_latency", nullable = false)
    private Double p99Latency;
}
