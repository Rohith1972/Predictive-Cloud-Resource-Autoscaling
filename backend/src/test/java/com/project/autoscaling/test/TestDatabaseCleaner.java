package com.project.autoscaling.test;

import com.project.autoscaling.application.repository.ApplicationRepository;
import com.project.autoscaling.infrastructure.repository.InstanceRepository;
import com.project.autoscaling.monitoring.repository.ResourceMetricRepository;
import com.project.autoscaling.scaling.repository.ScalingEventRepository;
import com.project.autoscaling.scaling.repository.ScalingPolicyRepository;
import com.project.autoscaling.scaling.repository.ScalingRecommendationRepository;
import com.project.autoscaling.user.repository.UserRepository;
import com.project.autoscaling.workload.repository.WorkloadMetricRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class TestDatabaseCleaner {

    private final com.project.autoscaling.alerts.repository.AlertRepository alertRepository;
    private final ScalingEventRepository eventRepository;
    private final ScalingRecommendationRepository recommendationRepository;
    private final ScalingPolicyRepository policyRepository;
    private final WorkloadMetricRepository workloadRepository;
    private final ResourceMetricRepository metricRepository;
    private final InstanceRepository instanceRepository;
    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;

    @Transactional
    public void clean() {
        alertRepository.deleteAll();
        eventRepository.deleteAll();
        recommendationRepository.deleteAll();
        policyRepository.deleteAll();
        workloadRepository.deleteAll();
        metricRepository.deleteAll();
        instanceRepository.deleteAll();
        applicationRepository.deleteAll();
        userRepository.deleteAll();
    }
}
