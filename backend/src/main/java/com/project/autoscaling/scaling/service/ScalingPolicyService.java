package com.project.autoscaling.scaling.service;

import com.project.autoscaling.common.response.PagedResponse;
import com.project.autoscaling.scaling.dto.CreateScalingPolicyRequest;
import com.project.autoscaling.scaling.dto.ScalingPolicyResponse;
import com.project.autoscaling.scaling.dto.ScalingPolicyStatusUpdateRequest;
import com.project.autoscaling.scaling.dto.UpdateScalingPolicyRequest;
import org.springframework.data.domain.Pageable;

public interface ScalingPolicyService {
    ScalingPolicyResponse createPolicy(CreateScalingPolicyRequest request);
    PagedResponse<ScalingPolicyResponse> getAllPolicies(Pageable pageable);
    ScalingPolicyResponse getPolicyById(Long id);
    ScalingPolicyResponse getPolicyByApplicationId(Long applicationId);
    ScalingPolicyResponse updatePolicy(Long id, UpdateScalingPolicyRequest request);
    ScalingPolicyResponse updatePolicyStatus(Long id, ScalingPolicyStatusUpdateRequest request);
}
