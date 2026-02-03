package com.example.orderservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.lambda.LambdaClient;

/**
 * AWS Lambda Configuration
 * 
 * Configures the AWS Lambda client for invoking Lambda functions.
 * 
 * The Lambda client is used to send order notification events to
 * AWS Lambda functions (e.g., for sending emails).
 * 
 * Configuration:
 * - Uses AWS credentials from environment variables or IAM role
 * - Region can be configured via application.yml
 * 
 */
@Configuration
public class Awslambdaconfig {
    @Bean
    public LambdaClient lambdaClient() {
        return LambdaClient.builder()
                .region(Region.AP_SOUTH_1) 
                .build();
    }
}
