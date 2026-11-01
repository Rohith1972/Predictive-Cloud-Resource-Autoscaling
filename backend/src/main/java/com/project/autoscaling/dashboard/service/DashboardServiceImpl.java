package com.project.autoscaling.dashboard.service;

import com.project.autoscaling.alerts.dto.AlertResponse;
import com.project.autoscaling.alerts.repository.AlertRepository;
import com.project.autoscaling.application.dto.ApplicationResponse;
import com.project.autoscaling.application.entity.Application;
import com.project.autoscaling.application.repository.ApplicationRepository;
import com.project.autoscaling.common.enums.AlertStatus;
import com.project.autoscaling.common.enums.ApplicationStatus;
import com.project.autoscaling.common.enums.InstanceStatus;
import com.project.autoscaling.common.enums.ScalingAction;
import com.project.autoscaling.common.exception.ResourceNotFoundException;
import com.project.autoscaling.dashboard.dto.ApplicationDashboardResponse;
import com.project.autoscaling.dashboard.dto.DashboardOverviewResponse;
import com.project.autoscaling.dashboard.dto.ResourceTrendPoint;
import com.project.autoscaling.dashboard.dto.WorkloadTrendPoint;
import com.project.autoscaling.infrastructure.dto.CapacitySummaryResponse;
import com.project.autoscaling.infrastructure.repository.InstanceRepository;
import com.project.autoscaling.infrastructure.service.InstanceService;
import com.project.autoscaling.monitoring.dto.MonitoringSummaryResponse;
import com.project.autoscaling.monitoring.entity.ResourceMetric;
import com.project.autoscaling.monitoring.repository.ResourceMetricRepository;
import com.project.autoscaling.monitoring.service.MonitoringService;
import com.project.autoscaling.scaling.dto.ScalingHistorySummaryResponse;
import com.project.autoscaling.scaling.dto.ScalingPolicyResponse;
import com.project.autoscaling.scaling.dto.ScalingRecommendationResponse;
import com.project.autoscaling.scaling.entity.ScalingPolicy;
import com.project.autoscaling.scaling.repository.ScalingEventRepository;
import com.project.autoscaling.scaling.repository.ScalingPolicyRepository;
import com.project.autoscaling.scaling.repository.ScalingRecommendationRepository;
import com.project.autoscaling.scaling.service.ScalingService;
import com.project.autoscaling.workload.entity.WorkloadMetric;
import com.project.autoscaling.workload.repository.WorkloadMetricRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final ApplicationRepository applicationRepository;
    private final InstanceRepository instanceRepository;
    private final ResourceMetricRepository metricRepository;
    private final WorkloadMetricRepository workloadRepository;
    private final ScalingPolicyRepository policyRepository;
    private final ScalingRecommendationRepository recommendationRepository;
    private final ScalingEventRepository eventRepository;
    private final AlertRepository alertRepository;
    private final InstanceService instanceService;
    private final MonitoringService monitoringService;
    private final ScalingService scalingService;

    @Override
    @Transactional(readOnly = true)
    public DashboardOverviewResponse getOverview() {
        long totalApps = applicationRepository.count();
        long activeApps = applicationRepository.countByStatus(ApplicationStatus.ACTIVE);
        long totalInstances = instanceRepository.count();
        long runningInstances = instanceRepository.countByStatus(InstanceStatus.RUNNING);

        // Aggregate utilization across all applications for last 1 hour
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        LocalDateTime now = LocalDateTime.now();

        double avgCpu = 0.0;
        double avgMem = 0.0;
        double currentRps = 0.0;
        double avgLatency = 0.0;

        List<Application> activeAppList = applicationRepository.findAll().stream()
                .filter(a -> a.getStatus() == ApplicationStatus.ACTIVE)
                .toList();

        if (!activeAppList.isEmpty()) {
            double totalCpu = 0.0;
            double totalMem = 0.0;
            double totalRps = 0.0;
            double totalLat = 0.0;
            int count = 0;

            for (Application app : activeAppList) {
                ResourceMetric latest = metricRepository.findTopByApplicationIdOrderByTimestampDesc(app.getId()).orElse(null);
                if (latest != null) {
                    totalCpu += latest.getCpuUtilization();
                    totalMem += latest.getMemoryUtilization();
                    totalRps += latest.getRequestRate();
                    totalLat += latest.getAverageResponseTime();
                    count++;
                }
            }

            if (count > 0) {
                avgCpu = Math.round((totalCpu / count) * 10.0) / 10.0;
                avgMem = Math.round((totalMem / count) * 10.0) / 10.0;
                currentRps = Math.round(totalRps * 10.0) / 10.0;
                avgLatency = Math.round((totalLat / count) * 10.0) / 10.0;
            }
        }

        long activeAlerts = alertRepository.countByStatus(AlertStatus.ACTIVE);
        long scaleOuts = eventRepository.countByScalingAction(ScalingAction.SCALE_OUT);
        long scaleIns = eventRepository.countByScalingAction(ScalingAction.SCALE_IN);

        return DashboardOverviewResponse.builder()
                .totalApplications(totalApps)
                .activeApplications(activeApps)
                .totalInstances(totalInstances)
                .runningInstances(runningInstances)
                .averageCpuUtilization(avgCpu)
                .averageMemoryUtilization(avgMem)
                .currentRequestRate(currentRps)
                .averageResponseTime(avgLatency)
                .activeAlerts(activeAlerts)
                .totalScaleOutOperations(scaleOuts)
                .totalScaleInOperations(scaleIns)
                .generatedAt(LocalDateTime.now())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ApplicationDashboardResponse getApplicationDashboard(Long applicationId) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application", "id", applicationId));

        CapacitySummaryResponse capacity = instanceService.getCapacitySummary(applicationId);
        MonitoringSummaryResponse monitoring = monitoringService.getMonitoringSummary(applicationId);

        ScalingPolicy policy = policyRepository.findByApplicationId(applicationId).orElse(null);
        ScalingPolicyResponse policyResp = policy != null ? ScalingPolicyResponse.fromEntity(policy) : null;

        List<ScalingRecommendationResponse> recentRecs = recommendationRepository
                .findByApplicationIdOrderByCreatedAtDesc(applicationId).stream()
                .limit(5)
                .map(ScalingRecommendationResponse::fromEntity)
                .collect(Collectors.toList());

        ScalingHistorySummaryResponse scalingSummary = scalingService.getScalingHistorySummary(applicationId);

        List<AlertResponse> alerts = alertRepository.findByApplicationIdAndStatus(applicationId, AlertStatus.ACTIVE).stream()
                .map(AlertResponse::fromEntity)
                .collect(Collectors.toList());

        return ApplicationDashboardResponse.builder()
                .application(ApplicationResponse.fromEntity(application))
                .capacity(capacity)
                .monitoring(monitoring)
                .policy(policyResp)
                .recentRecommendations(recentRecs)
                .scalingSummary(scalingSummary)
                .activeAlerts(alerts)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResourceTrendPoint> getResourceTrends(Long applicationId, LocalDateTime start, LocalDateTime end) {
        if (!applicationRepository.existsById(applicationId)) {
            throw new ResourceNotFoundException("Application", "id", applicationId);
        }

        LocalDateTime searchStart = start != null ? start : LocalDateTime.now().minusHours(24);
        LocalDateTime searchEnd = end != null ? end : LocalDateTime.now();

        List<ResourceMetric> metrics = metricRepository.findByApplicationIdAndTimestampBetween(
                applicationId, searchStart, searchEnd, PageRequest.of(0, 100, Sort.by(Sort.Direction.ASC, "timestamp"))
        ).getContent();

        return metrics.stream()
                .map(m -> ResourceTrendPoint.builder()
                        .timestamp(m.getTimestamp())
                        .cpuUtilization(m.getCpuUtilization())
                        .memoryUtilization(m.getMemoryUtilization())
                        .requestRate(m.getRequestRate())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkloadTrendPoint> getWorkloadTrends(Long applicationId, LocalDateTime start, LocalDateTime end) {
        if (!applicationRepository.existsById(applicationId)) {
            throw new ResourceNotFoundException("Application", "id", applicationId);
        }

        LocalDateTime searchStart = start != null ? start : LocalDateTime.now().minusHours(24);
        LocalDateTime searchEnd = end != null ? end : LocalDateTime.now();

        List<WorkloadMetric> metrics = workloadRepository.findByApplicationIdAndTimestampBetween(
                applicationId, searchStart, searchEnd, PageRequest.of(0, 100, Sort.by(Sort.Direction.ASC, "timestamp"))
        ).getContent();

        return metrics.stream()
                .map(w -> WorkloadTrendPoint.builder()
                        .timestamp(w.getTimestamp())
                        .requestsPerSecond(w.getRequestsPerSecond())
                        .averageLatency(w.getAverageLatency())
                        .failedRequests(w.getFailedRequests())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public com.project.autoscaling.scaling.dto.ScalingHistorySummaryResponse getScalingSummary(Long applicationId) {
        return scalingService.getScalingHistorySummary(applicationId);
    }

    @Override
    @Transactional(readOnly = true)
    public com.project.autoscaling.infrastructure.dto.CapacitySummaryResponse getInstanceSummary(Long applicationId) {
        return instanceService.getCapacitySummary(applicationId);
    }
}
