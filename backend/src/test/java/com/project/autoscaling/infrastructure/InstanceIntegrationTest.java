package com.project.autoscaling.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.autoscaling.application.entity.Application;
import com.project.autoscaling.application.repository.ApplicationRepository;
import com.project.autoscaling.common.enums.ApplicationEnvironment;
import com.project.autoscaling.common.enums.ApplicationStatus;
import com.project.autoscaling.common.enums.InstanceStatus;
import com.project.autoscaling.common.enums.ProviderType;
import com.project.autoscaling.common.enums.UserRole;
import com.project.autoscaling.infrastructure.dto.CreateInstanceRequest;
import com.project.autoscaling.infrastructure.dto.InstanceStatusUpdateRequest;
import com.project.autoscaling.infrastructure.entity.Instance;
import com.project.autoscaling.infrastructure.repository.InstanceRepository;
import com.project.autoscaling.security.JwtService;
import com.project.autoscaling.security.UserPrincipal;
import com.project.autoscaling.user.entity.User;
import com.project.autoscaling.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class InstanceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private InstanceRepository instanceRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private String adminToken;
    private String viewerToken;
    private Application testApp;

    @BeforeEach
    void setUp() {
        instanceRepository.deleteAll();
        applicationRepository.deleteAll();
        userRepository.deleteAll();

        User admin = userRepository.save(User.builder()
                .fullName("Admin")
                .email("admin-infra@test.com")
                .password(passwordEncoder.encode("Password123"))
                .role(UserRole.ADMIN)
                .enabled(true)
                .build());

        User viewer = userRepository.save(User.builder()
                .fullName("Viewer")
                .email("viewer-infra@test.com")
                .password(passwordEncoder.encode("Password123"))
                .role(UserRole.VIEWER)
                .enabled(true)
                .build());

        adminToken = "Bearer " + jwtService.generateToken(new UserPrincipal(admin), Map.of("role", "ADMIN"));
        viewerToken = "Bearer " + jwtService.generateToken(new UserPrincipal(viewer), Map.of("role", "VIEWER"));

        testApp = applicationRepository.save(Application.builder()
                .applicationName("Order Microservice")
                .applicationUrl("https://orders.internal")
                .environment(ApplicationEnvironment.PRODUCTION)
                .status(ApplicationStatus.ACTIVE)
                .build());
    }

    @Test
    void shouldRegisterInstanceSuccessfully() throws Exception {
        CreateInstanceRequest request = CreateInstanceRequest.builder()
                .instanceIdentifier("i-sim-0010")
                .instanceName("orders-worker-1")
                .applicationId(testApp.getId())
                .instanceType("c5.large")
                .cpuCores(2)
                .totalMemoryMb(4096L)
                .provider(ProviderType.LOCAL_SIMULATED)
                .build();

        mockMvc.perform(post("/api/v1/instances")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.instanceIdentifier").value("i-sim-0010"))
                .andExpect(jsonPath("$.data.cpuCores").value(2))
                .andExpect(jsonPath("$.data.status").value("RUNNING"));
    }

    @Test
    void shouldCalculateCapacitySummaryCorrectly() throws Exception {
        // Register two running instances and one stopped instance
        instanceRepository.save(Instance.builder()
                .instanceIdentifier("i-node-1")
                .instanceName("node-1")
                .application(testApp)
                .instanceType("t3.medium")
                .cpuCores(2)
                .totalMemoryMb(4096L)
                .status(InstanceStatus.RUNNING)
                .build());

        instanceRepository.save(Instance.builder()
                .instanceIdentifier("i-node-2")
                .instanceName("node-2")
                .application(testApp)
                .instanceType("t3.large")
                .cpuCores(4)
                .totalMemoryMb(8192L)
                .status(InstanceStatus.RUNNING)
                .build());

        instanceRepository.save(Instance.builder()
                .instanceIdentifier("i-node-3")
                .instanceName("node-3")
                .application(testApp)
                .instanceType("t3.medium")
                .cpuCores(2)
                .totalMemoryMb(4096L)
                .status(InstanceStatus.STOPPED)
                .build());

        mockMvc.perform(get("/api/v1/instances/application/" + testApp.getId() + "/capacity")
                        .header("Authorization", viewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalInstances").value(3))
                .andExpect(jsonPath("$.data.runningInstances").value(2))
                .andExpect(jsonPath("$.data.stoppedInstances").value(1))
                .andExpect(jsonPath("$.data.totalActiveCpuCores").value(6))
                .andExpect(jsonPath("$.data.totalActiveMemoryMb").value(12288));
    }

    @Test
    void shouldUpdateInstanceStatus() throws Exception {
        Instance inst = instanceRepository.save(Instance.builder()
                .instanceIdentifier("i-node-test")
                .instanceName("test-node")
                .application(testApp)
                .instanceType("t3.micro")
                .cpuCores(1)
                .totalMemoryMb(1024L)
                .status(InstanceStatus.RUNNING)
                .build());

        InstanceStatusUpdateRequest update = InstanceStatusUpdateRequest.builder()
                .status(InstanceStatus.UNHEALTHY)
                .build();

        mockMvc.perform(patch("/api/v1/instances/" + inst.getId() + "/status")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("UNHEALTHY"));
    }
}
