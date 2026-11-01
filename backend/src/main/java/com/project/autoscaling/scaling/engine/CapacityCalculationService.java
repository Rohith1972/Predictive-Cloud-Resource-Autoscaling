package com.project.autoscaling.scaling.engine;

import org.springframework.stereotype.Service;

@Service
public class CapacityCalculationService {

    /**
     * Calculates recommended capacity based on formula:
     * recommendedInstances = ceil(currentInstances * currentMetric / targetMetric)
     * Clamped to [minimumInstances, maximumInstances].
     */
    public int calculateRecommendedCapacity(int currentInstances, double currentMetric, double targetMetric, int minInstances, int maxInstances) {
        if (currentInstances <= 0) {
            return minInstances;
        }

        if (targetMetric <= 0.0) {
            return Math.min(currentInstances + 1, maxInstances);
        }

        double ratio = currentMetric / targetMetric;
        int calculated = (int) Math.ceil(currentInstances * ratio);

        // Clamp to allowed range
        return Math.max(minInstances, Math.min(maxInstances, calculated));
    }

    public int calculateScaleOutCapacity(int currentInstances, double currentMetric, double targetMetric, int minInstances, int maxInstances) {
        int recommended = calculateRecommendedCapacity(currentInstances, currentMetric, targetMetric, minInstances, maxInstances);
        // Ensure at least +1 if currently below max
        if (recommended <= currentInstances && currentInstances < maxInstances) {
            recommended = currentInstances + 1;
        }
        return Math.min(maxInstances, recommended);
    }

    public int calculateScaleInCapacity(int currentInstances, double currentMetric, double targetMetric, int minInstances, int maxInstances) {
        int recommended = calculateRecommendedCapacity(currentInstances, currentMetric, targetMetric, minInstances, maxInstances);
        // Ensure at least -1 if currently above min
        if (recommended >= currentInstances && currentInstances > minInstances) {
            recommended = currentInstances - 1;
        }
        return Math.max(minInstances, recommended);
    }
}
