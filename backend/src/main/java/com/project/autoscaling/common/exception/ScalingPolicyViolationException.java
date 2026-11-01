package com.project.autoscaling.common.exception;

public class ScalingPolicyViolationException extends RuntimeException {
    public ScalingPolicyViolationException(String message) {
        super(message);
    }
}
