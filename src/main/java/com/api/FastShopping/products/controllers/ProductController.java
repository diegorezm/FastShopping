package com.api.FastShopping.products.controllers;

import com.api.FastShopping.products.dtos.CachedProductPage;
import com.api.FastShopping.products.dtos.ProductDTO;
import com.api.FastShopping.products.events.WriteEventProducer;
import com.api.FastShopping.products.models.Product;
import com.api.FastShopping.products.services.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {
    private final WriteEventProducer producer;
    private final ProductService productService;

    @PostMapping
    public ResponseEntity<Product> createProduct(@RequestBody ProductDTO product) {
        Product saved = productService.create(product);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping
    public ResponseEntity<CachedProductPage> getProducts(
            @RequestParam(required = false) String name,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        var products = productService.findAll(name, page, size);
        return ResponseEntity.ok(products);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> getProductById(@PathVariable String id) {
        return productService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, String>> updateProduct(
            @PathVariable String id,
            @RequestBody ProductDTO dto) {
        producer.publishProductUpdate(id, dto);
        return ResponseEntity.accepted().body(Map.of(
                "status", "QUEUED",
                "message", "Product update is being processed"
        ));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteProduct(@PathVariable String id) {
        producer.publishProductDelete(id);
        return ResponseEntity.accepted().body(Map.of(
                "status", "QUEUED",
                "message", "Product deletion is being processed"
        ));
    }
}