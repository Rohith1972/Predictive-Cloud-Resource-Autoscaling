package com.project.autoscaling.application.dto;

import com.project.autoscaling.application.entity.Application;
import com.project.autoscaling.common.enums.ApplicationEnvironment;
import com.project.autoscaling.common.enums.ApplicationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationResponse {
    private Long id;
    private String applicationName;
    private String description;
    private String applicationUrl;
    private ApplicationEnvironment environment;
    private ApplicationStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ApplicationResponse fromEntity(Application app) {
        return ApplicationResponse.builder()
                .id(app.getId())
                .applicationName(app.getApplicationName())
                .description(app.getDescription())
                .applicationUrl(app.getApplicationUrl())
                .environment(app.getEnvironment())
                .status(app.getStatus())
                .createdAt(app.getCreatedAt())
                .updatedAt(app.getUpdatedAt())
                .build();
    }
}
