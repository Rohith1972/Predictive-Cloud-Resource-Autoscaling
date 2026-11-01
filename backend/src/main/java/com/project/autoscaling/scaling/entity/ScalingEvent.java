package com.project.autoscaling.scaling.entity;

import com.project.autoscaling.application.entity.Application;
import com.project.autoscaling.common.enums.ExecutionMode;
import com.project.autoscaling.common.enums.ScalingAction;
import com.project.autoscaling.common.enums.ScalingEventStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "scaling_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScalingEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false)
    private Application application;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recommendation_id")
    private ScalingRecommendation recommendation;

    @Column(name = "previous_instance_count", nullable = false)
    private Integer previousInstanceCount;

    @Column(name = "new_instance_count", nullable = false)
    private Integer newInstanceCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "scaling_action", nullable = false, length = 50)
    private ScalingAction scalingAction;

    @Column(name = "triggering_metric", nullable = false, length = 100)
    private String triggeringMetric;

    @Column(name = "triggering_metric_value", nullable = false)
    private Double triggeringMetricValue;

    @Column(name = "decision_reason", nullable = false, columnDefinition = "TEXT")
    private String decisionReason;

    @Enumerated(EnumType.STRING)
    @Column(name = "execution_mode", nullable = false, length = 50)
    private ExecutionMode executionMode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private ScalingEventStatus status = ScalingEventStatus.COMPLETED;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;
}
