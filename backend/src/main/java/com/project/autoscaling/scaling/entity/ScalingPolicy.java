package com.project.autoscaling.scaling.entity;

import com.project.autoscaling.application.entity.Application;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "scaling_policies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScalingPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false, unique = true)
    private Application application;

    @Column(name = "minimum_instances", nullable = false)
    @Builder.Default
    private Integer minimumInstances = 1;

    @Column(name = "maximum_instances", nullable = false)
    @Builder.Default
    private Integer maximumInstances = 10;

    @Column(name = "desired_instances", nullable = false)
    @Builder.Default
    private Integer desiredInstances = 1;

    @Column(name = "cpu_scale_out_threshold", nullable = false)
    @Builder.Default
    private Double cpuScaleOutThreshold = 75.0;

    @Column(name = "cpu_scale_in_threshold", nullable = false)
    @Builder.Default
    private Double cpuScaleInThreshold = 25.0;

    @Column(name = "memory_scale_out_threshold", nullable = false)
    @Builder.Default
    private Double memoryScaleOutThreshold = 80.0;

    @Column(name = "memory_scale_in_threshold", nullable = false)
    @Builder.Default
    private Double memoryScaleInThreshold = 30.0;

    @Column(name = "request_rate_threshold", nullable = false)
    @Builder.Default
    private Double requestRateThreshold = 500.0;

    @Column(name = "cooldown_seconds", nullable = false)
    @Builder.Default
    private Integer cooldownSeconds = 300;

    @Column(name = "enabled", nullable = false)
    @Builder.Default
    private Boolean enabled = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
