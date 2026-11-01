package com.project.autoscaling.scaling;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.autoscaling.application.entity.Application;
import com.project.autoscaling.application.repository.ApplicationRepository;
import com.project.autoscaling.common.enums.*;
import com.project.autoscaling.infrastructure.entity.Instance;
import com.project.autoscaling.infrastructure.repository.InstanceRepository;
import com.project.autoscaling.monitoring.entity.ResourceMetric;
import com.project.autoscaling.monitoring.repository.ResourceMetricRepository;
import com.project.autoscaling.scaling.dto.ScalingEvaluationRequest;
import com.project.autoscaling.scaling.entity.ScalingPolicy;
import com.project.autoscaling.scaling.repository.ScalingEventRepository;
import com.project.autoscaling.scaling.repository.ScalingPolicyRepository;
import com.project.autoscaling.scaling.repository.ScalingRecommendationRepository;
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
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ScalingEngineIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private com.project.autoscaling.test.TestDatabaseCleaner cleaner;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private InstanceRepository instanceRepository;

    @Autowired
    private ResourceMetricRepository metricRepository;

    @Autowired
    private ScalingPolicyRepository policyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private String adminToken;
    private String viewerToken;
    private Application testApp;
    private Instance testInstance;

    @BeforeEach
    void setUp() {
        cleaner.clean();

        User admin = userRepository.save(User.builder()
                .fullName("Scaling Admin")
                .email("admin-scaling@test.com")
                .password(passwordEncoder.encode("Password123"))
                .role(UserRole.ADMIN)
                .enabled(true)
                .build());

        User viewer = userRepository.save(User.builder()
                .fullName("Scaling Viewer")
                .email("viewer-scaling@test.com")
                .password(passwordEncoder.encode("Password123"))
                .role(UserRole.VIEWER)
                .enabled(true)
                .build());

        adminToken = "Bearer " + jwtService.generateToken(new UserPrincipal(admin), Map.of("role", "ADMIN"));
        viewerToken = "Bearer " + jwtService.generateToken(new UserPrincipal(viewer), Map.of("role", "VIEWER"));

        testApp = applicationRepository.save(Application.builder()
                .applicationName("Payment Processing Engine")
                .applicationUrl("https://ppe.internal")
                .environment(ApplicationEnvironment.PRODUCTION)
                .status(ApplicationStatus.ACTIVE)
                .build());

        testInstance = instanceRepository.save(Instance.builder()
                .instanceIdentifier("i-ppe-01")
                .instanceName("ppe-node-1")
                .application(testApp)
                .instanceType("c5.xlarge")
                .cpuCores(4)
                .totalMemoryMb(8192L)
                .status(InstanceStatus.RUNNING)
                .build());

        // Configure policy: min 1, max 8, cpuOut 75%, cpuIn 25%, cooldown 300s
        policyRepository.save(ScalingPolicy.builder()
                .application(testApp)
                .minimumInstances(1)
                .maximumInstances(8)
                .desiredInstances(1)
                .cpuScaleOutThreshold(75.0)
                .cpuScaleInThreshold(25.0)
                .memoryScaleOutThreshold(80.0)
                .memoryScaleInThreshold(30.0)
                .requestRateThreshold(500.0)
                .cooldownSeconds(300)
                .enabled(true)
                .build());
    }

    @Test
    void shouldRecommendScaleOutWhenCpuExceedsThreshold() throws Exception {
        // Record high CPU metric: 90%
        metricRepository.save(ResourceMetric.builder()
                .application(testApp)
                .instance(testInstance)
                .timestamp(LocalDateTime.now())
                .cpuUtilization(90.0)
                .memoryUtilization(60.0)
                .requestRate(200.0)
                .averageResponseTime(55.0)
                .errorRate(0.0)
                .build());

        ScalingEvaluationRequest evalReq = ScalingEvaluationRequest.builder()
                .executionMode(ExecutionMode.MANUAL_APPROVAL)
                .build();

        mockMvc.perform(post("/api/v1/scaling/evaluate/" + testApp.getId())
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(evalReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.scalingAction").value("SCALE_OUT"))
                .andExpect(jsonPath("$.data.recommendedInstanceCount").value(greaterThan(1)))
                .andExpect(jsonPath("$.data.triggeringMetric").value("CPU_UTILIZATION"))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    void shouldRecommendScaleInWhenResourcesAreUnderutilized() throws Exception {
        // Add 2 running instances
        instanceRepository.save(Instance.builder()
                .instanceIdentifier("i-ppe-02")
                .instanceName("ppe-node-2")
                .application(testApp)
                .instanceType("c5.xlarge")
                .cpuCores(4)
                .totalMemoryMb(8192L)
                .status(InstanceStatus.RUNNING)
                .build());

        // Record low resource metric: CPU 15%, Memory 20%, RPS 50
        metricRepository.save(ResourceMetric.builder()
                .application(testApp)
                .instance(testInstance)
                .timestamp(LocalDateTime.now())
                .cpuUtilization(15.0)
                .memoryUtilization(20.0)
                .requestRate(50.0)
                .averageResponseTime(12.0)
                .errorRate(0.0)
                .build());

        mockMvc.perform(post("/api/v1/scaling/evaluate/" + testApp.getId())
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.scalingAction").value("SCALE_IN"))
                .andExpect(jsonPath("$.data.recommendedInstanceCount").value(1));
    }

    @Test
    void shouldApproveRecommendationAndAdjustSimulatedCapacity() throws Exception {
        // Record high CPU metric
        metricRepository.save(ResourceMetric.builder()
                .application(testApp)
                .instance(testInstance)
                .timestamp(LocalDateTime.now())
                .cpuUtilization(85.0)
                .memoryUtilization(50.0)
                .requestRate(100.0)
                .averageResponseTime(40.0)
                .errorRate(0.0)
                .build());

        // Evaluate
        MvcResult evalResult = mockMvc.perform(post("/api/v1/scaling/evaluate/" + testApp.getId())
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.scalingAction").value("SCALE_OUT"))
                .andReturn();

        long recommendationId = objectMapper.readTree(evalResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        // Approve recommendation
        mockMvc.perform(post("/api/v1/scaling/recommendations/" + recommendationId + "/approve")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.newInstanceCount").value(greaterThan(1)));

        // Duplicate approval must be rejected
        mockMvc.perform(post("/api/v1/scaling/recommendations/" + recommendationId + "/approve")
                        .header("Authorization", adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("cannot be approved because its status is APPROVED")));
    }

    @Test
    void shouldEnforceCooldownSafeguard() throws Exception {
        // High CPU
        metricRepository.save(ResourceMetric.builder()
                .application(testApp)
                .instance(testInstance)
                .timestamp(LocalDateTime.now())
                .cpuUtilization(88.0)
                .memoryUtilization(60.0)
                .requestRate(200.0)
                .averageResponseTime(40.0)
                .errorRate(0.0)
                .build());

        // Evaluate and approve once
        MvcResult evalResult = mockMvc.perform(post("/api/v1/scaling/evaluate/" + testApp.getId())
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andReturn();

        long recId = objectMapper.readTree(evalResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        mockMvc.perform(post("/api/v1/scaling/recommendations/" + recId + "/approve")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk());

        // Immediate next evaluation must be stopped by cooldown
        mockMvc.perform(post("/api/v1/scaling/evaluate/" + testApp.getId())
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.scalingAction").value("NO_ACTION"))
                .andExpect(jsonPath("$.data.decisionReason", containsString("Cooldown active")));
    }
}
