package com.project.autoscaling.monitoring.service;

import com.project.autoscaling.common.response.PagedResponse;
import com.project.autoscaling.monitoring.dto.AggregateMetricsResponse;
import com.project.autoscaling.monitoring.dto.MetricIngestionRequest;
import com.project.autoscaling.monitoring.dto.MetricResponse;
import com.project.autoscaling.monitoring.dto.MonitoringSummaryResponse;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

public interface MonitoringService {
    MetricResponse recordMetric(MetricIngestionRequest request);
    MetricResponse getLatestMetric(Long applicationId);
    PagedResponse<MetricResponse> getMetricsByInstance(Long instanceId, Pageable pageable);
    PagedResponse<MetricResponse> getHistoricalMetrics(Long applicationId, LocalDateTime start, LocalDateTime end, Pageable pageable);
    AggregateMetricsResponse getAggregateMetrics(Long applicationId, LocalDateTime start, LocalDateTime end);
    MonitoringSummaryResponse getMonitoringSummary(Long applicationId);
}
