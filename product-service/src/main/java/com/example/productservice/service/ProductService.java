package com.example.productservice.service;

import com.example.productservice.dto.ProductRequest;
import com.example.productservice.dto.ProductResponse;

import java.util.List;

public interface ProductService {
    ProductResponse createProduct(ProductRequest request);
    ProductResponse getProductById(String id);
    List<ProductResponse> getAllProducts();
    ProductResponse updateProduct(String id, ProductRequest request);
    void deleteProduct(String id);
    boolean checkAvailability(String productId, Integer quantity);
    void reduceStock(String productId, Integer quantity);
}
