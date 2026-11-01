package com.project.autoscaling.monitoring;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.autoscaling.application.entity.Application;
import com.project.autoscaling.application.repository.ApplicationRepository;
import com.project.autoscaling.common.enums.ApplicationEnvironment;
import com.project.autoscaling.common.enums.ApplicationStatus;
import com.project.autoscaling.common.enums.UserRole;
import com.project.autoscaling.infrastructure.entity.Instance;
import com.project.autoscaling.infrastructure.repository.InstanceRepository;
import com.project.autoscaling.monitoring.dto.MetricIngestionRequest;
import com.project.autoscaling.monitoring.repository.ResourceMetricRepository;
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
class MonitoringIntegrationTest {

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
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private String userToken;
    private Application testApp;
    private Instance testInstance;

    @BeforeEach
    void setUp() {
        cleaner.clean();

        User user = userRepository.save(User.builder()
                .fullName("Monitor User")
                .email("monitor@test.com")
                .password(passwordEncoder.encode("Password123"))
                .role(UserRole.VIEWER)
                .enabled(true)
                .build());

        userToken = "Bearer " + jwtService.generateToken(new UserPrincipal(user), Map.of("role", "VIEWER"));

        testApp = applicationRepository.save(Application.builder()
                .applicationName("Checkout Service")
                .applicationUrl("https://checkout.internal")
                .environment(ApplicationEnvironment.PRODUCTION)
                .status(ApplicationStatus.ACTIVE)
                .build());

        testInstance = instanceRepository.save(Instance.builder()
                .instanceIdentifier("i-checkout-1")
                .instanceName("checkout-node-1")
                .application(testApp)
                .instanceType("c5.xlarge")
                .cpuCores(4)
                .totalMemoryMb(8192L)
                .build());
    }

    @Test
    void shouldIngestTelemetryMetricSuccessfully() throws Exception {
        MetricIngestionRequest request = MetricIngestionRequest.builder()
                .applicationId(testApp.getId())
                .instanceId(testInstance.getId())
                .timestamp(LocalDateTime.now())
                .cpuUtilization(65.5)
                .memoryUtilization(72.0)
                .requestRate(350.0)
                .averageResponseTime(45.2)
                .errorRate(0.5)
                .activeRequests(50)
                .networkInBytes(1024000L)
                .networkOutBytes(2048000L)
                .build();

        mockMvc.perform(post("/api/v1/monitoring/metrics")
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.cpuUtilization").value(65.5))
                .andExpect(jsonPath("$.data.memoryUtilization").value(72.0));
    }

    @Test
    void shouldRejectInvalidMetricValues() throws Exception {
        MetricIngestionRequest request = MetricIngestionRequest.builder()
                .applicationId(testApp.getId())
                .instanceId(testInstance.getId())
                .cpuUtilization(150.0) // Invalid: > 100
                .memoryUtilization(-5.0) // Invalid: < 0
                .requestRate(-10.0) // Invalid: negative
                .averageResponseTime(20.0)
                .errorRate(1.0)
                .build();

        mockMvc.perform(post("/api/v1/monitoring/metrics")
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void shouldRetrieveLatestMetricsAndAggregates() throws Exception {
        // Ingest two metrics
        MetricIngestionRequest m1 = MetricIngestionRequest.builder()
                .applicationId(testApp.getId())
                .instanceId(testInstance.getId())
                .timestamp(LocalDateTime.now().minusMinutes(5))
                .cpuUtilization(40.0)
                .memoryUtilization(50.0)
                .requestRate(100.0)
                .averageResponseTime(30.0)
                .errorRate(0.0)
                .build();

        MetricIngestionRequest m2 = MetricIngestionRequest.builder()
                .applicationId(testApp.getId())
                .instanceId(testInstance.getId())
                .timestamp(LocalDateTime.now())
                .cpuUtilization(80.0)
                .memoryUtilization(70.0)
                .requestRate(200.0)
                .averageResponseTime(50.0)
                .errorRate(2.0)
                .build();

        mockMvc.perform(post("/api/v1/monitoring/metrics")
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(m1)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/monitoring/metrics")
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(m2)))
                .andExpect(status().isCreated());

        // Get latest
        mockMvc.perform(get("/api/v1/monitoring/latest/" + testApp.getId())
                        .header("Authorization", userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.cpuUtilization").value(80.0));

        // Get summary
        mockMvc.perform(get("/api/v1/monitoring/summary/" + testApp.getId())
                        .header("Authorization", userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currentCpuUtilization").value(80.0));
    }
}
