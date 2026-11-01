package com.project.autoscaling.infrastructure.repository;

import com.project.autoscaling.common.enums.InstanceStatus;
import com.project.autoscaling.infrastructure.entity.Instance;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InstanceRepository extends JpaRepository<Instance, Long> {
    Optional<Instance> findByInstanceIdentifier(String instanceIdentifier);
    boolean existsByInstanceIdentifier(String instanceIdentifier);
    Page<Instance> findByStatus(InstanceStatus status, Pageable pageable);
    List<Instance> findByApplicationId(Long applicationId);
    List<Instance> findByApplicationIdAndStatus(Long applicationId, InstanceStatus status);
    long countByApplicationId(Long applicationId);
    long countByApplicationIdAndStatus(Long applicationId, InstanceStatus status);
    long countByStatus(InstanceStatus status);

    @Query("SELECT COALESCE(SUM(i.cpuCores), 0) FROM Instance i WHERE i.application.id = :applicationId AND i.status = :status")
    int sumCpuCoresByApplicationIdAndStatus(@Param("applicationId") Long applicationId, @Param("status") InstanceStatus status);

    @Query("SELECT COALESCE(SUM(i.totalMemoryMb), 0) FROM Instance i WHERE i.application.id = :applicationId AND i.status = :status")
    long sumTotalMemoryMbByApplicationIdAndStatus(@Param("applicationId") Long applicationId, @Param("status") InstanceStatus status);
}
