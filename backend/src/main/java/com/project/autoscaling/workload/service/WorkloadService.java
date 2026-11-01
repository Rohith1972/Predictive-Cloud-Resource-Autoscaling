package com.project.autoscaling.workload.service;

import com.project.autoscaling.common.response.PagedResponse;
import com.project.autoscaling.workload.dto.WorkloadRecordRequest;
import com.project.autoscaling.workload.dto.WorkloadResponse;
import com.project.autoscaling.workload.dto.WorkloadSummaryResponse;
import com.project.autoscaling.workload.dto.WorkloadTrendResponse;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

public interface WorkloadService {
    WorkloadResponse recordWorkload(WorkloadRecordRequest request);
    WorkloadTrendResponse getWorkloadTrend(Long applicationId);
    WorkloadSummaryResponse getWorkloadSummary(Long applicationId, LocalDateTime start, LocalDateTime end);
    PagedResponse<WorkloadResponse> getWorkloadHistory(Long applicationId, LocalDateTime start, LocalDateTime end, Pageable pageable);
}
