-- Flyway Database Migration: Initial Schema
-- V1__init_schema.sql

-- 1. USERS TABLE
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    full_name VARCHAR(150) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL DEFAULT 'VIEWER',
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_users_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. APPLICATIONS TABLE
CREATE TABLE IF NOT EXISTS applications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    application_name VARCHAR(150) NOT NULL UNIQUE,
    description TEXT,
    application_url VARCHAR(255) NOT NULL,
    environment VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_applications_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. INFRASTRUCTURE INSTANCES TABLE
CREATE TABLE IF NOT EXISTS infrastructure_instances (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    instance_identifier VARCHAR(100) NOT NULL UNIQUE,
    instance_name VARCHAR(150) NOT NULL,
    application_id BIGINT NOT NULL,
    instance_type VARCHAR(50) NOT NULL,
    cpu_cores INT NOT NULL,
    total_memory_mb BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'RUNNING',
    provider VARCHAR(50) NOT NULL DEFAULT 'LOCAL_SIMULATED',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_infra_instance_app FOREIGN KEY (application_id) REFERENCES applications (id) ON DELETE RESTRICT,
    INDEX idx_infra_instance_app_status (application_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. RESOURCE METRICS TABLE
CREATE TABLE IF NOT EXISTS resource_metrics (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    application_id BIGINT NOT NULL,
    instance_id BIGINT NOT NULL,
    timestamp TIMESTAMP NOT NULL,
    cpu_utilization DOUBLE NOT NULL,
    memory_utilization DOUBLE NOT NULL,
    request_rate DOUBLE NOT NULL,
    average_response_time DOUBLE NOT NULL,
    error_rate DOUBLE NOT NULL,
    active_requests INT NOT NULL DEFAULT 0,
    network_in_bytes BIGINT NOT NULL DEFAULT 0,
    network_out_bytes BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_resource_metrics_app FOREIGN KEY (application_id) REFERENCES applications (id) ON DELETE CASCADE,
    CONSTRAINT fk_resource_metrics_instance FOREIGN KEY (instance_id) REFERENCES infrastructure_instances (id) ON DELETE CASCADE,
    INDEX idx_resource_metrics_app_ts (application_id, timestamp),
    INDEX idx_resource_metrics_instance_ts (instance_id, timestamp)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. WORKLOAD METRICS TABLE
CREATE TABLE IF NOT EXISTS workload_metrics (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    application_id BIGINT NOT NULL,
    timestamp TIMESTAMP NOT NULL,
    request_count BIGINT NOT NULL,
    requests_per_second DOUBLE NOT NULL,
    successful_requests BIGINT NOT NULL,
    failed_requests BIGINT NOT NULL,
    average_latency DOUBLE NOT NULL,
    p95_latency DOUBLE NOT NULL,
    p99_latency DOUBLE NOT NULL,
    CONSTRAINT fk_workload_metrics_app FOREIGN KEY (application_id) REFERENCES applications (id) ON DELETE CASCADE,
    INDEX idx_workload_metrics_app_ts (application_id, timestamp)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6. SCALING POLICIES TABLE
CREATE TABLE IF NOT EXISTS scaling_policies (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    application_id BIGINT NOT NULL UNIQUE,
    minimum_instances INT NOT NULL DEFAULT 1,
    maximum_instances INT NOT NULL DEFAULT 10,
    desired_instances INT NOT NULL DEFAULT 1,
    cpu_scale_out_threshold DOUBLE NOT NULL DEFAULT 75.0,
    cpu_scale_in_threshold DOUBLE NOT NULL DEFAULT 25.0,
    memory_scale_out_threshold DOUBLE NOT NULL DEFAULT 80.0,
    memory_scale_in_threshold DOUBLE NOT NULL DEFAULT 30.0,
    request_rate_threshold DOUBLE NOT NULL DEFAULT 500.0,
    cooldown_seconds INT NOT NULL DEFAULT 300,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_scaling_policies_app FOREIGN KEY (application_id) REFERENCES applications (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 7. SCALING RECOMMENDATIONS TABLE
CREATE TABLE IF NOT EXISTS scaling_recommendations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    application_id BIGINT NOT NULL,
    current_instance_count INT NOT NULL,
    recommended_instance_count INT NOT NULL,
    scaling_action VARCHAR(50) NOT NULL,
    triggering_metric VARCHAR(100) NOT NULL,
    observed_metric_value DOUBLE NOT NULL,
    configured_threshold DOUBLE NOT NULL,
    decision_reason TEXT NOT NULL,
    execution_mode VARCHAR(50) NOT NULL DEFAULT 'MANUAL_APPROVAL',
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    evaluated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_scaling_recom_app FOREIGN KEY (application_id) REFERENCES applications (id) ON DELETE CASCADE,
    INDEX idx_scaling_recom_app_status (application_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 8. SCALING EVENTS (AUDIT TRAIL) TABLE
CREATE TABLE IF NOT EXISTS scaling_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    application_id BIGINT NOT NULL,
    recommendation_id BIGINT,
    previous_instance_count INT NOT NULL,
    newInstance_count INT NOT NULL,
    scaling_action VARCHAR(50) NOT NULL,
    triggering_metric VARCHAR(100) NOT NULL,
    triggering_metric_value DOUBLE NOT NULL,
    decision_reason TEXT NOT NULL,
    execution_mode VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'COMPLETED',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,
    CONSTRAINT fk_scaling_events_app FOREIGN KEY (application_id) REFERENCES applications (id) ON DELETE CASCADE,
    CONSTRAINT fk_scaling_events_recom FOREIGN KEY (recommendation_id) REFERENCES scaling_recommendations (id) ON DELETE SET NULL,
    INDEX idx_scaling_events_app_created (application_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 9. ALERTS TABLE
CREATE TABLE IF NOT EXISTS alerts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    application_id BIGINT NOT NULL,
    instance_id BIGINT,
    alert_type VARCHAR(50) NOT NULL,
    severity VARCHAR(50) NOT NULL,
    message VARCHAR(255) NOT NULL,
    metric_name VARCHAR(100) NOT NULL,
    metric_value DOUBLE NOT NULL,
    threshold_value DOUBLE NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP,
    CONSTRAINT fk_alerts_app FOREIGN KEY (application_id) REFERENCES applications (id) ON DELETE CASCADE,
    CONSTRAINT fk_alerts_instance FOREIGN KEY (instance_id) REFERENCES infrastructure_instances (id) ON DELETE SET NULL,
    INDEX idx_alerts_app_status_severity (application_id, status, severity)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
