package com.project.autoscaling.alerts.dto;

import com.project.autoscaling.alerts.entity.Alert;
import com.project.autoscaling.common.enums.AlertSeverity;
import com.project.autoscaling.common.enums.AlertStatus;
import com.project.autoscaling.common.enums.AlertType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertResponse {
    private Long id;
    private Long applicationId;
    private String applicationName;
    private Long instanceId;
    private String instanceIdentifier;
    private AlertType alertType;
    private AlertSeverity severity;
    private String message;
    private String metricName;
    private Double metricValue;
    private Double thresholdValue;
    private AlertStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;

    public static AlertResponse fromEntity(Alert alert) {
        return AlertResponse.builder()
                .id(alert.getId())
                .applicationId(alert.getApplication().getId())
                .applicationName(alert.getApplication().getApplicationName())
                .instanceId(alert.getInstance() != null ? alert.getInstance().getId() : null)
                .instanceIdentifier(alert.getInstance() != null ? alert.getInstance().getInstanceIdentifier() : null)
                .alertType(alert.getAlertType())
                .severity(alert.getSeverity())
                .message(alert.getMessage())
                .metricName(alert.getMetricName())
                .metricValue(alert.getMetricValue())
                .thresholdValue(alert.getThresholdValue())
                .status(alert.getStatus())
                .createdAt(alert.getCreatedAt())
                .resolvedAt(alert.getResolvedAt())
                .build();
    }
}
