package com.project.autoscaling.scaling.service;

import com.project.autoscaling.application.entity.Application;
import com.project.autoscaling.application.repository.ApplicationRepository;
import com.project.autoscaling.common.exception.DuplicateResourceException;
import com.project.autoscaling.common.exception.ResourceNotFoundException;
import com.project.autoscaling.common.exception.ScalingPolicyViolationException;
import com.project.autoscaling.common.response.PagedResponse;
import com.project.autoscaling.scaling.dto.CreateScalingPolicyRequest;
import com.project.autoscaling.scaling.dto.ScalingPolicyResponse;
import com.project.autoscaling.scaling.dto.ScalingPolicyStatusUpdateRequest;
import com.project.autoscaling.scaling.dto.UpdateScalingPolicyRequest;
import com.project.autoscaling.scaling.entity.ScalingPolicy;
import com.project.autoscaling.scaling.repository.ScalingPolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ScalingPolicyServiceImpl implements ScalingPolicyService {

    private final ScalingPolicyRepository policyRepository;
    private final ApplicationRepository applicationRepository;

    @Override
    @Transactional
    public ScalingPolicyResponse createPolicy(CreateScalingPolicyRequest request) {
        if (policyRepository.existsByApplicationId(request.getApplicationId())) {
            throw new DuplicateResourceException("Scaling policy already exists for application ID: " + request.getApplicationId());
        }

        Application application = applicationRepository.findById(request.getApplicationId())
                .orElseThrow(() -> new ResourceNotFoundException("Application", "id", request.getApplicationId()));

        validateRules(
                request.getMinimumInstances(),
                request.getMaximumInstances(),
                request.getDesiredInstances(),
                request.getCpuScaleInThreshold(),
                request.getCpuScaleOutThreshold(),
                request.getMemoryScaleInThreshold(),
                request.getMemoryScaleOutThreshold(),
                request.getCooldownSeconds()
        );

        ScalingPolicy policy = ScalingPolicy.builder()
                .application(application)
                .minimumInstances(request.getMinimumInstances())
                .maximumInstances(request.getMaximumInstances())
                .desiredInstances(request.getDesiredInstances())
                .cpuScaleOutThreshold(request.getCpuScaleOutThreshold())
                .cpuScaleInThreshold(request.getCpuScaleInThreshold())
                .memoryScaleOutThreshold(request.getMemoryScaleOutThreshold())
                .memoryScaleInThreshold(request.getMemoryScaleInThreshold())
                .requestRateThreshold(request.getRequestRateThreshold())
                .cooldownSeconds(request.getCooldownSeconds())
                .enabled(request.getEnabled() != null ? request.getEnabled() : true)
                .build();

        ScalingPolicy saved = policyRepository.save(policy);
        return ScalingPolicyResponse.fromEntity(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<ScalingPolicyResponse> getAllPolicies(Pageable pageable) {
        Page<ScalingPolicy> page = policyRepository.findAll(pageable);
        return PagedResponse.of(page.map(ScalingPolicyResponse::fromEntity));
    }

    @Override
    @Transactional(readOnly = true)
    public ScalingPolicyResponse getPolicyById(Long id) {
        ScalingPolicy policy = findPolicyOrThrow(id);
        return ScalingPolicyResponse.fromEntity(policy);
    }

    @Override
    @Transactional(readOnly = true)
    public ScalingPolicyResponse getPolicyByApplicationId(Long applicationId) {
        if (!applicationRepository.existsById(applicationId)) {
            throw new ResourceNotFoundException("Application", "id", applicationId);
        }

        ScalingPolicy policy = policyRepository.findByApplicationId(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("No scaling policy found for application id: " + applicationId));

        return ScalingPolicyResponse.fromEntity(policy);
    }

    @Override
    @Transactional
    public ScalingPolicyResponse updatePolicy(Long id, UpdateScalingPolicyRequest request) {
        ScalingPolicy policy = findPolicyOrThrow(id);

        int min = request.getMinimumInstances() != null ? request.getMinimumInstances() : policy.getMinimumInstances();
        int max = request.getMaximumInstances() != null ? request.getMaximumInstances() : policy.getMaximumInstances();
        int desired = request.getDesiredInstances() != null ? request.getDesiredInstances() : policy.getDesiredInstances();
        double cpuIn = request.getCpuScaleInThreshold() != null ? request.getCpuScaleInThreshold() : policy.getCpuScaleInThreshold();
        double cpuOut = request.getCpuScaleOutThreshold() != null ? request.getCpuScaleOutThreshold() : policy.getCpuScaleOutThreshold();
        double memIn = request.getMemoryScaleInThreshold() != null ? request.getMemoryScaleInThreshold() : policy.getMemoryScaleInThreshold();
        double memOut = request.getMemoryScaleOutThreshold() != null ? request.getMemoryScaleOutThreshold() : policy.getMemoryScaleOutThreshold();
        int cooldown = request.getCooldownSeconds() != null ? request.getCooldownSeconds() : policy.getCooldownSeconds();

        validateRules(min, max, desired, cpuIn, cpuOut, memIn, memOut, cooldown);

        policy.setMinimumInstances(min);
        policy.setMaximumInstances(max);
        policy.setDesiredInstances(desired);
        policy.setCpuScaleInThreshold(cpuIn);
        policy.setCpuScaleOutThreshold(cpuOut);
        policy.setMemoryScaleInThreshold(memIn);
        policy.setMemoryScaleOutThreshold(memOut);
        policy.setCooldownSeconds(cooldown);

        if (request.getRequestRateThreshold() != null) {
            policy.setRequestRateThreshold(request.getRequestRateThreshold());
        }
        if (request.getEnabled() != null) {
            policy.setEnabled(request.getEnabled());
        }

        ScalingPolicy saved = policyRepository.save(policy);
        return ScalingPolicyResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public ScalingPolicyResponse updatePolicyStatus(Long id, ScalingPolicyStatusUpdateRequest request) {
        ScalingPolicy policy = findPolicyOrThrow(id);
        policy.setEnabled(request.getEnabled());
        ScalingPolicy saved = policyRepository.save(policy);
        return ScalingPolicyResponse.fromEntity(saved);
    }

    private void validateRules(int min, int max, int desired, double cpuIn, double cpuOut, double memIn, double memOut, int cooldown) {
        if (min < 1) {
            throw new ScalingPolicyViolationException("minimumInstances must be at least 1");
        }
        if (max < min) {
            throw new ScalingPolicyViolationException(String.format("maximumInstances (%d) cannot be less than minimumInstances (%d)", max, min));
        }
        if (desired < min || desired > max) {
            throw new ScalingPolicyViolationException(String.format("desiredInstances (%d) must be within range [%d, %d]", desired, min, max));
        }
        if (cpuIn >= cpuOut) {
            throw new ScalingPolicyViolationException(String.format("cpuScaleInThreshold (%.1f%%) must be strictly lower than cpuScaleOutThreshold (%.1f%%)", cpuIn, cpuOut));
        }
        if (memIn >= memOut) {
            throw new ScalingPolicyViolationException(String.format("memoryScaleInThreshold (%.1f%%) must be strictly lower than memoryScaleOutThreshold (%.1f%%)", memIn, memOut));
        }
        if (cooldown < 0) {
            throw new ScalingPolicyViolationException("cooldownSeconds cannot be negative");
        }
    }

    private ScalingPolicy findPolicyOrThrow(Long id) {
        return policyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ScalingPolicy", "id", id));
    }
}
