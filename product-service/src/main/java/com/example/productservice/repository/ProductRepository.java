package com.example.productservice.repository;

import com.example.productservice.entity.Product;

import org.springframework.data.repository.CrudRepository;
// import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ProductRepository extends CrudRepository<Product, String> {
    // Spring Data Redis supports these query methods via @Indexed fields
    List<Product> findByCategory(String category);
    // List<Product> findByStockQuantityGreaterThan(Integer quantity);
}
