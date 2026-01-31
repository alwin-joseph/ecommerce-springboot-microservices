package com.example.productservice.entity;

// import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.index.Indexed;

@RedisHash("product")  // ← Redis annotation instead of @Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Product implements java.io.Serializable {
    
    @Id
    private String id;
    
    @Indexed  // ← Create secondary index for queries
    private String name;
    
    private String description;
    
    private BigDecimal price;
    
    private Integer stockQuantity;
    
    @Indexed 
    private String category;
}
