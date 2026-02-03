package com.example.orderservice.client;

import com.example.orderservice.dto.ProductResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Feign Client for Product Service
 * This interface enables communication with Product Service using OpenFeign
 * The name "product-service" matches the spring.application.name in Product Service
 */
@FeignClient(name = "product-service")
public interface ProductClient {
    
    @GetMapping("/api/products/{id}")
    ProductResponse getProductById(@PathVariable("id") String id);
    
    @GetMapping("/api/products/{id}/availability")
    Boolean checkAvailability(@PathVariable("id") String id, @RequestParam("quantity") Integer quantity);
    
    @PostMapping("/api/products/{id}/reduce-stock")
    void reduceStock(@PathVariable("id") String id, @RequestParam("quantity") Integer quantity);
}
