package com.project.autoscaling.monitoring.service;

import com.project.autoscaling.application.entity.Application;
import com.project.autoscaling.application.repository.ApplicationRepository;
import com.project.autoscaling.common.enums.InstanceStatus;
import com.project.autoscaling.common.exception.InvalidOperationException;
import com.project.autoscaling.common.exception.ResourceNotFoundException;
import com.project.autoscaling.common.response.PagedResponse;
import com.project.autoscaling.infrastructure.entity.Instance;
import com.project.autoscaling.infrastructure.repository.InstanceRepository;
import com.project.autoscaling.monitoring.collector.TelemetryCollector;
import com.project.autoscaling.monitoring.dto.AggregateMetricsResponse;
import com.project.autoscaling.monitoring.dto.MetricIngestionRequest;
import com.project.autoscaling.monitoring.dto.MetricResponse;
import com.project.autoscaling.monitoring.dto.MonitoringSummaryResponse;
import com.project.autoscaling.monitoring.entity.ResourceMetric;
import com.project.autoscaling.monitoring.repository.ResourceMetricRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MonitoringServiceImpl implements MonitoringService, TelemetryCollector {

    private final ResourceMetricRepository metricRepository;
    private final ApplicationRepository applicationRepository;
    private final InstanceRepository instanceRepository;

    @Override
    @Transactional
    public MetricResponse collect(MetricIngestionRequest request) {
        return recordMetric(request);
    }

    @Override
    @Transactional
    public MetricResponse recordMetric(MetricIngestionRequest request) {
        Application application = applicationRepository.findById(request.getApplicationId())
                .orElseThrow(() -> new ResourceNotFoundException("Application", "id", request.getApplicationId()));

        Instance instance = instanceRepository.findById(request.getInstanceId())
                .orElseThrow(() -> new ResourceNotFoundException("Instance", "id", request.getInstanceId()));

        if (!instance.getApplication().getId().equals(application.getId())) {
            throw new InvalidOperationException(String.format(
                    "Instance ID %d belongs to Application ID %d, not to specified Application ID %d",
                    instance.getId(), instance.getApplication().getId(), application.getId()));
        }

        LocalDateTime timestamp = request.getTimestamp() != null ? request.getTimestamp() : LocalDateTime.now();

        ResourceMetric metric = ResourceMetric.builder()
                .application(application)
                .instance(instance)
                .timestamp(timestamp)
                .cpuUtilization(request.getCpuUtilization())
                .memoryUtilization(request.getMemoryUtilization())
                .requestRate(request.getRequestRate())
                .averageResponseTime(request.getAverageResponseTime())
                .errorRate(request.getErrorRate())
                .activeRequests(request.getActiveRequests() != null ? request.getActiveRequests() : 0)
                .networkInBytes(request.getNetworkInBytes() != null ? request.getNetworkInBytes() : 0L)
                .networkOutBytes(request.getNetworkOutBytes() != null ? request.getNetworkOutBytes() : 0L)
                .build();

        ResourceMetric saved = metricRepository.save(metric);
        return MetricResponse.fromEntity(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public MetricResponse getLatestMetric(Long applicationId) {
        if (!applicationRepository.existsById(applicationId)) {
            throw new ResourceNotFoundException("Application", "id", applicationId);
        }

        ResourceMetric latest = metricRepository.findTopByApplicationIdOrderByTimestampDesc(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("No metrics recorded yet for application id: " + applicationId));

        return MetricResponse.fromEntity(latest);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<MetricResponse> getMetricsByInstance(Long instanceId, Pageable pageable) {
        if (!instanceRepository.existsById(instanceId)) {
            throw new ResourceNotFoundException("Instance", "id", instanceId);
        }

        Page<ResourceMetric> page = metricRepository.findByInstanceId(instanceId, pageable);
        return PagedResponse.of(page.map(MetricResponse::fromEntity));
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<MetricResponse> getHistoricalMetrics(Long applicationId, LocalDateTime start, LocalDateTime end, Pageable pageable) {
        if (!applicationRepository.existsById(applicationId)) {
            throw new ResourceNotFoundException("Application", "id", applicationId);
        }

        Page<ResourceMetric> page;
        if (start != null && end != null) {
            page = metricRepository.findByApplicationIdAndTimestampBetween(applicationId, start, end, pageable);
        } else {
            page = metricRepository.findByApplicationId(applicationId, pageable);
        }

        return PagedResponse.of(page.map(MetricResponse::fromEntity));
    }

    @Override
    @Transactional(readOnly = true)
    public AggregateMetricsResponse getAggregateMetrics(Long applicationId, LocalDateTime start, LocalDateTime end) {
        if (!applicationRepository.existsById(applicationId)) {
            throw new ResourceNotFoundException("Application", "id", applicationId);
        }

        LocalDateTime searchStart = start != null ? start : LocalDateTime.now().minusHours(24);
        LocalDateTime searchEnd = end != null ? end : LocalDateTime.now();

        List<Object[]> rows = metricRepository.calculateAggregates(applicationId, searchStart, searchEnd);

        if (rows.isEmpty() || rows.get(0)[9] == null || ((Number) rows.get(0)[9]).longValue() == 0L) {
            return AggregateMetricsResponse.builder()
                    .applicationId(applicationId)
                    .startTime(searchStart)
                    .endTime(searchEnd)
                    .totalDataPoints(0)
                    .build();
        }

        Object[] result = rows.get(0);
        return AggregateMetricsResponse.builder()
                .applicationId(applicationId)
                .startTime(searchStart)
                .endTime(searchEnd)
                .averageCpuUtilization(((Number) result[0]).doubleValue())
                .minCpuUtilization(((Number) result[1]).doubleValue())
                .maxCpuUtilization(((Number) result[2]).doubleValue())
                .averageMemoryUtilization(((Number) result[3]).doubleValue())
                .minMemoryUtilization(((Number) result[4]).doubleValue())
                .maxMemoryUtilization(((Number) result[5]).doubleValue())
                .averageRequestRate(((Number) result[6]).doubleValue())
                .averageResponseTime(((Number) result[7]).doubleValue())
                .averageErrorRate(((Number) result[8]).doubleValue())
                .totalDataPoints(((Number) result[9]).longValue())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public MonitoringSummaryResponse getMonitoringSummary(Long applicationId) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application", "id", applicationId));

        int activeInstances = (int) instanceRepository.countByApplicationIdAndStatus(applicationId, InstanceStatus.RUNNING);
        int unhealthyInstances = (int) instanceRepository.countByApplicationIdAndStatus(applicationId, InstanceStatus.UNHEALTHY);

        ResourceMetric latest = metricRepository.findTopByApplicationIdOrderByTimestampDesc(applicationId)
                .orElse(null);

        double cpu = latest != null ? latest.getCpuUtilization() : 0.0;
        double mem = latest != null ? latest.getMemoryUtilization() : 0.0;
        double rps = latest != null ? latest.getRequestRate() : 0.0;
        double latency = latest != null ? latest.getAverageResponseTime() : 0.0;
        double errorRate = latest != null ? latest.getErrorRate() : 0.0;
        LocalDateTime ts = latest != null ? latest.getTimestamp() : LocalDateTime.now();

        boolean isOverloaded = cpu > 80.0 || mem > 85.0 || errorRate > 5.0;

        return MonitoringSummaryResponse.builder()
                .applicationId(application.getId())
                .applicationName(application.getApplicationName())
                .activeInstancesCount(activeInstances)
                .currentCpuUtilization(cpu)
                .currentMemoryUtilization(mem)
                .currentRequestRate(rps)
                .currentResponseTime(latency)
                .currentErrorRate(errorRate)
                .isOverloaded(isOverloaded)
                .unhealthyInstancesCount(unhealthyInstances)
                .timestamp(ts)
                .build();
    }
}
