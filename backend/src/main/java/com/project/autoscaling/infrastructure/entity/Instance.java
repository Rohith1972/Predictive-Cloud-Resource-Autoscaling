package com.project.autoscaling.infrastructure.entity;

import com.project.autoscaling.application.entity.Application;
import com.project.autoscaling.common.enums.InstanceStatus;
import com.project.autoscaling.common.enums.ProviderType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "infrastructure_instances")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Instance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "instance_identifier", nullable = false, unique = true, length = 100)
    private String instanceIdentifier;

    @Column(name = "instance_name", nullable = false, length = 150)
    private String instanceName;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false)
    private Application application;

    @Column(name = "instance_type", nullable = false, length = 50)
    private String instanceType;

    @Column(name = "cpu_cores", nullable = false)
    private Integer cpuCores;

    @Column(name = "total_memory_mb", nullable = false)
    private Long totalMemoryMb;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private InstanceStatus status = InstanceStatus.RUNNING;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 50)
    @Builder.Default
    private ProviderType provider = ProviderType.LOCAL_SIMULATED;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
