package com.project.autoscaling.simulation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.autoscaling.application.entity.Application;
import com.project.autoscaling.application.repository.ApplicationRepository;
import com.project.autoscaling.common.enums.ApplicationEnvironment;
import com.project.autoscaling.common.enums.ApplicationStatus;
import com.project.autoscaling.common.enums.UserRole;
import com.project.autoscaling.common.enums.WorkloadProfile;
import com.project.autoscaling.infrastructure.repository.InstanceRepository;
import com.project.autoscaling.monitoring.repository.ResourceMetricRepository;
import com.project.autoscaling.security.JwtService;
import com.project.autoscaling.security.UserPrincipal;
import com.project.autoscaling.simulation.dto.StartSimulationRequest;
import com.project.autoscaling.simulation.service.WorkloadSimulationService;
import com.project.autoscaling.test.TestDatabaseCleaner;
import com.project.autoscaling.user.entity.User;
import com.project.autoscaling.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class SimulationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TestDatabaseCleaner databaseCleaner;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private InstanceRepository instanceRepository;

    @Autowired
    private ResourceMetricRepository metricRepository;

    @Autowired
    private WorkloadSimulationService simulationService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String adminToken;
    private String viewerToken;
    private Application testApp;

    @BeforeEach
    void setUp() {
        databaseCleaner.clean();

        User admin = userRepository.save(User.builder()
                .fullName("Simulation Admin")
                .email("admin@sim.com")
                .password(passwordEncoder.encode("Password123!"))
                .role(UserRole.ADMIN)
                .enabled(true)
                .build());
        adminToken = "Bearer " + jwtService.generateToken(new UserPrincipal(admin), Map.of("role", "ADMIN"));

        User viewer = userRepository.save(User.builder()
                .fullName("Simulation Viewer")
                .email("viewer@sim.com")
                .password(passwordEncoder.encode("Password123!"))
                .role(UserRole.VIEWER)
                .enabled(true)
                .build());
        viewerToken = "Bearer " + jwtService.generateToken(new UserPrincipal(viewer), Map.of("role", "VIEWER"));

        testApp = applicationRepository.save(Application.builder()
                .applicationName("Simulation Target App")
                .description("Application for load test simulations")
                .applicationUrl("https://sim.test.internal")
                .environment(ApplicationEnvironment.TESTING)
                .status(ApplicationStatus.ACTIVE)
                .build());
    }

    @AfterEach
    void tearDown() {
        if (testApp != null && testApp.getId() != null) {
            simulationService.stopSimulation(testApp.getId());
        }
    }

    @Test
    @DisplayName("Should successfully start simulation and generate baseline instances")
    void testStartAndStopSimulation() throws Exception {
        StartSimulationRequest req = StartSimulationRequest.builder()
                .workloadProfile(WorkloadProfile.TRAFFIC_SPIKE)
                .intervalSeconds(2)
                .randomSeed(12345L)
                .build();

        // Start simulation
        mockMvc.perform(post("/api/v1/simulation/start/" + testApp.getId())
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.running").value(true))
                .andExpect(jsonPath("$.data.profile").value("TRAFFIC_SPIKE"))
                .andExpect(jsonPath("$.data.simulated").value(true));

        // Baseline instances should be automatically provisioned
        assertThat(instanceRepository.count()).isGreaterThanOrEqualTo(2);

        // Status check
        mockMvc.perform(get("/api/v1/simulation/status/" + testApp.getId())
                        .header("Authorization", viewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.running").value(true));

        // Attempt duplicate start on running simulation -> should return 409 Conflict
        mockMvc.perform(post("/api/v1/simulation/start/" + testApp.getId())
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict());

        // Stop simulation
        mockMvc.perform(post("/api/v1/simulation/stop/" + testApp.getId())
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.running").value(false));
    }

    @Test
    @DisplayName("VIEWER should be forbidden from starting or stopping simulation")
    void testViewerUnauthorizedForSimulationControl() throws Exception {
        mockMvc.perform(post("/api/v1/simulation/start/" + testApp.getId())
                        .header("Authorization", viewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/simulation/stop/" + testApp.getId())
                        .header("Authorization", viewerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should generate single deterministic tick and persist telemetry")
    void testDeterministicSingleTickGeneration() {
        StartSimulationRequest req = StartSimulationRequest.builder()
                .workloadProfile(WorkloadProfile.NORMAL)
                .randomSeed(42L)
                .baseCpu(50.0)
                .baseMemory(60.0)
                .baseRequestRate(120.0)
                .build();

        simulationService.generateSingleTick(testApp.getId(), req);

        // Verify metrics persisted
        assertThat(metricRepository.count()).isGreaterThanOrEqualTo(2);
    }
}
