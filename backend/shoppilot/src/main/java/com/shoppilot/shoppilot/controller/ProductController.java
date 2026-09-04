package com.shoppilot.shoppilot.controller;

import com.shoppilot.shoppilot.dto.ApiResponse;
import com.shoppilot.shoppilot.exception.ResourceNotFoundException;
import com.shoppilot.shoppilot.model.Product;
import com.shoppilot.shoppilot.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductRepository productRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Product>>> getProducts(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search) {

        List<Product> products;
        if (search != null && !search.isBlank()) {
            products = productRepository.searchProducts(search.trim());
        } else if (category != null && !category.isBlank() && !category.equalsIgnoreCase("all")) {
            products = productRepository.findByCategoryIgnoreCaseAndActiveTrue(category.trim());
        } else {
            products = productRepository.findByActiveTrue();
        }

        return ResponseEntity.ok(ApiResponse.ok(products));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Product>> getProductById(@PathVariable String id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
        return ResponseEntity.ok(ApiResponse.ok(product));
    }
}
