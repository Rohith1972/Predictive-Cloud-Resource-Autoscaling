package com.project.autoscaling.application.repository;

import com.project.autoscaling.application.entity.Application;
import com.project.autoscaling.common.enums.ApplicationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {
    Optional<Application> findByApplicationName(String applicationName);
    boolean existsByApplicationName(String applicationName);
    Page<Application> findByStatus(ApplicationStatus status, Pageable pageable);
    long countByStatus(ApplicationStatus status);
}
