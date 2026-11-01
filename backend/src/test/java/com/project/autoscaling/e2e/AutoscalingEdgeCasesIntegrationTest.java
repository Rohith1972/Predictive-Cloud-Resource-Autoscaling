package com.project.autoscaling.e2e;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.autoscaling.application.entity.Application;
import com.project.autoscaling.application.repository.ApplicationRepository;
import com.project.autoscaling.common.enums.ApplicationEnvironment;
import com.project.autoscaling.common.enums.ApplicationStatus;
import com.project.autoscaling.common.enums.ExecutionMode;
import com.project.autoscaling.common.enums.InstanceStatus;
import com.project.autoscaling.common.enums.ProviderType;
import com.project.autoscaling.common.enums.ScalingAction;
import com.project.autoscaling.common.enums.ScalingEventStatus;
import com.project.autoscaling.common.enums.UserRole;
import com.project.autoscaling.infrastructure.entity.Instance;
import com.project.autoscaling.infrastructure.repository.InstanceRepository;
import com.project.autoscaling.monitoring.dto.MetricIngestionRequest;
import com.project.autoscaling.monitoring.entity.ResourceMetric;
import com.project.autoscaling.monitoring.repository.ResourceMetricRepository;
import com.project.autoscaling.scaling.dto.ScalingEvaluationRequest;
import com.project.autoscaling.scaling.dto.ScalingRecommendationResponse;
import com.project.autoscaling.scaling.entity.ScalingEvent;
import com.project.autoscaling.scaling.entity.ScalingPolicy;
import com.project.autoscaling.scaling.entity.ScalingRecommendation;
import com.project.autoscaling.scaling.repository.ScalingEventRepository;
import com.project.autoscaling.scaling.repository.ScalingPolicyRepository;
import com.project.autoscaling.scaling.repository.ScalingRecommendationRepository;
import com.project.autoscaling.scaling.service.ScalingService;
import com.project.autoscaling.security.JwtService;
import com.project.autoscaling.security.UserPrincipal;
import com.project.autoscaling.test.TestDatabaseCleaner;
import com.project.autoscaling.user.entity.User;
import com.project.autoscaling.user.repository.UserRepository;
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

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AutoscalingEdgeCasesIntegrationTest {

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
    private ScalingPolicyRepository policyRepository;

    @Autowired
    private ScalingRecommendationRepository recommendationRepository;

    @Autowired
    private ScalingEventRepository eventRepository;

    @Autowired
    private ScalingService scalingService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String adminToken;
    private String viewerToken;
    private Application testApp;
    private Instance testInstance;

    @BeforeEach
    void setUp() {
        databaseCleaner.clean();

        User admin = userRepository.save(User.builder()
                .fullName("Edge Case Admin")
                .email("admin@edgecases.com")
                .password(passwordEncoder.encode("Password123!"))
                .role(UserRole.ADMIN)
                .enabled(true)
                .build());
        adminToken = "Bearer " + jwtService.generateToken(new UserPrincipal(admin), Map.of("role", "ADMIN"));

        User viewer = userRepository.save(User.builder()
                .fullName("Edge Case Viewer")
                .email("viewer@edgecases.com")
                .password(passwordEncoder.encode("Password123!"))
                .role(UserRole.VIEWER)
                .enabled(true)
                .build());
        viewerToken = "Bearer " + jwtService.generateToken(new UserPrincipal(viewer), Map.of("role", "VIEWER"));

        testApp = applicationRepository.save(Application.builder()
                .applicationName("Edge Test App")
                .description("App for testing autoscaling edge cases")
                .applicationUrl("https://edge.test.internal")
                .environment(ApplicationEnvironment.TESTING)
                .status(ApplicationStatus.ACTIVE)
                .build());

        testInstance = instanceRepository.save(Instance.builder()
                .instanceIdentifier("i-edge-001")
                .instanceName("edge-node-1")
                .application(testApp)
                .instanceType("t3.medium")
                .cpuCores(2)
                .totalMemoryMb(4096L)
                .status(InstanceStatus.RUNNING)
                .provider(ProviderType.LOCAL_SIMULATED)
                .build());
    }

    @Test
    @DisplayName("Edge Case: Ingest metrics at boundary CPU = 0% and 100%, reject invalid ranges")
    void testCpuZeroAndHundredPercentValidation() throws Exception {
        // CPU 0.0% is valid boundary
        MetricIngestionRequest validZero = MetricIngestionRequest.builder()
                .applicationId(testApp.getId())
                .instanceId(testInstance.getId())
                .cpuUtilization(0.0)
                .memoryUtilization(10.0)
                .requestRate(0.0)
                .averageResponseTime(0.0)
                .errorRate(0.0)
                .build();

        mockMvc.perform(post("/api/v1/monitoring/metrics")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validZero)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.cpuUtilization").value(0.0));

        // CPU 100.0% is valid boundary
        MetricIngestionRequest validHundred = MetricIngestionRequest.builder()
                .applicationId(testApp.getId())
                .instanceId(testInstance.getId())
                .cpuUtilization(100.0)
                .memoryUtilization(99.0)
                .requestRate(500.0)
                .averageResponseTime(800.0)
                .errorRate(5.0)
                .build();

        mockMvc.perform(post("/api/v1/monitoring/metrics")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validHundred)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.cpuUtilization").value(100.0));

        // CPU 100.1% must fail validation
        MetricIngestionRequest invalidOver = MetricIngestionRequest.builder()
                .applicationId(testApp.getId())
                .instanceId(testInstance.getId())
                .cpuUtilization(100.1)
                .memoryUtilization(50.0)
                .requestRate(10.0)
                .averageResponseTime(50.0)
                .errorRate(0.0)
                .build();

        mockMvc.perform(post("/api/v1/monitoring/metrics")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidOver)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Edge Case: Evaluation with missing metrics returns NO_ACTION")
    void testMissingMetricsEvaluation() {
        policyRepository.save(ScalingPolicy.builder()
                .application(testApp)
                .minimumInstances(1)
                .maximumInstances(5)
                .desiredInstances(1)
                .cpuScaleOutThreshold(75.0)
                .cpuScaleInThreshold(30.0)
                .memoryScaleOutThreshold(80.0)
                .memoryScaleInThreshold(40.0)
                .requestRateThreshold(200.0)
                .cooldownSeconds(300)
                .enabled(true)
                .build());

        ScalingEvaluationRequest evalReq = ScalingEvaluationRequest.builder()
                .executionMode(ExecutionMode.MANUAL_APPROVAL)
                .build();

        // No metrics saved
        ScalingRecommendationResponse decision = scalingService.evaluateScaling(testApp.getId(), evalReq);
        assertThat(decision.getScalingAction()).isEqualTo(ScalingAction.NO_ACTION);
        assertThat(decision.getDecisionReason()).containsIgnoringCase("No recent telemetry");
    }

    @Test
    @DisplayName("Edge Case: Maximum capacity already reached prevents further scale-out")
    void testMaximumCapacityReached() {
        policyRepository.save(ScalingPolicy.builder()
                .application(testApp)
                .minimumInstances(1)
                .maximumInstances(1) // Already at max!
                .desiredInstances(1)
                .cpuScaleOutThreshold(70.0)
                .cpuScaleInThreshold(30.0)
                .memoryScaleOutThreshold(75.0)
                .memoryScaleInThreshold(35.0)
                .requestRateThreshold(100.0)
                .cooldownSeconds(300)
                .enabled(true)
                .build());

        metricRepository.save(ResourceMetric.builder()
                .application(testApp)
                .instance(testInstance)
                .cpuUtilization(95.0)
                .memoryUtilization(85.0)
                .requestRate(500.0)
                .averageResponseTime(200.0)
                .errorRate(0.0)
                .timestamp(LocalDateTime.now())
                .build());

        ScalingEvaluationRequest evalReq = ScalingEvaluationRequest.builder()
                .executionMode(ExecutionMode.MANUAL_APPROVAL)
                .build();

        ScalingRecommendationResponse decision = scalingService.evaluateScaling(testApp.getId(), evalReq);
        assertThat(decision.getScalingAction()).isEqualTo(ScalingAction.NO_ACTION);
        assertThat(decision.getDecisionReason()).containsIgnoringCase("Maximum");
    }

    @Test
    @DisplayName("Edge Case: Cooldown active suppresses reactive scaling")
    void testCooldownActiveSuppressesScaling() {
        policyRepository.save(ScalingPolicy.builder()
                .application(testApp)
                .minimumInstances(1)
                .maximumInstances(5)
                .desiredInstances(1)
                .cpuScaleOutThreshold(70.0)
                .cpuScaleInThreshold(30.0)
                .memoryScaleOutThreshold(75.0)
                .memoryScaleInThreshold(35.0)
                .requestRateThreshold(100.0)
                .cooldownSeconds(600) // 10 minutes
                .enabled(true)
                .build());

        // Insert a completed event from 1 minute ago
        eventRepository.save(ScalingEvent.builder()
                .application(testApp)
                .scalingAction(ScalingAction.SCALE_OUT)
                .previousInstanceCount(1)
                .newInstanceCount(2)
                .triggeringMetric("cpuUtilization")
                .triggeringMetricValue(88.0)
                .decisionReason("Scale out triggered")
                .executionMode(ExecutionMode.MANUAL_APPROVAL)
                .status(ScalingEventStatus.COMPLETED)
                .completedAt(LocalDateTime.now().minusMinutes(1))
                .build());

        // High CPU telemetry
        metricRepository.save(ResourceMetric.builder()
                .application(testApp)
                .instance(testInstance)
                .cpuUtilization(95.0)
                .memoryUtilization(85.0)
                .requestRate(400.0)
                .averageResponseTime(200.0)
                .errorRate(0.0)
                .timestamp(LocalDateTime.now())
                .build());

        ScalingEvaluationRequest evalReq = ScalingEvaluationRequest.builder()
                .executionMode(ExecutionMode.MANUAL_APPROVAL)
                .build();

        ScalingRecommendationResponse decision = scalingService.evaluateScaling(testApp.getId(), evalReq);
        assertThat(decision.getScalingAction()).isEqualTo(ScalingAction.NO_ACTION);
        assertThat(decision.getDecisionReason()).containsIgnoringCase("cooldown");
    }

    @Test
    @DisplayName("Edge Case: Duplicate approval rejected and unauthorized VIEWER forbidden")
    void testDuplicateApprovalAndUnauthorizedAccess() throws Exception {
        policyRepository.save(ScalingPolicy.builder()
                .application(testApp)
                .minimumInstances(1)
                .maximumInstances(5)
                .desiredInstances(1)
                .cpuScaleOutThreshold(70.0)
                .cpuScaleInThreshold(30.0)
                .memoryScaleOutThreshold(75.0)
                .memoryScaleInThreshold(35.0)
                .requestRateThreshold(100.0)
                .cooldownSeconds(300)
                .enabled(true)
                .build());

        metricRepository.save(ResourceMetric.builder()
                .application(testApp)
                .instance(testInstance)
                .cpuUtilization(95.0)
                .memoryUtilization(85.0)
                .requestRate(400.0)
                .averageResponseTime(200.0)
                .errorRate(0.0)
                .timestamp(LocalDateTime.now())
                .build());

        ScalingEvaluationRequest evalReq = ScalingEvaluationRequest.builder()
                .executionMode(ExecutionMode.MANUAL_APPROVAL)
                .build();

        // Evaluate to create recommendation
        ScalingRecommendationResponse decision = scalingService.evaluateScaling(testApp.getId(), evalReq);
        assertThat(decision.getId()).isNotNull();

        // VIEWER attempting approval must be forbidden
        mockMvc.perform(post("/api/v1/scaling/recommendations/" + decision.getId() + "/approve")
                        .header("Authorization", viewerToken))
                .andExpect(status().isForbidden());

        // ADMIN approves recommendation
        mockMvc.perform(post("/api/v1/scaling/recommendations/" + decision.getId() + "/approve")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // ADMIN attempts to approve ALREADY APPROVED recommendation -> fails
        mockMvc.perform(post("/api/v1/scaling/recommendations/" + decision.getId() + "/approve")
                        .header("Authorization", adminToken))
                .andExpect(status().isBadRequest());
    }
}
