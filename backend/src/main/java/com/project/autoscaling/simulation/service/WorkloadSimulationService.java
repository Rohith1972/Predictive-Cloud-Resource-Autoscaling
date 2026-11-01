package com.project.autoscaling.simulation.service;

import com.project.autoscaling.simulation.dto.SimulationStatusResponse;
import com.project.autoscaling.simulation.dto.StartSimulationRequest;

public interface WorkloadSimulationService {
    SimulationStatusResponse startSimulation(Long applicationId, StartSimulationRequest request);
    SimulationStatusResponse stopSimulation(Long applicationId);
    SimulationStatusResponse getSimulationStatus(Long applicationId);
    void generateSingleTick(Long applicationId, StartSimulationRequest request);
}
