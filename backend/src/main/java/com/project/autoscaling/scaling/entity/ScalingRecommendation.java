package com.project.autoscaling.scaling.entity;

import com.project.autoscaling.application.entity.Application;
import com.project.autoscaling.common.enums.ExecutionMode;
import com.project.autoscaling.common.enums.RecommendationStatus;
import com.project.autoscaling.common.enums.ScalingAction;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "scaling_recommendations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScalingRecommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false)
    private Application application;

    @Column(name = "current_instance_count", nullable = false)
    private Integer currentInstanceCount;

    @Column(name = "recommended_instance_count", nullable = false)
    private Integer recommendedInstanceCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "scaling_action", nullable = false, length = 50)
    private ScalingAction scalingAction;

    @Column(name = "triggering_metric", nullable = false, length = 100)
    private String triggeringMetric;

    @Column(name = "observed_metric_value", nullable = false)
    private Double observedMetricValue;

    @Column(name = "configured_threshold", nullable = false)
    private Double configuredThreshold;

    @Column(name = "decision_reason", nullable = false, columnDefinition = "TEXT")
    private String decisionReason;

    @Enumerated(EnumType.STRING)
    @Column(name = "execution_mode", nullable = false, length = 50)
    @Builder.Default
    private ExecutionMode executionMode = ExecutionMode.MANUAL_APPROVAL;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private RecommendationStatus status = RecommendationStatus.PENDING;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "evaluated_at", nullable = false)
    @Builder.Default
    private LocalDateTime evaluatedAt = LocalDateTime.now();
}
