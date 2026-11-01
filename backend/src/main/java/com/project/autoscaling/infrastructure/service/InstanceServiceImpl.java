package com.project.autoscaling.infrastructure.service;

import com.project.autoscaling.application.entity.Application;
import com.project.autoscaling.application.repository.ApplicationRepository;
import com.project.autoscaling.common.enums.InstanceStatus;
import com.project.autoscaling.common.exception.DuplicateResourceException;
import com.project.autoscaling.common.exception.ResourceNotFoundException;
import com.project.autoscaling.common.response.PagedResponse;
import com.project.autoscaling.infrastructure.dto.*;
import com.project.autoscaling.infrastructure.entity.Instance;
import com.project.autoscaling.infrastructure.repository.InstanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InstanceServiceImpl implements InstanceService {

    private final InstanceRepository instanceRepository;
    private final ApplicationRepository applicationRepository;

    @Override
    @Transactional
    public InstanceResponse createInstance(CreateInstanceRequest request) {
        if (instanceRepository.existsByInstanceIdentifier(request.getInstanceIdentifier())) {
            throw new DuplicateResourceException("Instance already exists with identifier: " + request.getInstanceIdentifier());
        }

        Application application = applicationRepository.findById(request.getApplicationId())
                .orElseThrow(() -> new ResourceNotFoundException("Application", "id", request.getApplicationId()));

        Instance instance = Instance.builder()
                .instanceIdentifier(request.getInstanceIdentifier().trim())
                .instanceName(request.getInstanceName().trim())
                .application(application)
                .instanceType(request.getInstanceType().trim())
                .cpuCores(request.getCpuCores())
                .totalMemoryMb(request.getTotalMemoryMb())
                .status(InstanceStatus.RUNNING)
                .provider(request.getProvider())
                .build();

        Instance saved = instanceRepository.save(instance);
        return InstanceResponse.fromEntity(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<InstanceResponse> getAllInstances(InstanceStatus status, Pageable pageable) {
        Page<Instance> page;
        if (status != null) {
            page = instanceRepository.findByStatus(status, pageable);
        } else {
            page = instanceRepository.findAll(pageable);
        }
        return PagedResponse.of(page.map(InstanceResponse::fromEntity));
    }

    @Override
    @Transactional(readOnly = true)
    public InstanceResponse getInstanceById(Long id) {
        Instance instance = findInstanceOrThrow(id);
        return InstanceResponse.fromEntity(instance);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InstanceResponse> getInstancesByApplicationId(Long applicationId) {
        if (!applicationRepository.existsById(applicationId)) {
            throw new ResourceNotFoundException("Application", "id", applicationId);
        }
        return instanceRepository.findByApplicationId(applicationId).stream()
                .map(InstanceResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public InstanceResponse updateInstance(Long id, UpdateInstanceRequest request) {
        Instance instance = findInstanceOrThrow(id);

        if (request.getInstanceName() != null && !request.getInstanceName().isBlank()) {
            instance.setInstanceName(request.getInstanceName().trim());
        }
        if (request.getInstanceType() != null && !request.getInstanceType().isBlank()) {
            instance.setInstanceType(request.getInstanceType().trim());
        }
        if (request.getCpuCores() != null) {
            instance.setCpuCores(request.getCpuCores());
        }
        if (request.getTotalMemoryMb() != null) {
            instance.setTotalMemoryMb(request.getTotalMemoryMb());
        }

        Instance saved = instanceRepository.save(instance);
        return InstanceResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public InstanceResponse updateInstanceStatus(Long id, InstanceStatusUpdateRequest request) {
        Instance instance = findInstanceOrThrow(id);
        instance.setStatus(request.getStatus());
        Instance saved = instanceRepository.save(instance);
        return InstanceResponse.fromEntity(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public CapacitySummaryResponse getCapacitySummary(Long applicationId) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application", "id", applicationId));

        List<Instance> instances = instanceRepository.findByApplicationId(applicationId);

        int total = instances.size();
        int running = 0;
        int stopped = 0;
        int terminated = 0;
        int unhealthy = 0;
        int runningCpuCores = 0;
        long runningMemoryMb = 0;

        for (Instance inst : instances) {
            switch (inst.getStatus()) {
                case RUNNING -> {
                    running++;
                    runningCpuCores += inst.getCpuCores();
                    runningMemoryMb += inst.getTotalMemoryMb();
                }
                case STOPPED -> stopped++;
                case TERMINATED -> terminated++;
                case UNHEALTHY -> unhealthy++;
            }
        }

        return CapacitySummaryResponse.builder()
                .applicationId(application.getId())
                .applicationName(application.getApplicationName())
                .totalInstances(total)
                .runningInstances(running)
                .stoppedInstances(stopped)
                .terminatedInstances(terminated)
                .unhealthyInstances(unhealthy)
                .totalActiveCpuCores(runningCpuCores)
                .totalActiveMemoryMb(runningMemoryMb)
                .build();
    }

    private Instance findInstanceOrThrow(Long id) {
        return instanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Instance", "id", id));
    }
}
