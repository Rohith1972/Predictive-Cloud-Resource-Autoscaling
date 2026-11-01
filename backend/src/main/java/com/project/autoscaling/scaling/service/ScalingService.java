package com.project.autoscaling.scaling.service;

import com.project.autoscaling.common.response.PagedResponse;
import com.project.autoscaling.scaling.dto.*;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface ScalingService {
    ScalingRecommendationResponse evaluateScaling(Long applicationId, ScalingEvaluationRequest request);
    List<ScalingRecommendationResponse> getRecommendationsByApplication(Long applicationId);
    ScalingRecommendationResponse getRecommendationById(Long recommendationId);
    ScalingEventResponse approveRecommendation(Long recommendationId);
    ScalingRecommendationResponse rejectRecommendation(Long recommendationId, String reason);
    PagedResponse<ScalingEventResponse> getScalingHistory(Long applicationId, LocalDateTime start, LocalDateTime end, Pageable pageable);
    ScalingEventResponse getScalingEventById(Long eventId);
    ScalingHistorySummaryResponse getScalingHistorySummary(Long applicationId);
}
