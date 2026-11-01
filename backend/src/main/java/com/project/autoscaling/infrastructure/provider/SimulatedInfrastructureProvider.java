package com.project.autoscaling.infrastructure.provider;

import com.project.autoscaling.application.entity.Application;
import com.project.autoscaling.application.repository.ApplicationRepository;
import com.project.autoscaling.common.enums.InstanceStatus;
import com.project.autoscaling.common.enums.ProviderType;
import com.project.autoscaling.common.exception.ResourceNotFoundException;
import com.project.autoscaling.infrastructure.entity.Instance;
import com.project.autoscaling.infrastructure.repository.InstanceRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class SimulatedInfrastructureProvider implements InfrastructureProvider {

    private static final Logger log = LoggerFactory.getLogger(SimulatedInfrastructureProvider.class);

    private final InstanceRepository instanceRepository;
    private final ApplicationRepository applicationRepository;

    @Override
    public ProviderType getProviderType() {
        return ProviderType.LOCAL_SIMULATED;
    }

    @Override
    @Transactional(readOnly = true)
    public List<InstanceCapacityDetails> listInstances(Long applicationId) {
        return instanceRepository.findByApplicationId(applicationId).stream()
                .map(this::toCapacityDetails)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public InstanceCapacityDetails describeInstance(String instanceIdentifier) {
        Instance instance = instanceRepository.findByInstanceIdentifier(instanceIdentifier)
                .orElseThrow(() -> new ResourceNotFoundException("Instance", "identifier", instanceIdentifier));
        return toCapacityDetails(instance);
    }

    @Override
    @Transactional
    public boolean updateDesiredCapacity(Long applicationId, int desiredCapacity) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application", "id", applicationId));

        List<Instance> activeInstances = instanceRepository.findByApplicationIdAndStatus(applicationId, InstanceStatus.RUNNING);
        int currentCount = activeInstances.size();

        log.info("Simulated provider: adjusting capacity for app {} from {} to {}", applicationId, currentCount, desiredCapacity);

        if (desiredCapacity > currentCount) {
            // Scale out: provision simulated instances
            int toAdd = desiredCapacity - currentCount;
            for (int i = 0; i < toAdd; i++) {
                String identifier = "i-sim-" + UUID.randomUUID().toString().substring(0, 8);
                Instance newInstance = Instance.builder()
                        .instanceIdentifier(identifier)
                        .instanceName(application.getApplicationName().toLowerCase().replace(" ", "-") + "-node-" + (currentCount + i + 1))
                        .application(application)
                        .instanceType("t3.medium")
                        .cpuCores(2)
                        .totalMemoryMb(4096L)
                        .status(InstanceStatus.RUNNING)
                        .provider(ProviderType.LOCAL_SIMULATED)
                        .build();
                instanceRepository.save(newInstance);
            }
        } else if (desiredCapacity < currentCount) {
            // Scale in: terminate instances from the end
            int toRemove = currentCount - desiredCapacity;
            for (int i = 0; i < toRemove; i++) {
                Instance instanceToTerminate = activeInstances.get(activeInstances.size() - 1 - i);
                instanceToTerminate.setStatus(InstanceStatus.TERMINATED);
                instanceRepository.save(instanceToTerminate);
            }
        }
        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public int getActiveInstanceCount(Long applicationId) {
        return (int) instanceRepository.countByApplicationIdAndStatus(applicationId, InstanceStatus.RUNNING);
    }

    private InstanceCapacityDetails toCapacityDetails(Instance instance) {
        return InstanceCapacityDetails.builder()
                .instanceIdentifier(instance.getInstanceIdentifier())
                .instanceName(instance.getInstanceName())
                .instanceType(instance.getInstanceType())
                .cpuCores(instance.getCpuCores())
                .totalMemoryMb(instance.getTotalMemoryMb())
                .status(instance.getStatus())
                .provider(instance.getProvider())
                .build();
    }
}
