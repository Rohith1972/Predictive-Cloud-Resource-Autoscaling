package com.project.autoscaling.application.service;

import com.project.autoscaling.application.dto.*;
import com.project.autoscaling.common.enums.ApplicationStatus;
import com.project.autoscaling.common.response.PagedResponse;
import org.springframework.data.domain.Pageable;

public interface ApplicationService {
    ApplicationResponse createApplication(CreateApplicationRequest request);
    PagedResponse<ApplicationResponse> getAllApplications(ApplicationStatus status, Pageable pageable);
    ApplicationResponse getApplicationById(Long id);
    ApplicationResponse updateApplication(Long id, UpdateApplicationRequest request);
    ApplicationResponse updateApplicationStatus(Long id, ApplicationStatusUpdateRequest request);
    void deleteApplication(Long id);
    ApplicationSummaryResponse getApplicationSummary(Long id);
}
