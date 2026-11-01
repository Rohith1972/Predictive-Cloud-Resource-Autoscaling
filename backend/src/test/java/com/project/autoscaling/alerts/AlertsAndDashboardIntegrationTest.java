package com.project.autoscaling.alerts;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.autoscaling.alerts.dto.AlertResponse;
import com.project.autoscaling.alerts.repository.AlertRepository;
import com.project.autoscaling.alerts.service.AlertService;
import com.project.autoscaling.application.entity.Application;
import com.project.autoscaling.application.repository.ApplicationRepository;
import com.project.autoscaling.common.enums.AlertSeverity;
import com.project.autoscaling.common.enums.AlertStatus;
import com.project.autoscaling.common.enums.AlertType;
import com.project.autoscaling.common.enums.ApplicationEnvironment;
import com.project.autoscaling.common.enums.ApplicationStatus;
import com.project.autoscaling.common.enums.InstanceStatus;
import com.project.autoscaling.common.enums.ProviderType;
import com.project.autoscaling.common.enums.UserRole;
import com.project.autoscaling.infrastructure.entity.Instance;
import com.project.autoscaling.infrastructure.repository.InstanceRepository;
import com.project.autoscaling.monitoring.entity.ResourceMetric;
import com.project.autoscaling.monitoring.repository.ResourceMetricRepository;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AlertsAndDashboardIntegrationTest {

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
    private AlertRepository alertRepository;

    @Autowired
    private AlertService alertService;

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
                .fullName("Admin User")
                .email("admin@alerts.com")
                .password(passwordEncoder.encode("Password123!"))
                .role(UserRole.ADMIN)
                .enabled(true)
                .build());
        adminToken = "Bearer " + jwtService.generateToken(new UserPrincipal(admin), Map.of("role", "ADMIN"));

        User viewer = userRepository.save(User.builder()
                .fullName("Viewer User")
                .email("viewer@alerts.com")
                .password(passwordEncoder.encode("Password123!"))
                .role(UserRole.VIEWER)
                .enabled(true)
                .build());
        viewerToken = "Bearer " + jwtService.generateToken(new UserPrincipal(viewer), Map.of("role", "VIEWER"));

        testApp = applicationRepository.save(Application.builder()
                .applicationName("Alerts Test App")
                .description("Application for alert and dashboard tests")
                .applicationUrl("https://alerts.test.com")
                .environment(ApplicationEnvironment.PRODUCTION)
                .status(ApplicationStatus.ACTIVE)
                .build());

        testInstance = instanceRepository.save(Instance.builder()
                .instanceIdentifier("i-alert-001")
                .instanceName("web-instance-01")
                .application(testApp)
                .instanceType("t3.medium")
                .cpuCores(2)
                .totalMemoryMb(4096L)
                .status(InstanceStatus.RUNNING)
                .provider(ProviderType.LOCAL_SIMULATED)
                .build());

        metricRepository.save(ResourceMetric.builder()
                .application(testApp)
                .instance(testInstance)
                .cpuUtilization(92.5)
                .memoryUtilization(78.0)
                .requestRate(350.0)
                .averageResponseTime(120.0)
                .errorRate(1.2)
                .activeRequests(50)
                .networkInBytes(1024000L)
                .networkOutBytes(2048000L)
                .timestamp(LocalDateTime.now())
                .build());
    }

    @Test
    @DisplayName("Should trigger alert and avoid duplicate active alerts for same condition")
    void testAlertGenerationAndDeduplication() {
        // Trigger high CPU alert
        AlertResponse alert1 = alertService.createAlertIfAbsent(
                testApp,
                testInstance,
                AlertType.HIGH_CPU,
                AlertSeverity.CRITICAL,
                "CPU breached critical threshold",
                "cpuUtilization",
                92.5,
                80.0
        );

        assertThat(alert1).isNotNull();
        assertThat(alert1.getId()).isNotNull();
        assertThat(alert1.getStatus()).isEqualTo(AlertStatus.ACTIVE);

        // Attempting to trigger duplicate active alert for same condition
        AlertResponse alert2 = alertService.createAlertIfAbsent(
                testApp,
                testInstance,
                AlertType.HIGH_CPU,
                AlertSeverity.CRITICAL,
                "Duplicate trigger attempt",
                "cpuUtilization",
                93.0,
                80.0
        );

        // Deduplication: should return null when duplicate condition already exists and not insert a new row
        assertThat(alert2).isNull();
        assertThat(alertRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should acknowledge and resolve alert successfully via ADMIN")
    void testAcknowledgeAndResolveAlert() throws Exception {
        AlertResponse alert = alertService.createAlertIfAbsent(
                testApp,
                testInstance,
                AlertType.HIGH_MEMORY,
                AlertSeverity.WARNING,
                "Memory warning alert",
                "memoryUtilization",
                82.0,
                80.0
        );

        // Acknowledge alert
        mockMvc.perform(patch("/api/v1/alerts/" + alert.getId() + "/acknowledge")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("ACKNOWLEDGED"));

        // Resolve alert
        mockMvc.perform(patch("/api/v1/alerts/" + alert.getId() + "/resolve")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("RESOLVED"))
                .andExpect(jsonPath("$.data.resolvedAt").isNotEmpty());
    }

    @Test
    @DisplayName("VIEWER should not be authorized to acknowledge or resolve alerts")
    void testViewerUnauthorizedToModifyAlerts() throws Exception {
        AlertResponse alert = alertService.createAlertIfAbsent(
                testApp,
                testInstance,
                AlertType.HIGH_LATENCY,
                AlertSeverity.WARNING,
                "High latency alert",
                "averageResponseTime",
                550.0,
                500.0
        );

        mockMvc.perform(patch("/api/v1/alerts/" + alert.getId() + "/acknowledge")
                        .header("Authorization", viewerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should retrieve dashboard overview with accurate aggregated statistics")
    void testDashboardOverview() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/overview")
                        .header("Authorization", viewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalApplications").value(1))
                .andExpect(jsonPath("$.data.activeApplications").value(1))
                .andExpect(jsonPath("$.data.totalInstances").value(1))
                .andExpect(jsonPath("$.data.runningInstances").value(1))
                .andExpect(jsonPath("$.data.averageCpuUtilization").value(92.5));
    }

    @Test
    @DisplayName("Should retrieve application dashboard snapshot and resource trends")
    void testApplicationDashboardAndTrends() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/application/" + testApp.getId())
                        .header("Authorization", viewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.application.id").value(testApp.getId()))
                .andExpect(jsonPath("$.data.capacity.runningInstances").value(1));

        mockMvc.perform(get("/api/v1/dashboard/resource-trends/" + testApp.getId())
                        .header("Authorization", viewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].cpuUtilization").value(92.5));
    }
}
