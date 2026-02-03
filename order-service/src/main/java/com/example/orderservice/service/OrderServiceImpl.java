package com.example.orderservice.service;

import com.example.orderservice.client.ProductClient;
import com.example.orderservice.client.UserClient;
import com.example.orderservice.dto.OrderEmailEvent;
import com.example.orderservice.dto.OrderRequest;
import com.example.orderservice.dto.OrderResponse;
import com.example.orderservice.dto.ProductResponse;
import com.example.orderservice.dto.UserResponse;
import com.example.orderservice.entity.Order;
import com.example.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {
    
    private final OrderRepository orderRepository;
    private final ProductClient productClient;  // Feign Client
    private final UserClient userClient;        // Feign Client
    private final EmailService emailService;    // AWS Lambda Email Service
    
    @Override
    @Transactional
    public OrderResponse createOrder(OrderRequest request) {
        log.info("Creating order for user: {} and product: {}", request.getUserId(), request.getProductId());
        
        // Step 1: Validate user exists using Feign Client
        UserResponse user;
        try {
            user = userClient.getUserById(request.getUserId());
            log.info("User validated: {}", user.getName());
        } catch (Exception e) {
            log.error("User not found: {}", request.getUserId());
            throw new RuntimeException("User not found with id: " + request.getUserId());
        }
        
        // Step 2: Get product details and check availability using Feign Client
        ProductResponse product;
        try {
            product = productClient.getProductById(request.getProductId());
            log.info("Product found: {}", product.getName());
        } catch (Exception e) {
            log.error("Product not found: {}", request.getProductId());
            throw new RuntimeException("Product not found with id: " + request.getProductId());
        }
        
        // Step 3: Check product availability using Feign Client
        Boolean isAvailable = productClient.checkAvailability(request.getProductId(), request.getQuantity());
        if (!isAvailable) {
            log.error("Insufficient stock for product: {}", request.getProductId());
            throw new RuntimeException("Insufficient stock for product: " + product.getName());
        }
        
        // Step 4: Calculate total price
        BigDecimal totalPrice = product.getPrice().multiply(BigDecimal.valueOf(request.getQuantity()));
        
        // Step 5: Create order
        Order order = new Order();
        order.setUserId(request.getUserId());
        order.setProductId(request.getProductId());
        order.setQuantity(request.getQuantity());
        order.setTotalPrice(totalPrice);
        order.setStatus(Order.OrderStatus.PENDING);
        order.setOrderDate(LocalDateTime.now());
        
        Order savedOrder = orderRepository.save(order);
        log.info("Order created with id: {}", savedOrder.getId());
        
        // Step 6: Reduce stock using Feign Client
        try {
            productClient.reduceStock(request.getProductId(), request.getQuantity());
            log.info("Stock reduced for product: {}", request.getProductId());
            
            // Update order status to CONFIRMED
            savedOrder.setStatus(Order.OrderStatus.CONFIRMED);
            orderRepository.save(savedOrder);
            
            // Step 7: Send order confirmation email asynchronously via AWS Lambda
            sendOrderConfirmationEmail(savedOrder, user, product);
            
        } catch (Exception e) {
            log.error("Failed to reduce stock, cancelling order", e);
            savedOrder.setStatus(Order.OrderStatus.CANCELLED);
            orderRepository.save(savedOrder);
            throw new RuntimeException("Failed to process order: " + e.getMessage());
        }
        
        return mapToResponse(savedOrder, user, product);
    }
    
    /**
     * Send order confirmation email via AWS Lambda
     * 
     * This method prepares the email event and invokes Lambda asynchronously.
     * The email is sent in the background and doesn't block the order creation flow.
     * 
     * @param order Saved order entity
     * @param user User details
     * @param product Product details
     */
    private void sendOrderConfirmationEmail(Order order, UserResponse user, ProductResponse product) {
        try {
            // Build email event with all necessary data
            OrderEmailEvent emailEvent = OrderEmailEvent.builder()
                    .orderId(order.getId().toString())
                    .customerEmail(user.getEmail())
                    .customerName(user.getName())
                    .productName(product.getName())
                    .productDescription(product.getDescription())
                    .quantity(order.getQuantity())
                    .unitPrice(product.getPrice())
                    .totalPrice(order.getTotalPrice())
                    .orderStatus(order.getStatus().name())
                    .orderDate(order.getOrderDate())
                    .productId(product.getId().toString())
                    .userId(user.getId().toString())
                    .build();
            
            // Send email asynchronously (doesn't block)
            emailService.sendOrderConfirmationEmail(emailEvent);
            
            log.info("Order confirmation email triggered for order: {}", order.getId());
            
        } catch (Exception e) {
            // Log error but don't fail the order
            // Email failure should not prevent order creation
            log.error("Failed to trigger order confirmation email for order: {}", order.getId(), e);
        }
    }
    
    @Override
    public OrderResponse getOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + id));
        
        // Fetch user and product details using Feign Clients
        UserResponse user = userClient.getUserById(order.getUserId());
        ProductResponse product = productClient.getProductById(order.getProductId());
        
        return mapToResponse(order, user, product);
    }
    
    @Override
    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAll().stream()
                .map(order -> {
                    UserResponse user = userClient.getUserById(order.getUserId());
                    ProductResponse product = productClient.getProductById(order.getProductId());
                    return mapToResponse(order, user, product);
                })
                .collect(Collectors.toList());
    }
    
    @Override
    public List<OrderResponse> getOrdersByUserId(Long userId) {
        return orderRepository.findByUserId(userId).stream()
                .map(order -> {
                    UserResponse user = userClient.getUserById(order.getUserId());
                    ProductResponse product = productClient.getProductById(order.getProductId());
                    return mapToResponse(order, user, product);
                })
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional
    public OrderResponse updateOrderStatus(Long id, Order.OrderStatus status) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + id));
        
        order.setStatus(status);
        Order updatedOrder = orderRepository.save(order);
        log.info("Order status updated to: {} for order id: {}", status, id);
        
        UserResponse user = userClient.getUserById(order.getUserId());
        ProductResponse product = productClient.getProductById(order.getProductId());
        
        return mapToResponse(updatedOrder, user, product);
    }
    
    @Override
    @Transactional
    public void cancelOrder(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + id));
        
        if (order.getStatus() == Order.OrderStatus.DELIVERED) {
            throw new RuntimeException("Cannot cancel delivered order");
        }
        
        order.setStatus(Order.OrderStatus.CANCELLED);
        orderRepository.save(order);
        log.info("Order cancelled with id: {}", id);
    }
    
    private OrderResponse mapToResponse(Order order, UserResponse user, ProductResponse product) {
        OrderResponse response = new OrderResponse();
        response.setId(order.getId());
        response.setUserId(order.getUserId());
        response.setUserName(user.getName());
        response.setProductId(order.getProductId());
        response.setProductName(product.getName());
        response.setQuantity(order.getQuantity());
        response.setTotalPrice(order.getTotalPrice());
        response.setStatus(order.getStatus());
        response.setOrderDate(order.getOrderDate());
        return response;
    }
}
