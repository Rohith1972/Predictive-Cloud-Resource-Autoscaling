package com.project.autoscaling.simulation.service;

import com.project.autoscaling.application.entity.Application;
import com.project.autoscaling.application.repository.ApplicationRepository;
import com.project.autoscaling.common.enums.ApplicationStatus;
import com.project.autoscaling.common.enums.InstanceStatus;
import com.project.autoscaling.common.enums.ProviderType;
import com.project.autoscaling.common.enums.WorkloadProfile;
import com.project.autoscaling.common.exception.DuplicateResourceException;
import com.project.autoscaling.common.exception.InvalidOperationException;
import com.project.autoscaling.common.exception.ResourceNotFoundException;
import com.project.autoscaling.infrastructure.entity.Instance;
import com.project.autoscaling.infrastructure.repository.InstanceRepository;
import com.project.autoscaling.monitoring.dto.MetricIngestionRequest;
import com.project.autoscaling.monitoring.service.MonitoringService;
import com.project.autoscaling.simulation.dto.SimulationStatusResponse;
import com.project.autoscaling.simulation.dto.StartSimulationRequest;
import com.project.autoscaling.workload.dto.WorkloadRecordRequest;
import com.project.autoscaling.workload.service.WorkloadService;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkloadSimulationServiceImpl implements WorkloadSimulationService {

    private final ApplicationRepository applicationRepository;
    private final InstanceRepository instanceRepository;
    private final MonitoringService monitoringService;
    private final WorkloadService workloadService;

    @Qualifier("simulationTaskScheduler")
    private final ThreadPoolTaskScheduler taskScheduler;

    private final ConcurrentHashMap<Long, RunningSimulation> activeSimulations = new ConcurrentHashMap<>();

    @Getter
    private static class RunningSimulation {
        private final Long applicationId;
        private final String applicationName;
        private final WorkloadProfile profile;
        private final int intervalSeconds;
        private final Long randomSeed;
        private final Integer durationSeconds;
        private final LocalDateTime startedAt;
        private final ScheduledFuture<?> scheduledFuture;
        private final AtomicLong tickCounter = new AtomicLong(0);
        private volatile LocalDateTime lastGeneratedAt;
        private final Double baseCpu;
        private final Double baseMemory;
        private final Double baseRps;

        public RunningSimulation(
                Long applicationId,
                String applicationName,
                StartSimulationRequest request,
                ScheduledFuture<?> scheduledFuture) {
            this.applicationId = applicationId;
            this.applicationName = applicationName;
            this.profile = request.getWorkloadProfile();
            this.intervalSeconds = request.getIntervalSeconds();
            this.randomSeed = request.getRandomSeed();
            this.durationSeconds = request.getDurationSeconds();
            this.baseCpu = request.getBaseCpu();
            this.baseMemory = request.getBaseMemory();
            this.baseRps = request.getBaseRequestRate();
            this.startedAt = LocalDateTime.now();
            this.scheduledFuture = scheduledFuture;
        }

        public void recordTick() {
            this.tickCounter.incrementAndGet();
            this.lastGeneratedAt = LocalDateTime.now();
        }
    }

    @Override
    public SimulationStatusResponse startSimulation(Long applicationId, StartSimulationRequest request) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application", "id", applicationId));

        if (application.getStatus() != ApplicationStatus.ACTIVE) {
            throw new InvalidOperationException("Cannot start workload simulation on an INACTIVE application");
        }

        if (activeSimulations.containsKey(applicationId)) {
            throw new DuplicateResourceException("Workload simulation is already actively running for application ID " + applicationId);
        }

        ensureSampleInstancesExist(application);

        ScheduledFuture<?> future = taskScheduler.scheduleAtFixedRate(
                () -> executeSimulationTick(applicationId),
                Duration.ofSeconds(Math.max(1, request.getIntervalSeconds()))
        );

        RunningSimulation simulation = new RunningSimulation(applicationId, application.getApplicationName(), request, future);
        activeSimulations.put(applicationId, simulation);

        log.info("Started workload simulation for application '{}' (id: {}) with profile '{}' every {}s",
                application.getApplicationName(), applicationId, request.getWorkloadProfile(), request.getIntervalSeconds());

        return mapToResponse(simulation, true);
    }

    @Override
    public SimulationStatusResponse stopSimulation(Long applicationId) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application", "id", applicationId));

        RunningSimulation simulation = activeSimulations.remove(applicationId);
        if (simulation != null) {
            simulation.getScheduledFuture().cancel(false);
            log.info("Stopped workload simulation for application '{}' (id: {})",
                    application.getApplicationName(), applicationId);
            return mapToResponse(simulation, false);
        }

        return SimulationStatusResponse.builder()
                .applicationId(applicationId)
                .applicationName(application.getApplicationName())
                .running(false)
                .metricsGeneratedCount(0)
                .simulated(true)
                .build();
    }

    @Override
    public SimulationStatusResponse getSimulationStatus(Long applicationId) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application", "id", applicationId));

        RunningSimulation simulation = activeSimulations.get(applicationId);
        if (simulation != null) {
            return mapToResponse(simulation, true);
        }

        return SimulationStatusResponse.builder()
                .applicationId(applicationId)
                .applicationName(application.getApplicationName())
                .running(false)
                .metricsGeneratedCount(0)
                .simulated(true)
                .build();
    }

    @Override
    public void generateSingleTick(Long applicationId, StartSimulationRequest request) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application", "id", applicationId));

        ensureSampleInstancesExist(application);
        generateAndPersistMetrics(applicationId, request.getWorkloadProfile(), 0, request.getRandomSeed(),
                request.getBaseCpu(), request.getBaseMemory(), request.getBaseRequestRate());
    }

    private void executeSimulationTick(Long applicationId) {
        RunningSimulation simulation = activeSimulations.get(applicationId);
        if (simulation == null) {
            return;
        }

        try {
            // Check duration limit if set
            if (simulation.getDurationSeconds() != null) {
                long elapsedSeconds = Duration.between(simulation.getStartedAt(), LocalDateTime.now()).getSeconds();
                if (elapsedSeconds >= simulation.getDurationSeconds()) {
                    log.info("Simulation duration of {}s expired for app {}. Stopping automatically.",
                            simulation.getDurationSeconds(), applicationId);
                    stopSimulation(applicationId);
                    return;
                }
            }

            long tickIndex = simulation.getTickCounter().get();
            generateAndPersistMetrics(
                    applicationId,
                    simulation.getProfile(),
                    tickIndex,
                    simulation.getRandomSeed(),
                    simulation.getBaseCpu(),
                    simulation.getBaseMemory(),
                    simulation.getBaseRps()
            );
            simulation.recordTick();
        } catch (Exception e) {
            log.error("Error executing simulation tick for application {}: {}", applicationId, e.getMessage(), e);
        }
    }

    private void generateAndPersistMetrics(
            Long applicationId,
            WorkloadProfile profile,
            long tickIndex,
            Long randomSeed,
            Double baseCpu,
            Double baseMemory,
            Double baseRps
    ) {
        Random random = randomSeed != null ? new Random(randomSeed + tickIndex * 31) : new Random();
        LocalDateTime now = LocalDateTime.now();

        List<Instance> instances = instanceRepository.findByApplicationIdAndStatus(applicationId, InstanceStatus.RUNNING);
        if (instances.isEmpty()) {
            log.warn("No RUNNING instances found for application ID {} during simulation tick", applicationId);
            return;
        }

        double defaultCpu = baseCpu != null ? baseCpu : 45.0;
        double defaultMem = baseMemory != null ? baseMemory : 50.0;
        double defaultRps = baseRps != null ? baseRps : 100.0;

        double targetCpu = defaultCpu;
        double targetMem = defaultMem;
        double targetRps = defaultRps;
        double targetLatency = 75.0;
        double targetErrorRate = 0.2;

        switch (profile) {
            case INCREASING -> {
                // Growth proportional to tickIndex
                double growth = Math.min(tickIndex * 5.0, 45.0);
                targetCpu = Math.min(95.0, defaultCpu + growth + (random.nextDouble() * 6 - 3));
                targetMem = Math.min(90.0, defaultMem + (growth * 0.7) + (random.nextDouble() * 4 - 2));
                targetRps = defaultRps + (growth * 5) + (random.nextDouble() * 20 - 10);
                targetLatency = 100.0 + (growth * 4) + (random.nextDouble() * 20);
                targetErrorRate = Math.min(10.0, 0.2 + (growth * 0.05));
            }
            case TRAFFIC_SPIKE -> {
                // Sudden violent spike
                targetCpu = 88.0 + (random.nextDouble() * 10.0); // 88% - 98%
                targetMem = 82.0 + (random.nextDouble() * 12.0); // 82% - 94%
                targetRps = 450.0 + (random.nextDouble() * 150.0); // 450 - 600 rps
                targetLatency = 380.0 + (random.nextDouble() * 120.0);
                targetErrorRate = 2.5 + (random.nextDouble() * 2.0);
            }
            case TRAFFIC_REDUCTION -> {
                // Dropping load
                double decay = Math.min(tickIndex * 6.0, 35.0);
                targetCpu = Math.max(12.0, defaultCpu - decay + (random.nextDouble() * 4 - 2));
                targetMem = Math.max(20.0, defaultMem - (decay * 0.5) + (random.nextDouble() * 4 - 2));
                targetRps = Math.max(10.0, defaultRps - (decay * 4) + (random.nextDouble() * 10 - 5));
                targetLatency = Math.max(30.0, 60.0 - decay + (random.nextDouble() * 10));
                targetErrorRate = 0.05 + (random.nextDouble() * 0.1);
            }
            default -> {
                // NORMAL or other: steady normal state with realistic natural variance (+/- 5%)
                targetCpu = Math.max(10.0, Math.min(90.0, defaultCpu + (random.nextDouble() * 10 - 5)));
                targetMem = Math.max(15.0, Math.min(85.0, defaultMem + (random.nextDouble() * 6 - 3)));
                targetRps = Math.max(20.0, defaultRps + (random.nextDouble() * 20 - 10));
                targetLatency = 75.0 + (random.nextDouble() * 25 - 12);
                targetErrorRate = 0.1 + (random.nextDouble() * 0.3);
            }
        }

        // Persist telemetry per instance
        for (Instance inst : instances) {
            double jitterCpu = Math.max(0.0, Math.min(100.0, targetCpu + (random.nextDouble() * 4 - 2)));
            double jitterMem = Math.max(0.0, Math.min(100.0, targetMem + (random.nextDouble() * 4 - 2)));
            double instRps = Math.max(0.0, (targetRps / instances.size()) + (random.nextDouble() * 4 - 2));

            MetricIngestionRequest metricReq = MetricIngestionRequest.builder()
                    .applicationId(applicationId)
                    .instanceId(inst.getId())
                    .timestamp(now)
                    .cpuUtilization(Math.round(jitterCpu * 10.0) / 10.0)
                    .memoryUtilization(Math.round(jitterMem * 10.0) / 10.0)
                    .requestRate(Math.round(instRps * 10.0) / 10.0)
                    .averageResponseTime(Math.round(targetLatency * 10.0) / 10.0)
                    .errorRate(Math.round(targetErrorRate * 10.0) / 10.0)
                    .activeRequests((int) Math.round(instRps * 0.3))
                    .networkInBytes(Math.round(instRps * 1500L))
                    .networkOutBytes(Math.round(instRps * 4500L))
                    .build();

            monitoringService.recordMetric(metricReq);
        }

        // Persist application-level workload record
        long totalReqs = Math.round(targetRps * 5);
        long failedReqs = Math.round(totalReqs * (targetErrorRate / 100.0));
        long successfulReqs = totalReqs - failedReqs;

        WorkloadRecordRequest workloadReq = WorkloadRecordRequest.builder()
                .applicationId(applicationId)
                .timestamp(now)
                .requestCount(totalReqs)
                .requestsPerSecond(Math.round(targetRps * 10.0) / 10.0)
                .successfulRequests(successfulReqs)
                .failedRequests(failedReqs)
                .averageLatency(Math.round(targetLatency * 10.0) / 10.0)
                .p95Latency(Math.round(targetLatency * 1.3 * 10.0) / 10.0)
                .p99Latency(Math.round(targetLatency * 1.8 * 10.0) / 10.0)
                .build();

        workloadService.recordWorkload(workloadReq);
    }

    private void ensureSampleInstancesExist(Application application) {
        long runningCount = instanceRepository.countByApplicationIdAndStatus(application.getId(), InstanceStatus.RUNNING);
        if (runningCount == 0) {
            log.info("No running instances found for application {}. Generating simulated baseline instances.", application.getId());
            for (int i = 1; i <= 2; i++) {
                Instance sim = Instance.builder()
                        .instanceIdentifier("sim-node-" + application.getId() + "-" + UUID.randomUUID().toString().substring(0, 8))
                        .instanceName("simulated-" + application.getApplicationName().toLowerCase().replaceAll("\\s+", "-") + "-0" + i)
                        .application(application)
                        .instanceType("t3.large")
                        .cpuCores(2)
                        .totalMemoryMb(8192L)
                        .status(InstanceStatus.RUNNING)
                        .provider(ProviderType.LOCAL_SIMULATED)
                        .build();
                instanceRepository.save(sim);
            }
        }
    }

    private SimulationStatusResponse mapToResponse(RunningSimulation sim, boolean isRunning) {
        return SimulationStatusResponse.builder()
                .applicationId(sim.getApplicationId())
                .applicationName(sim.getApplicationName())
                .running(isRunning)
                .profile(sim.getProfile())
                .intervalSeconds(sim.getIntervalSeconds())
                .startedAt(sim.getStartedAt())
                .metricsGeneratedCount(sim.getTickCounter().get())
                .lastGeneratedAt(sim.getLastGeneratedAt())
                .simulated(true)
                .build();
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down WorkloadSimulationService. Cancelling all {} active simulations...", activeSimulations.size());
        activeSimulations.values().forEach(sim -> sim.getScheduledFuture().cancel(false));
        activeSimulations.clear();
    }
}
