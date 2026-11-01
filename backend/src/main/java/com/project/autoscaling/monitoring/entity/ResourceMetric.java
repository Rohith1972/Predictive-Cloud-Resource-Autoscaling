package com.project.autoscaling.monitoring.entity;

import com.project.autoscaling.application.entity.Application;
import com.project.autoscaling.infrastructure.entity.Instance;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "resource_metrics")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResourceMetric {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false)
    private Application application;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "instance_id", nullable = false)
    private Instance instance;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "cpu_utilization", nullable = false)
    private Double cpuUtilization;

    @Column(name = "memory_utilization", nullable = false)
    private Double memoryUtilization;

    @Column(name = "request_rate", nullable = false)
    private Double requestRate;

    @Column(name = "average_response_time", nullable = false)
    private Double averageResponseTime;

    @Column(name = "error_rate", nullable = false)
    private Double errorRate;

    @Column(name = "active_requests", nullable = false)
    @Builder.Default
    private Integer activeRequests = 0;

    @Column(name = "network_in_bytes", nullable = false)
    @Builder.Default
    private Long networkInBytes = 0L;

    @Column(name = "network_out_bytes", nullable = false)
    @Builder.Default
    private Long networkOutBytes = 0L;
}
