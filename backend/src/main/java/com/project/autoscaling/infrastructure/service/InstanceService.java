package com.project.autoscaling.infrastructure.service;

import com.project.autoscaling.common.enums.InstanceStatus;
import com.project.autoscaling.common.response.PagedResponse;
import com.project.autoscaling.infrastructure.dto.*;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface InstanceService {
    InstanceResponse createInstance(CreateInstanceRequest request);
    PagedResponse<InstanceResponse> getAllInstances(InstanceStatus status, Pageable pageable);
    InstanceResponse getInstanceById(Long id);
    List<InstanceResponse> getInstancesByApplicationId(Long applicationId);
    InstanceResponse updateInstance(Long id, UpdateInstanceRequest request);
    InstanceResponse updateInstanceStatus(Long id, InstanceStatusUpdateRequest request);
    CapacitySummaryResponse getCapacitySummary(Long applicationId);
}
