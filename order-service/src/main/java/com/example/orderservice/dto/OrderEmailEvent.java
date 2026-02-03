package com.example.orderservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Order Email Event DTO
 * 
 * This class represents the data sent to AWS Lambda for email notifications.
 * It contains all the information needed to generate an order confirmation email.
 * 
 * The object is serialized to JSON and sent as the Lambda function payload.
 * 
 * Example JSON payload:
 * {
 *   "orderId": "order-123-abc",
 *   "customerEmail": "john@example.com",
 *   "customerName": "John Doe",
 *   "productName": "Laptop",
 *   "quantity": 2,
 *   "totalPrice": 1999.98,
 *   "orderDate": "2024-01-31T10:30:00"
 * }
 * 
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderEmailEvent {
    
    /**
     * Unique order identifier
     * Example: "order-123-abc-456"
     */
    private String orderId;
    
    /**
     * Customer's email address (recipient)
     * Example: "customer@example.com"
     */
    private String customerEmail;
    
    /**
     * Customer's full name
     * Example: "John Doe"
     */
    private String customerName;
    
    /**
     * Product name/title
     * Example: "MacBook Pro 16-inch"
     */
    private String productName;
    
    /**
     * Product description (optional)
     * Example: "laptop for professionals"
     */
    private String productDescription;
    
    /**
     * Quantity ordered
     * Example: 2
     */
    private Integer quantity;
    
    /**
     * Unit price of the product
     * Example: 999.99
     */
    private BigDecimal unitPrice;
    
    /**
     * Total order price (quantity * unit price)
     * Example: 1999.98
     */
    private BigDecimal totalPrice;
    
    /**
     * Order status
     * Example: "PENDING", "CONFIRMED", "SHIPPED"
     */
    private String orderStatus;
    
    /**
     * Date and time when order was created
     * Example: "2024-01-31T10:30:00"
     */
    private LocalDateTime orderDate;
    
    /**
     * Product ID (for reference)
     * Example: "product-789"
     */
    private String productId;
    
    /**
     * User ID (for reference)
     * Example: "user-456"
     */
    private String userId;
}

/**
 * USAGE EXAMPLES:
 * ===============
 * 
 * 1. Creating with Builder Pattern 
 * 
 *    OrderEmailEvent event = OrderEmailEvent.builder()
 *        .orderId(order.getId())
 *        .customerEmail(user.getEmail())
 *        .customerName(user.getName())
 *        .productName(product.getName())
 *        .quantity(order.getQuantity())
 *        .totalPrice(order.getTotalPrice())
 *        .orderDate(LocalDateTime.now())
 *        .build();
 * 
 * 
 * 2. Creating with Constructor:
 * 
 *    OrderEmailEvent event = new OrderEmailEvent(
 *        "order-123",
 *        "john@example.com",
 *        "John Doe",
 *        "Laptop",
 *        "High performance",
 *        2,
 *        new BigDecimal("999.99"),
 *        new BigDecimal("1999.98"),
 *        "PENDING",
 *        LocalDateTime.now(),
 *        "product-456",
 *        "user-789"
 *    );
 * 
 * 
 * 3. Serializing to JSON:
 * 
 *    ObjectMapper mapper = new ObjectMapper();
 *    mapper.registerModule(new JavaTimeModule());
 *    String json = mapper.writeValueAsString(event);
 *    
 *    Result:
 *    {
 *      "orderId": "order-123",
 *      "customerEmail": "john@example.com",
 *      "customerName": "John Doe",
 *      ...
 *    }
 * 
 * 
 * 4. What Lambda Receives:
 * 
 *    The Lambda function will receive this JSON in the event parameter:
 *    
 *    exports.handler = async (event) => {
 *        console.log('Order ID:', event.orderId);
 *        console.log('Customer:', event.customerName);
 *        console.log('Email:', event.customerEmail);
 *        
 *        // Send email using this data
 *        await sendEmail(event);
 *    };
 * 
 * 
 * IMPORTANT NOTES:
 * ================
 * 
 * 1. All fields are included in JSON by default (Lombok @Data)
 * 2. LocalDateTime is serialized as ISO-8601 string
 * 3. BigDecimal is serialized as number
 * 4. Null fields are included (can be excluded with @JsonInclude(NON_NULL))
 * 5. Field names in JSON match Java field names (camelCase)
 * 
 * 
 */
