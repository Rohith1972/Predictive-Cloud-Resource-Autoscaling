package com.project.autoscaling.alerts.service;

import com.project.autoscaling.alerts.dto.AlertResponse;
import com.project.autoscaling.alerts.entity.Alert;
import com.project.autoscaling.alerts.repository.AlertRepository;
import com.project.autoscaling.application.entity.Application;
import com.project.autoscaling.common.enums.AlertSeverity;
import com.project.autoscaling.common.enums.AlertStatus;
import com.project.autoscaling.common.enums.AlertType;
import com.project.autoscaling.common.exception.ResourceNotFoundException;
import com.project.autoscaling.common.response.PagedResponse;
import com.project.autoscaling.infrastructure.entity.Instance;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AlertServiceImpl implements AlertService {

    private static final Logger log = LoggerFactory.getLogger(AlertServiceImpl.class);
    private final AlertRepository alertRepository;

    @Override
    @Transactional
    public AlertResponse createAlertIfAbsent(
            Application application,
            Instance instance,
            AlertType alertType,
            AlertSeverity severity,
            String message,
            String metricName,
            double metricValue,
            double thresholdValue
    ) {
        // Deduplication check: check if an ACTIVE alert for this condition already exists
        boolean exists;
        if (instance != null) {
            exists = alertRepository.existsByInstanceIdAndAlertTypeAndStatus(instance.getId(), alertType, AlertStatus.ACTIVE);
        } else {
            exists = alertRepository.existsByApplicationIdAndAlertTypeAndStatus(application.getId(), alertType, AlertStatus.ACTIVE);
        }

        if (exists) {
            log.debug("Active alert already exists for app {} / alertType {}. Skipping duplicate.", application.getId(), alertType);
            return null;
        }

        Alert alert = Alert.builder()
                .application(application)
                .instance(instance)
                .alertType(alertType)
                .severity(severity)
                .message(message)
                .metricName(metricName)
                .metricValue(metricValue)
                .thresholdValue(thresholdValue)
                .status(AlertStatus.ACTIVE)
                .build();

        Alert saved = alertRepository.save(alert);
        log.warn("Generated alert: {} - {}", alertType, message);
        return AlertResponse.fromEntity(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AlertResponse> getActiveAlerts() {
        return alertRepository.findByStatus(AlertStatus.ACTIVE).stream()
                .map(AlertResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<AlertResponse> getAlerts(AlertStatus status, AlertSeverity severity, Pageable pageable) {
        Page<Alert> page;
        if (status != null && severity != null) {
            page = alertRepository.findByStatusAndSeverity(status, severity, pageable);
        } else if (status != null) {
            page = alertRepository.findByStatus(status, pageable);
        } else {
            page = alertRepository.findAll(pageable);
        }
        return PagedResponse.of(page.map(AlertResponse::fromEntity));
    }

    @Override
    @Transactional(readOnly = true)
    public AlertResponse getAlertById(Long id) {
        Alert alert = alertRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Alert", "id", id));
        return AlertResponse.fromEntity(alert);
    }

    @Override
    @Transactional
    public AlertResponse acknowledgeAlert(Long id) {
        Alert alert = alertRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Alert", "id", id));

        alert.setStatus(AlertStatus.ACKNOWLEDGED);
        Alert saved = alertRepository.save(alert);
        return AlertResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public AlertResponse resolveAlert(Long id) {
        Alert alert = alertRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Alert", "id", id));

        alert.setStatus(AlertStatus.RESOLVED);
        alert.setResolvedAt(LocalDateTime.now());
        Alert saved = alertRepository.save(alert);
        return AlertResponse.fromEntity(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public long getActiveAlertCount() {
        return alertRepository.countByStatus(AlertStatus.ACTIVE);
    }
}
