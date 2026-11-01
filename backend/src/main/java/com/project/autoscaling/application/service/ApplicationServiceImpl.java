package com.project.autoscaling.application.service;

import com.project.autoscaling.application.dto.*;
import com.project.autoscaling.application.entity.Application;
import com.project.autoscaling.application.repository.ApplicationRepository;
import com.project.autoscaling.common.enums.ApplicationStatus;
import com.project.autoscaling.common.enums.InstanceStatus;
import com.project.autoscaling.common.exception.DuplicateResourceException;
import com.project.autoscaling.common.exception.InvalidOperationException;
import com.project.autoscaling.common.exception.ResourceNotFoundException;
import com.project.autoscaling.common.response.PagedResponse;
import com.project.autoscaling.infrastructure.repository.InstanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ApplicationServiceImpl implements ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final InstanceRepository instanceRepository;

    @Override
    @Transactional
    public ApplicationResponse createApplication(CreateApplicationRequest request) {
        if (applicationRepository.existsByApplicationName(request.getApplicationName())) {
            throw new DuplicateResourceException("Application already exists with name: " + request.getApplicationName());
        }

        Application application = Application.builder()
                .applicationName(request.getApplicationName().trim())
                .description(request.getDescription())
                .applicationUrl(request.getApplicationUrl().trim())
                .environment(request.getEnvironment())
                .status(ApplicationStatus.ACTIVE)
                .build();

        Application saved = applicationRepository.save(application);
        return ApplicationResponse.fromEntity(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<ApplicationResponse> getAllApplications(ApplicationStatus status, Pageable pageable) {
        Page<Application> page;
        if (status != null) {
            page = applicationRepository.findByStatus(status, pageable);
        } else {
            page = applicationRepository.findAll(pageable);
        }
        return PagedResponse.of(page.map(ApplicationResponse::fromEntity));
    }

    @Override
    @Transactional(readOnly = true)
    public ApplicationResponse getApplicationById(Long id) {
        Application application = findAppOrThrow(id);
        return ApplicationResponse.fromEntity(application);
    }

    @Override
    @Transactional
    public ApplicationResponse updateApplication(Long id, UpdateApplicationRequest request) {
        Application application = findAppOrThrow(id);

        if (request.getApplicationName() != null && !request.getApplicationName().isBlank()) {
            String newName = request.getApplicationName().trim();
            if (!newName.equalsIgnoreCase(application.getApplicationName())
                    && applicationRepository.existsByApplicationName(newName)) {
                throw new DuplicateResourceException("Application name already in use: " + newName);
            }
            application.setApplicationName(newName);
        }

        if (request.getDescription() != null) {
            application.setDescription(request.getDescription());
        }

        if (request.getApplicationUrl() != null && !request.getApplicationUrl().isBlank()) {
            application.setApplicationUrl(request.getApplicationUrl().trim());
        }

        if (request.getEnvironment() != null) {
            application.setEnvironment(request.getEnvironment());
        }

        Application updated = applicationRepository.save(application);
        return ApplicationResponse.fromEntity(updated);
    }

    @Override
    @Transactional
    public ApplicationResponse updateApplicationStatus(Long id, ApplicationStatusUpdateRequest request) {
        Application application = findAppOrThrow(id);
        application.setStatus(request.getStatus());
        Application updated = applicationRepository.save(application);
        return ApplicationResponse.fromEntity(updated);
    }

    @Override
    @Transactional
    public void deleteApplication(Long id) {
        Application application = findAppOrThrow(id);

        long instanceCount = instanceRepository.countByApplicationId(id);
        if (instanceCount > 0) {
            throw new InvalidOperationException(String.format(
                    "Cannot delete application '%s' because %d infrastructure instance(s) are associated with it. " +
                            "Please terminate or remove all instances first, or deactivate the application status instead.",
                    application.getApplicationName(), instanceCount));
        }

        applicationRepository.delete(application);
    }

    @Override
    @Transactional(readOnly = true)
    public ApplicationSummaryResponse getApplicationSummary(Long id) {
        Application application = findAppOrThrow(id);
        int totalInstances = (int) instanceRepository.countByApplicationId(id);
        int runningInstances = (int) instanceRepository.countByApplicationIdAndStatus(id, InstanceStatus.RUNNING);

        return ApplicationSummaryResponse.builder()
                .applicationId(application.getId())
                .applicationName(application.getApplicationName())
                .environment(application.getEnvironment())
                .status(application.getStatus())
                .totalInstances(totalInstances)
                .runningInstances(runningInstances)
                .currentCpuUtilization(0.0)
                .currentMemoryUtilization(0.0)
                .currentRequestRate(0.0)
                .activeAlertsCount(0)
                .lastUpdated(application.getUpdatedAt())
                .build();
    }

    private Application findAppOrThrow(Long id) {
        return applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application", "id", id));
    }
}
