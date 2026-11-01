package com.project.autoscaling.infrastructure.provider;

import com.project.autoscaling.common.enums.ProviderType;

import java.util.List;

/**
 * Provider-independent abstraction for infrastructure operations.
 * Future AWS implementation (using AWS EC2 / Auto Scaling Groups) will implement this interface.
 */
public interface InfrastructureProvider {

    ProviderType getProviderType();

    List<InstanceCapacityDetails> listInstances(Long applicationId);

    InstanceCapacityDetails describeInstance(String instanceIdentifier);

    boolean updateDesiredCapacity(Long applicationId, int desiredCapacity);

    int getActiveInstanceCount(Long applicationId);
}
