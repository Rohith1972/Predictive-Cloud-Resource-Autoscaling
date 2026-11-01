package com.project.autoscaling.alerts.repository;

import com.project.autoscaling.alerts.entity.Alert;
import com.project.autoscaling.common.enums.AlertSeverity;
import com.project.autoscaling.common.enums.AlertStatus;
import com.project.autoscaling.common.enums.AlertType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlertRepository extends JpaRepository<Alert, Long> {

    List<Alert> findByStatus(AlertStatus status);

    Page<Alert> findByStatus(AlertStatus status, Pageable pageable);

    List<Alert> findByApplicationIdAndStatus(Long applicationId, AlertStatus status);

    long countByStatus(AlertStatus status);

    long countByApplicationIdAndStatus(Long applicationId, AlertStatus status);

    boolean existsByApplicationIdAndAlertTypeAndStatus(Long applicationId, AlertType alertType, AlertStatus status);

    boolean existsByInstanceIdAndAlertTypeAndStatus(Long instanceId, AlertType alertType, AlertStatus status);

    Page<Alert> findByStatusAndSeverity(AlertStatus status, AlertSeverity severity, Pageable pageable);

    Page<Alert> findByApplicationId(Long applicationId, Pageable pageable);
}
