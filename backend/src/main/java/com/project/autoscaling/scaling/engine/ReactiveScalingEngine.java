package com.project.autoscaling.scaling.engine;

import com.project.autoscaling.common.enums.ScalingAction;
import com.project.autoscaling.monitoring.entity.ResourceMetric;
import com.project.autoscaling.scaling.entity.ScalingEvent;
import com.project.autoscaling.scaling.entity.ScalingPolicy;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class ReactiveScalingEngine {

    private static final Logger log = LoggerFactory.getLogger(ReactiveScalingEngine.class);
    private final CapacityCalculationService capacityCalculationService;

    public ScalingEvaluationResult evaluate(
            ScalingPolicy policy,
            ResourceMetric latestMetric,
            int currentRunningInstances,
            ScalingEvent lastScalingEvent
    ) {
        if (!Boolean.TRUE.equals(policy.getEnabled())) {
            return ScalingEvaluationResult.builder()
                    .action(ScalingAction.NO_ACTION)
                    .currentInstanceCount(currentRunningInstances)
                    .recommendedInstanceCount(currentRunningInstances)
                    .triggeringMetric("NONE")
                    .observedMetricValue(0.0)
                    .configuredThreshold(0.0)
                    .decisionReason("Scaling policy is currently disabled for this application")
                    .build();
        }

        // Safeguard 1: Missing telemetry
        if (latestMetric == null) {
            return ScalingEvaluationResult.builder()
                    .action(ScalingAction.NO_ACTION)
                    .currentInstanceCount(currentRunningInstances)
                    .recommendedInstanceCount(currentRunningInstances)
                    .triggeringMetric("NONE")
                    .observedMetricValue(0.0)
                    .configuredThreshold(0.0)
                    .decisionReason("No recent telemetry metrics available to make an autoscaling decision")
                    .build();
        }

        // Safeguard 2: Cooldown check
        if (lastScalingEvent != null && lastScalingEvent.getScalingAction() != ScalingAction.NO_ACTION) {
            long secondsSinceLastScale = Duration.between(lastScalingEvent.getCreatedAt(), LocalDateTime.now()).getSeconds();
            if (secondsSinceLastScale < policy.getCooldownSeconds()) {
                long remaining = policy.getCooldownSeconds() - secondsSinceLastScale;
                return ScalingEvaluationResult.builder()
                        .action(ScalingAction.NO_ACTION)
                        .currentInstanceCount(currentRunningInstances)
                        .recommendedInstanceCount(currentRunningInstances)
                        .triggeringMetric("COOLDOWN")
                        .observedMetricValue((double) secondsSinceLastScale)
                        .configuredThreshold((double) policy.getCooldownSeconds())
                        .decisionReason(String.format("Cooldown active: %d seconds elapsed of configured %d seconds (%d seconds remaining). Scaling suspended.",
                                secondsSinceLastScale, policy.getCooldownSeconds(), remaining))
                        .build();
            }
        }

        double cpu = latestMetric.getCpuUtilization();
        double mem = latestMetric.getMemoryUtilization();
        double rps = latestMetric.getRequestRate();

        // 1. Evaluate Scale-Out Triggers
        if (cpu >= policy.getCpuScaleOutThreshold()) {
            return handleScaleOut(currentRunningInstances, "CPU_UTILIZATION", cpu, policy.getCpuScaleOutThreshold(), policy);
        }
        if (mem >= policy.getMemoryScaleOutThreshold()) {
            return handleScaleOut(currentRunningInstances, "MEMORY_UTILIZATION", mem, policy.getMemoryScaleOutThreshold(), policy);
        }
        if (rps >= policy.getRequestRateThreshold()) {
            return handleScaleOut(currentRunningInstances, "REQUEST_RATE", rps, policy.getRequestRateThreshold(), policy);
        }

        // 2. Evaluate Scale-In Triggers
        // Scale-in requires CPU below scale-in threshold AND Memory below scale-in threshold AND RPS sufficiently low
        boolean cpuLow = cpu <= policy.getCpuScaleInThreshold();
        boolean memLow = mem <= policy.getMemoryScaleInThreshold();
        boolean rpsLow = rps < (policy.getRequestRateThreshold() * 0.5);

        if (cpuLow && memLow && rpsLow) {
            return handleScaleIn(currentRunningInstances, "CPU_AND_MEMORY_LOW", cpu, policy.getCpuScaleInThreshold(), policy);
        }

        // 3. Normal / No Action
        return ScalingEvaluationResult.builder()
                .action(ScalingAction.NO_ACTION)
                .currentInstanceCount(currentRunningInstances)
                .recommendedInstanceCount(currentRunningInstances)
                .triggeringMetric("NONE")
                .observedMetricValue(cpu)
                .configuredThreshold(policy.getCpuScaleOutThreshold())
                .decisionReason(String.format("All resource metrics are within acceptable operating bounds (CPU: %.1f%%, Memory: %.1f%%, RPS: %.1f)", cpu, mem, rps))
                .build();
    }

    private ScalingEvaluationResult handleScaleOut(
            int currentInstances,
            String metricName,
            double observedValue,
            double threshold,
            ScalingPolicy policy
    ) {
        if (currentInstances >= policy.getMaximumInstances()) {
            return ScalingEvaluationResult.builder()
                    .action(ScalingAction.NO_ACTION)
                    .currentInstanceCount(currentInstances)
                    .recommendedInstanceCount(currentInstances)
                    .triggeringMetric(metricName)
                    .observedMetricValue(observedValue)
                    .configuredThreshold(threshold)
                    .decisionReason(String.format("%s (%.1f) exceeded scale-out threshold (%.1f), but maximum instance limit (%d) is already reached",
                            metricName, observedValue, threshold, policy.getMaximumInstances()))
                    .build();
        }

        int target = capacityCalculationService.calculateScaleOutCapacity(
                currentInstances,
                observedValue,
                threshold,
                policy.getMinimumInstances(),
                policy.getMaximumInstances()
        );

        return ScalingEvaluationResult.builder()
                .action(ScalingAction.SCALE_OUT)
                .currentInstanceCount(currentInstances)
                .recommendedInstanceCount(target)
                .triggeringMetric(metricName)
                .observedMetricValue(observedValue)
                .configuredThreshold(threshold)
                .decisionReason(String.format("%s (%.1f) exceeded scale-out threshold (%.1f). Recommending scale-out from %d to %d instances.",
                        metricName, observedValue, threshold, currentInstances, target))
                .build();
    }

    private ScalingEvaluationResult handleScaleIn(
            int currentInstances,
            String metricName,
            double observedValue,
            double threshold,
            ScalingPolicy policy
    ) {
        if (currentInstances <= policy.getMinimumInstances()) {
            return ScalingEvaluationResult.builder()
                    .action(ScalingAction.NO_ACTION)
                    .currentInstanceCount(currentInstances)
                    .recommendedInstanceCount(currentInstances)
                    .triggeringMetric(metricName)
                    .observedMetricValue(observedValue)
                    .configuredThreshold(threshold)
                    .decisionReason(String.format("Resources are under-utilized (observed: %.1f, threshold: %.1f), but minimum instance limit (%d) is already reached",
                            observedValue, threshold, policy.getMinimumInstances()))
                    .build();
        }

        int target = capacityCalculationService.calculateScaleInCapacity(
                currentInstances,
                observedValue,
                threshold,
                policy.getMinimumInstances(),
                policy.getMaximumInstances()
        );

        return ScalingEvaluationResult.builder()
                .action(ScalingAction.SCALE_IN)
                .currentInstanceCount(currentInstances)
                .recommendedInstanceCount(target)
                .triggeringMetric(metricName)
                .observedMetricValue(observedValue)
                .configuredThreshold(threshold)
                .decisionReason(String.format("Resources under-utilized (observed: %.1f, threshold: %.1f). Recommending safe scale-in from %d to %d instances.",
                        observedValue, threshold, currentInstances, target))
                .build();
    }
}
