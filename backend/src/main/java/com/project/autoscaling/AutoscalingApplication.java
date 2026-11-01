package com.project.autoscaling;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AutoscalingApplication {

    public static void main(String[] args) {
        SpringApplication.run(AutoscalingApplication.class, args);
    }
}
