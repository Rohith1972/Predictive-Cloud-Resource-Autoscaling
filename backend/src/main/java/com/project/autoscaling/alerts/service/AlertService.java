package com.project.autoscaling.alerts.service;

import com.project.autoscaling.alerts.dto.AlertResponse;
import com.project.autoscaling.application.entity.Application;
import com.project.autoscaling.common.enums.AlertSeverity;
import com.project.autoscaling.common.enums.AlertStatus;
import com.project.autoscaling.common.enums.AlertType;
import com.project.autoscaling.common.response.PagedResponse;
import com.project.autoscaling.infrastructure.entity.Instance;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface AlertService {
    AlertResponse createAlertIfAbsent(
            Application application,
            Instance instance,
            AlertType alertType,
            AlertSeverity severity,
            String message,
            String metricName,
            double metricValue,
            double thresholdValue
    );
    List<AlertResponse> getActiveAlerts();
    PagedResponse<AlertResponse> getAlerts(AlertStatus status, AlertSeverity severity, Pageable pageable);
    AlertResponse getAlertById(Long id);
    AlertResponse acknowledgeAlert(Long id);
    AlertResponse resolveAlert(Long id);
    long getActiveAlertCount();
}
