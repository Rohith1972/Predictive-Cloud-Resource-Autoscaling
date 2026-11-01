package com.project.autoscaling.workload;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.autoscaling.application.entity.Application;
import com.project.autoscaling.application.repository.ApplicationRepository;
import com.project.autoscaling.common.enums.ApplicationEnvironment;
import com.project.autoscaling.common.enums.ApplicationStatus;
import com.project.autoscaling.common.enums.UserRole;
import com.project.autoscaling.security.JwtService;
import com.project.autoscaling.security.UserPrincipal;
import com.project.autoscaling.user.entity.User;
import com.project.autoscaling.user.repository.UserRepository;
import com.project.autoscaling.workload.dto.WorkloadRecordRequest;
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
class WorkloadIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private com.project.autoscaling.test.TestDatabaseCleaner cleaner;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private String userToken;
    private Application testApp;

    @BeforeEach
    void setUp() {
        cleaner.clean();

        User user = userRepository.save(User.builder()
                .fullName("Workload User")
                .email("workload@test.com")
                .password(passwordEncoder.encode("Password123"))
                .role(UserRole.VIEWER)
                .enabled(true)
                .build());

        userToken = "Bearer " + jwtService.generateToken(new UserPrincipal(user), Map.of("role", "VIEWER"));

        testApp = applicationRepository.save(Application.builder()
                .applicationName("Payment Ingestion Service")
                .applicationUrl("https://payments.internal")
                .environment(ApplicationEnvironment.PRODUCTION)
                .status(ApplicationStatus.ACTIVE)
                .build());
    }

    @Test
    void shouldRecordWorkloadSuccessfully() throws Exception {
        WorkloadRecordRequest request = WorkloadRecordRequest.builder()
                .applicationId(testApp.getId())
                .timestamp(LocalDateTime.now())
                .requestCount(15000L)
                .requestsPerSecond(250.0)
                .successfulRequests(14950L)
                .failedRequests(50L)
                .averageLatency(32.4)
                .p95Latency(65.0)
                .p99Latency(120.0)
                .build();

        mockMvc.perform(post("/api/v1/workload/records")
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.requestsPerSecond").value(250.0))
                .andExpect(jsonPath("$.data.successfulRequests").value(14950));
    }

    @Test
    void shouldDetectTrafficSurgeTrend() throws Exception {
        // Record earlier observation
        WorkloadRecordRequest w1 = WorkloadRecordRequest.builder()
                .applicationId(testApp.getId())
                .timestamp(LocalDateTime.now().minusMinutes(5))
                .requestCount(5000L)
                .requestsPerSecond(100.0)
                .successfulRequests(5000L)
                .failedRequests(0L)
                .averageLatency(20.0)
                .build();

        // Record surge observation (+100% jump)
        WorkloadRecordRequest w2 = WorkloadRecordRequest.builder()
                .applicationId(testApp.getId())
                .timestamp(LocalDateTime.now())
                .requestCount(12000L)
                .requestsPerSecond(200.0)
                .successfulRequests(11980L)
                .failedRequests(20L)
                .averageLatency(45.0)
                .build();

        mockMvc.perform(post("/api/v1/workload/records")
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(w1)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/workload/records")
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(w2)))
                .andExpect(status().isCreated());

        // Trend endpoint
        mockMvc.perform(get("/api/v1/workload/" + testApp.getId())
                        .header("Authorization", userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.trendDirection").value("TRAFFIC_SPIKE"))
                .andExpect(jsonPath("$.data.currentRequestsPerSecond").value(200.0));

        // Summary endpoint
        mockMvc.perform(get("/api/v1/workload/" + testApp.getId() + "/summary")
                        .header("Authorization", userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalRequests").value(17000))
                .andExpect(jsonPath("$.data.totalObservations").value(2));
    }
}
