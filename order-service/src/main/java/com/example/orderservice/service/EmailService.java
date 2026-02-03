package com.example.orderservice.service;

import com.example.orderservice.dto.OrderEmailEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;
import software.amazon.awssdk.services.lambda.model.InvocationType;

/**
 * Email Service
 * 
 * Handles sending order confirmation emails by invoking AWS Lambda functions.
 * All email operations are asynchronous to avoid blocking the order creation flow.
 * 
 * Flow:
 * 1. Order Service calls sendOrderConfirmationEmail()
 * 2. Method returns immediately (async)
 * 3. Email sending happens in background thread
 * 4. Lambda function is invoked
 * 5. Lambda sends email via SES
 * 
 * @author Your Name
 * @version 1.0
 */
@Service
@Slf4j
public class EmailService {
    
    @Autowired
    private LambdaClient lambdaClient;
    
    private final ObjectMapper objectMapper;
    
    /**
     * AWS Lambda function name from application.yml
     * Example: "send-order-email"
     */
    @Value("${aws.lambda.email-function-name}")
    private String emailFunctionName;
    
    /**
     * Constructor
     * Configures ObjectMapper to handle Java 8 date/time types
     */
    public EmailService() {
        this.objectMapper = new ObjectMapper();
        // Register module to serialize LocalDateTime, LocalDate, etc.
        this.objectMapper.registerModule(new JavaTimeModule());
    }
    
    /**
     * Send Order Confirmation Email (Asynchronously)
     * 
     * This method invokes AWS Lambda function to send email.
     * It runs in a background thread and doesn't block the order creation.
     * 
     * Process:
     * 1. Serialize OrderEmailEvent to JSON
     * 2. Create Lambda InvokeRequest
     * 3. Invoke Lambda with EVENT invocation type (async)
     * 4. Log success/failure
     * 
     * @param event Order email event containing all email data
     * 
     * @Async annotation makes this method run in background thread
     * "taskExecutor" refers to the bean name in AsyncConfig
     */
    @Async("taskExecutor")
    public void sendOrderConfirmationEmail(OrderEmailEvent event) {
        
        log.info("Starting async email send for order: {}", event.getOrderId());
        
        try {
            // Step 1: Convert event to JSON string
            String jsonPayload = objectMapper.writeValueAsString(event);
            
            log.debug("Lambda payload: {}", jsonPayload);
            
            // Step 2: Create Lambda invoke request
            InvokeRequest invokeRequest = InvokeRequest.builder()
                    .functionName(emailFunctionName)
                    .payload(SdkBytes.fromUtf8String(jsonPayload))
                    .invocationType(InvocationType.EVENT)  // Async invocation
                    .build();
            
            // Step 3: Invoke Lambda function
            InvokeResponse response = lambdaClient.invoke(invokeRequest);
            
            // Step 4: Check response
            int statusCode = response.statusCode();
            
            if (statusCode == 202) {
                // 202 = Accepted (Lambda invoked successfully for async call)
                log.info("Email Lambda invoked successfully for order: {} (Status: {})", 
                        event.getOrderId(), statusCode);
            } else {
                // Unexpected status code
                log.warn("Email Lambda returned unexpected status for order: {} (Status: {})", 
                        event.getOrderId(), statusCode);
            }
            
        } catch (Exception e) {
            // Log error but don't fail the order
            log.error("Failed to invoke email Lambda for order: {}", event.getOrderId(), e);
            
            // In production, you might want to:
            // 1. Save failed email to database for retry
            // 2. Publish to DLQ (Dead Letter Queue)
            // 3. Send alert to monitoring system
        }
    }
    
    /**
     * Send Order Confirmation Email (Synchronously)
     * 
     * Alternative method that waits for Lambda to complete.
     * Use this if you need to know immediately if email was sent.
     * 
     * WARNING: This will slow down order creation API!
     * Only use for critical notifications where you need confirmation.
     * 
     * @param event Order email event
     * @return true if email was sent successfully, false otherwise
     */
    public boolean sendOrderConfirmationEmailSync(OrderEmailEvent event) {
        
        log.info("Starting synchronous email send for order: {}", event.getOrderId());
        
        try {
            String jsonPayload = objectMapper.writeValueAsString(event);
            
            // Use REQUEST_RESPONSE instead of EVENT for synchronous call
            InvokeRequest invokeRequest = InvokeRequest.builder()
                    .functionName(emailFunctionName)
                    .payload(SdkBytes.fromUtf8String(jsonPayload))
                    .invocationType(InvocationType.REQUEST_RESPONSE)  // Sync
                    .build();
            
            InvokeResponse response = lambdaClient.invoke(invokeRequest);
            
            int statusCode = response.statusCode();
            
            if (statusCode == 200) {
                log.info("Email sent successfully for order: {}", event.getOrderId());
                
                // You can also read the response payload
                String responsePayload = response.payload().asUtf8String();
                log.debug("Lambda response: {}", responsePayload);
                
                return true;
            } else {
                log.error("Email failed for order: {} (Status: {})", 
                        event.getOrderId(), statusCode);
                return false;
            }
            
        } catch (Exception e) {
            log.error("Failed to send email for order: {}", event.getOrderId(), e);
            return false;
        }
    }
    
    /**
     * Test Lambda Connection
     * 
     * Utility method to test if Lambda function exists and is accessible.
     * Call this during application startup or health checks.
     * 
     * @return true if Lambda function is accessible
     */
    public boolean testLambdaConnection() {
        try {
            // Create a test payload
            OrderEmailEvent testEvent = OrderEmailEvent.builder()
                    .orderId("test-order")
                    .customerEmail("test@example.com")
                    .customerName("Test User")
                    .productName("Test Product")
                    .build();
            
            String jsonPayload = objectMapper.writeValueAsString(testEvent);
            
            // Try to invoke (with dry-run if available, otherwise real invoke)
            InvokeRequest request = InvokeRequest.builder()
                    .functionName(emailFunctionName)
                    .payload(SdkBytes.fromUtf8String(jsonPayload))
                    .invocationType(InvocationType.DRY_RUN)  // Test only
                    .build();
            
            lambdaClient.invoke(request);
            
            log.info("Lambda function '{}' is accessible", emailFunctionName);
            return true;
            
        } catch (Exception e) {
            log.error("Lambda function '{}' is NOT accessible: {}", 
                    emailFunctionName, e.getMessage());
            return false;
        }
    }
}