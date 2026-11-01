package com.project.autoscaling.application.dto;

import com.project.autoscaling.common.enums.ApplicationEnvironment;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateApplicationRequest {

    @Size(min = 2, max = 150, message = "Application name must be between 2 and 150 characters")
    private String applicationName;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    @Pattern(
            regexp = "^(https?|http)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]",
            message = "Application URL must be a valid HTTP or HTTPS URL"
    )
    private String applicationUrl;

    private ApplicationEnvironment environment;
}
