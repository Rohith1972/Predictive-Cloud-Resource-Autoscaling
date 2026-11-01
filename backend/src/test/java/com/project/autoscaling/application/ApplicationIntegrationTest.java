package com.project.autoscaling.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.autoscaling.application.dto.ApplicationStatusUpdateRequest;
import com.project.autoscaling.application.dto.CreateApplicationRequest;
import com.project.autoscaling.application.dto.UpdateApplicationRequest;
import com.project.autoscaling.application.entity.Application;
import com.project.autoscaling.application.repository.ApplicationRepository;
import com.project.autoscaling.common.enums.ApplicationEnvironment;
import com.project.autoscaling.common.enums.ApplicationStatus;
import com.project.autoscaling.common.enums.UserRole;
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
class ApplicationIntegrationTest {

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

    @BeforeEach
    void setUp() {
        instanceRepository.deleteAll();
        applicationRepository.deleteAll();
        userRepository.deleteAll();

        User admin = userRepository.save(User.builder()
                .fullName("Admin")
                .email("admin@test.com")
                .password(passwordEncoder.encode("Password123"))
                .role(UserRole.ADMIN)
                .enabled(true)
                .build());

        User viewer = userRepository.save(User.builder()
                .fullName("Viewer")
                .email("viewer@test.com")
                .password(passwordEncoder.encode("Password123"))
                .role(UserRole.VIEWER)
                .enabled(true)
                .build());

        adminToken = "Bearer " + jwtService.generateToken(new UserPrincipal(admin), Map.of("role", "ADMIN"));
        viewerToken = "Bearer " + jwtService.generateToken(new UserPrincipal(viewer), Map.of("role", "VIEWER"));
    }

    @Test
    void shouldAllowAdminToCreateApplication() throws Exception {
        CreateApplicationRequest request = CreateApplicationRequest.builder()
                .applicationName("Ecommerce Service")
                .description("Main backend API for ecommerce store")
                .applicationUrl("https://api.ecommerce.example.com")
                .environment(ApplicationEnvironment.PRODUCTION)
                .build();

        mockMvc.perform(post("/api/v1/applications")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.applicationName").value("Ecommerce Service"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    void shouldDenyViewerFromCreatingApplication() throws Exception {
        CreateApplicationRequest request = CreateApplicationRequest.builder()
                .applicationName("Forbidden Service")
                .description("Should not be created")
                .applicationUrl("https://forbidden.example.com")
                .environment(ApplicationEnvironment.DEVELOPMENT)
                .build();

        mockMvc.perform(post("/api/v1/applications")
                        .header("Authorization", viewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldPreventDuplicateApplicationNames() throws Exception {
        CreateApplicationRequest request = CreateApplicationRequest.builder()
                .applicationName("Duplicate App")
                .applicationUrl("https://app1.example.com")
                .environment(ApplicationEnvironment.TESTING)
                .build();

        mockMvc.perform(post("/api/v1/applications")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/applications")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void shouldUpdateApplicationAndToggleStatus() throws Exception {
        Application app = applicationRepository.save(Application.builder()
                .applicationName("Payment Gateway")
                .description("Handles payments")
                .applicationUrl("https://pay.example.com")
                .environment(ApplicationEnvironment.PRODUCTION)
                .status(ApplicationStatus.ACTIVE)
                .build());

        UpdateApplicationRequest updateRequest = UpdateApplicationRequest.builder()
                .description("Updated payment service description")
                .build();

        mockMvc.perform(put("/api/v1/applications/" + app.getId())
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.description").value("Updated payment service description"));

        // Toggle status to INACTIVE
        ApplicationStatusUpdateRequest statusReq = ApplicationStatusUpdateRequest.builder()
                .status(ApplicationStatus.INACTIVE)
                .build();

        mockMvc.perform(patch("/api/v1/applications/" + app.getId() + "/status")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("INACTIVE"));
    }

    @Test
    void shouldBlockDeletionWhenInstancesExist() throws Exception {
        Application app = applicationRepository.save(Application.builder()
                .applicationName("Core Service")
                .applicationUrl("https://core.example.com")
                .environment(ApplicationEnvironment.PRODUCTION)
                .status(ApplicationStatus.ACTIVE)
                .build());

        instanceRepository.save(Instance.builder()
                .instanceIdentifier("i-sim-001")
                .instanceName("core-node-1")
                .application(app)
                .instanceType("t3.medium")
                .cpuCores(2)
                .totalMemoryMb(4096L)
                .build());

        mockMvc.perform(delete("/api/v1/applications/" + app.getId())
                        .header("Authorization", adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("associated with it")));
    }
}
