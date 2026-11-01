package com.project.autoscaling.scaling;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.autoscaling.application.entity.Application;
import com.project.autoscaling.application.repository.ApplicationRepository;
import com.project.autoscaling.common.enums.ApplicationEnvironment;
import com.project.autoscaling.common.enums.ApplicationStatus;
import com.project.autoscaling.common.enums.UserRole;
import com.project.autoscaling.infrastructure.repository.InstanceRepository;
import com.project.autoscaling.monitoring.repository.ResourceMetricRepository;
import com.project.autoscaling.scaling.dto.CreateScalingPolicyRequest;
import com.project.autoscaling.scaling.dto.ScalingPolicyStatusUpdateRequest;
import com.project.autoscaling.scaling.dto.UpdateScalingPolicyRequest;
import com.project.autoscaling.scaling.repository.ScalingPolicyRepository;
import com.project.autoscaling.security.JwtService;
import com.project.autoscaling.security.UserPrincipal;
import com.project.autoscaling.user.entity.User;
import com.project.autoscaling.user.repository.UserRepository;
import com.project.autoscaling.workload.repository.WorkloadMetricRepository;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ScalingPolicyIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ScalingPolicyRepository policyRepository;

    @Autowired
    private WorkloadMetricRepository workloadRepository;

    @Autowired
    private ResourceMetricRepository metricRepository;

    @Autowired
    private InstanceRepository instanceRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

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
        policyRepository.deleteAll();
        workloadRepository.deleteAll();
        metricRepository.deleteAll();
        instanceRepository.deleteAll();
        applicationRepository.deleteAll();
        userRepository.deleteAll();

        User admin = userRepository.save(User.builder()
                .fullName("Policy Admin")
                .email("admin-policy@test.com")
                .password(passwordEncoder.encode("Password123"))
                .role(UserRole.ADMIN)
                .enabled(true)
                .build());

        User viewer = userRepository.save(User.builder()
                .fullName("Policy Viewer")
                .email("viewer-policy@test.com")
                .password(passwordEncoder.encode("Password123"))
                .role(UserRole.VIEWER)
                .enabled(true)
                .build());

        adminToken = "Bearer " + jwtService.generateToken(new UserPrincipal(admin), Map.of("role", "ADMIN"));
        viewerToken = "Bearer " + jwtService.generateToken(new UserPrincipal(viewer), Map.of("role", "VIEWER"));

        testApp = applicationRepository.save(Application.builder()
                .applicationName("Inventory Service")
                .applicationUrl("https://inventory.internal")
                .environment(ApplicationEnvironment.PRODUCTION)
                .status(ApplicationStatus.ACTIVE)
                .build());
    }

    @Test
    void shouldCreateScalingPolicySuccessfully() throws Exception {
        CreateScalingPolicyRequest request = CreateScalingPolicyRequest.builder()
                .applicationId(testApp.getId())
                .minimumInstances(2)
                .maximumInstances(10)
                .desiredInstances(3)
                .cpuScaleOutThreshold(75.0)
                .cpuScaleInThreshold(25.0)
                .memoryScaleOutThreshold(80.0)
                .memoryScaleInThreshold(30.0)
                .requestRateThreshold(400.0)
                .cooldownSeconds(300)
                .enabled(true)
                .build();

        mockMvc.perform(post("/api/v1/scaling/policies")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.minimumInstances").value(2))
                .andExpect(jsonPath("$.data.maximumInstances").value(10))
                .andExpect(jsonPath("$.data.desiredInstances").value(3));
    }

    @Test
    void shouldRejectInvalidPolicyThresholds() throws Exception {
        // scale-in >= scale-out is illegal
        CreateScalingPolicyRequest request = CreateScalingPolicyRequest.builder()
                .applicationId(testApp.getId())
                .minimumInstances(2)
                .maximumInstances(10)
                .desiredInstances(3)
                .cpuScaleOutThreshold(50.0)
                .cpuScaleInThreshold(60.0) // Invalid! scaleIn > scaleOut
                .memoryScaleOutThreshold(80.0)
                .memoryScaleInThreshold(30.0)
                .requestRateThreshold(400.0)
                .cooldownSeconds(300)
                .enabled(true)
                .build();

        mockMvc.perform(post("/api/v1/scaling/policies")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("lower than cpuScaleOutThreshold")));
    }

    @Test
    void shouldRejectDesiredInstancesOutOfBounds() throws Exception {
        CreateScalingPolicyRequest request = CreateScalingPolicyRequest.builder()
                .applicationId(testApp.getId())
                .minimumInstances(2)
                .maximumInstances(5)
                .desiredInstances(8) // Invalid! > max
                .cpuScaleOutThreshold(75.0)
                .cpuScaleInThreshold(25.0)
                .memoryScaleOutThreshold(80.0)
                .memoryScaleInThreshold(30.0)
                .requestRateThreshold(400.0)
                .cooldownSeconds(300)
                .build();

        mockMvc.perform(post("/api/v1/scaling/policies")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("within range")));
    }

    @Test
    void shouldTogglePolicyStatus() throws Exception {
        CreateScalingPolicyRequest request = CreateScalingPolicyRequest.builder()
                .applicationId(testApp.getId())
                .minimumInstances(1)
                .maximumInstances(5)
                .desiredInstances(2)
                .cpuScaleOutThreshold(70.0)
                .cpuScaleInThreshold(30.0)
                .memoryScaleOutThreshold(80.0)
                .memoryScaleInThreshold(30.0)
                .requestRateThreshold(300.0)
                .cooldownSeconds(180)
                .enabled(true)
                .build();

        String respStr = mockMvc.perform(post("/api/v1/scaling/policies")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        long policyId = objectMapper.readTree(respStr).path("data").path("id").asLong();

        ScalingPolicyStatusUpdateRequest statusReq = ScalingPolicyStatusUpdateRequest.builder()
                .enabled(false)
                .build();

        mockMvc.perform(patch("/api/v1/scaling/policies/" + policyId + "/status")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(false));
    }
}
