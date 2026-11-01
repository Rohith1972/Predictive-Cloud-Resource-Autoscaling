package com.project.autoscaling.monitoring.collector;

import com.project.autoscaling.monitoring.dto.MetricIngestionRequest;
import com.project.autoscaling.monitoring.dto.MetricResponse;

public interface TelemetryCollector {
    MetricResponse collect(MetricIngestionRequest request);
}
