package com.project.autoscaling.scaling.service;

import com.project.autoscaling.application.entity.Application;
import com.project.autoscaling.application.repository.ApplicationRepository;
import com.project.autoscaling.common.enums.ExecutionMode;
import com.project.autoscaling.common.enums.InstanceStatus;
import com.project.autoscaling.common.enums.RecommendationStatus;
import com.project.autoscaling.common.enums.ScalingAction;
import com.project.autoscaling.common.enums.ScalingEventStatus;
import com.project.autoscaling.common.exception.InvalidOperationException;
import com.project.autoscaling.common.exception.ResourceNotFoundException;
import com.project.autoscaling.common.response.PagedResponse;
import com.project.autoscaling.infrastructure.provider.InfrastructureProvider;
import com.project.autoscaling.infrastructure.repository.InstanceRepository;
import com.project.autoscaling.monitoring.entity.ResourceMetric;
import com.project.autoscaling.monitoring.repository.ResourceMetricRepository;
import com.project.autoscaling.scaling.dto.*;
import com.project.autoscaling.scaling.engine.ReactiveScalingEngine;
import com.project.autoscaling.scaling.engine.ScalingEvaluationResult;
import com.project.autoscaling.scaling.entity.ScalingEvent;
import com.project.autoscaling.scaling.entity.ScalingPolicy;
import com.project.autoscaling.scaling.entity.ScalingRecommendation;
import com.project.autoscaling.scaling.repository.ScalingEventRepository;
import com.project.autoscaling.scaling.repository.ScalingPolicyRepository;
import com.project.autoscaling.scaling.repository.ScalingRecommendationRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ScalingServiceImpl implements ScalingService {

    private static final Logger log = LoggerFactory.getLogger(ScalingServiceImpl.class);

    private final ApplicationRepository applicationRepository;
    private final ScalingPolicyRepository policyRepository;
    private final ResourceMetricRepository metricRepository;
    private final InstanceRepository instanceRepository;
    private final InfrastructureProvider infrastructureProvider;
    private final ReactiveScalingEngine scalingEngine;
    private final ScalingRecommendationRepository recommendationRepository;
    private final ScalingEventRepository eventRepository;

    @Override
    @Transactional
    public ScalingRecommendationResponse evaluateScaling(Long applicationId, ScalingEvaluationRequest request) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application", "id", applicationId));

        ScalingPolicy policy = policyRepository.findByApplicationId(applicationId)
                .orElseThrow(() -> new InvalidOperationException("No scaling policy configured for application: " + application.getApplicationName()));

        int currentRunning = (int) instanceRepository.countByApplicationIdAndStatus(applicationId, InstanceStatus.RUNNING);
        ResourceMetric latestMetric = metricRepository.findTopByApplicationIdOrderByTimestampDesc(applicationId).orElse(null);
        ScalingEvent lastEvent = eventRepository.findTopByApplicationIdOrderByCreatedAtDesc(applicationId).orElse(null);

        ScalingEvaluationResult evalResult = scalingEngine.evaluate(policy, latestMetric, currentRunning, lastEvent);

        ExecutionMode mode = (request != null && request.getExecutionMode() != null)
                ? request.getExecutionMode()
                : ExecutionMode.MANUAL_APPROVAL;

        RecommendationStatus initialStatus = evalResult.getAction() == ScalingAction.NO_ACTION
                ? RecommendationStatus.EXPIRED
                : RecommendationStatus.PENDING;

        ScalingRecommendation recommendation = ScalingRecommendation.builder()
                .application(application)
                .currentInstanceCount(evalResult.getCurrentInstanceCount())
                .recommendedInstanceCount(evalResult.getRecommendedInstanceCount())
                .scalingAction(evalResult.getAction())
                .triggeringMetric(evalResult.getTriggeringMetric())
                .observedMetricValue(evalResult.getObservedMetricValue())
                .configuredThreshold(evalResult.getConfiguredThreshold())
                .decisionReason(evalResult.getDecisionReason())
                .executionMode(mode)
                .status(initialStatus)
                .evaluatedAt(LocalDateTime.now())
                .build();

        ScalingRecommendation savedRecommendation = recommendationRepository.save(recommendation);

        // Record evaluation event in audit log
        ScalingEvent event = ScalingEvent.builder()
                .application(application)
                .recommendation(savedRecommendation)
                .previousInstanceCount(currentRunning)
                .newInstanceCount(evalResult.getRecommendedInstanceCount())
                .scalingAction(evalResult.getAction())
                .triggeringMetric(evalResult.getTriggeringMetric())
                .triggeringMetricValue(evalResult.getObservedMetricValue())
                .decisionReason(evalResult.getDecisionReason())
                .executionMode(mode)
                .status(evalResult.getAction() == ScalingAction.NO_ACTION ? ScalingEventStatus.COMPLETED : ScalingEventStatus.PENDING)
                .completedAt(evalResult.getAction() == ScalingAction.NO_ACTION ? LocalDateTime.now() : null)
                .build();

        eventRepository.save(event);

        return ScalingRecommendationResponse.fromEntity(savedRecommendation);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ScalingRecommendationResponse> getRecommendationsByApplication(Long applicationId) {
        if (!applicationRepository.existsById(applicationId)) {
            throw new ResourceNotFoundException("Application", "id", applicationId);
        }
        return recommendationRepository.findByApplicationIdOrderByCreatedAtDesc(applicationId).stream()
                .map(ScalingRecommendationResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ScalingRecommendationResponse getRecommendationById(Long recommendationId) {
        ScalingRecommendation r = findRecommendationOrThrow(recommendationId);
        return ScalingRecommendationResponse.fromEntity(r);
    }

    @Override
    @Transactional
    public ScalingEventResponse approveRecommendation(Long recommendationId) {
        ScalingRecommendation recommendation = findRecommendationOrThrow(recommendationId);

        if (recommendation.getStatus() != RecommendationStatus.PENDING) {
            throw new InvalidOperationException(String.format(
                    "Recommendation ID %d cannot be approved because its status is %s",
                    recommendationId, recommendation.getStatus()));
        }

        Application application = recommendation.getApplication();
        int currentRunning = (int) instanceRepository.countByApplicationIdAndStatus(application.getId(), InstanceStatus.RUNNING);
        int targetCapacity = recommendation.getRecommendedInstanceCount();

        log.info("Approving scaling recommendation {}: adjusting capacity from {} to {}", recommendationId, currentRunning, targetCapacity);

        // Execute capacity change via provider
        boolean capacityUpdated = infrastructureProvider.updateDesiredCapacity(application.getId(), targetCapacity);

        recommendation.setStatus(RecommendationStatus.APPROVED);
        recommendationRepository.save(recommendation);

        ScalingEvent event = ScalingEvent.builder()
                .application(application)
                .recommendation(recommendation)
                .previousInstanceCount(currentRunning)
                .newInstanceCount(targetCapacity)
                .scalingAction(recommendation.getScalingAction())
                .triggeringMetric(recommendation.getTriggeringMetric())
                .triggeringMetricValue(recommendation.getObservedMetricValue())
                .decisionReason("Recommendation approved by administrator. Capacity updated to " + targetCapacity)
                .executionMode(ExecutionMode.MANUAL_APPROVAL)
                .status(capacityUpdated ? ScalingEventStatus.COMPLETED : ScalingEventStatus.FAILED)
                .completedAt(LocalDateTime.now())
                .build();

        ScalingEvent savedEvent = eventRepository.save(event);
        return ScalingEventResponse.fromEntity(savedEvent);
    }

    @Override
    @Transactional
    public ScalingRecommendationResponse rejectRecommendation(Long recommendationId, String reason) {
        ScalingRecommendation recommendation = findRecommendationOrThrow(recommendationId);

        if (recommendation.getStatus() != RecommendationStatus.PENDING) {
            throw new InvalidOperationException(String.format(
                    "Recommendation ID %d cannot be rejected because its status is %s",
                    recommendationId, recommendation.getStatus()));
        }

        recommendation.setStatus(RecommendationStatus.REJECTED);
        ScalingRecommendation saved = recommendationRepository.save(recommendation);

        ScalingEvent rejectionEvent = ScalingEvent.builder()
                .application(recommendation.getApplication())
                .recommendation(recommendation)
                .previousInstanceCount(recommendation.getCurrentInstanceCount())
                .newInstanceCount(recommendation.getCurrentInstanceCount())
                .scalingAction(ScalingAction.NO_ACTION)
                .triggeringMetric(recommendation.getTriggeringMetric())
                .triggeringMetricValue(recommendation.getObservedMetricValue())
                .decisionReason("Recommendation rejected by administrator" + (reason != null && !reason.isBlank() ? ": " + reason : ""))
                .executionMode(ExecutionMode.MANUAL_APPROVAL)
                .status(ScalingEventStatus.COMPLETED)
                .completedAt(LocalDateTime.now())
                .build();

        eventRepository.save(rejectionEvent);

        return ScalingRecommendationResponse.fromEntity(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<ScalingEventResponse> getScalingHistory(Long applicationId, LocalDateTime start, LocalDateTime end, Pageable pageable) {
        if (!applicationRepository.existsById(applicationId)) {
            throw new ResourceNotFoundException("Application", "id", applicationId);
        }

        Page<ScalingEvent> page;
        if (start != null && end != null) {
            page = eventRepository.findByApplicationIdAndCreatedAtBetween(applicationId, start, end, pageable);
        } else {
            page = eventRepository.findByApplicationId(applicationId, pageable);
        }

        return PagedResponse.of(page.map(ScalingEventResponse::fromEntity));
    }

    @Override
    @Transactional(readOnly = true)
    public ScalingEventResponse getScalingEventById(Long eventId) {
        ScalingEvent event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("ScalingEvent", "id", eventId));
        return ScalingEventResponse.fromEntity(event);
    }

    @Override
    @Transactional(readOnly = true)
    public ScalingHistorySummaryResponse getScalingHistorySummary(Long applicationId) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application", "id", applicationId));

        long scaleOuts = eventRepository.countByApplicationIdAndScalingAction(applicationId, ScalingAction.SCALE_OUT);
        long scaleIns = eventRepository.countByApplicationIdAndScalingAction(applicationId, ScalingAction.SCALE_IN);
        long noActions = eventRepository.countByApplicationIdAndScalingAction(applicationId, ScalingAction.NO_ACTION);

        LocalDateTime lastScaled = eventRepository.findTopByApplicationIdOrderByCreatedAtDesc(applicationId)
                .map(ScalingEvent::getCreatedAt)
                .orElse(null);

        return ScalingHistorySummaryResponse.builder()
                .applicationId(applicationId)
                .applicationName(application.getApplicationName())
                .totalEvents(scaleOuts + scaleIns + noActions)
                .scaleOutEvents(scaleOuts)
                .scaleInEvents(scaleIns)
                .noActionEvaluations(noActions)
                .lastScaledAt(lastScaled)
                .build();
    }

    private ScalingRecommendation findRecommendationOrThrow(Long id) {
        return recommendationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ScalingRecommendation", "id", id));
    }
}
