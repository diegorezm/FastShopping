package com.api.FastShopping.products.controllers;

import com.api.FastShopping.products.dtos.ProductDTO;
import com.api.FastShopping.products.models.Product;
import com.api.FastShopping.products.repositories.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {
    private final ProductRepository productRepository;

    @PostMapping
    public ResponseEntity<Product> createProduct(@RequestBody ProductDTO product) {
        var p = new Product(product);
        Product savedProduct = productRepository.save(p);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedProduct);
    }

    @GetMapping
    @Cacheable(value = "productSearch", key = "#name + '_' + #page + '_' + #size")
    @Transactional(readOnly = true)
    public ResponseEntity<Page<Product>> getProducts(
            @RequestParam(required = false) String name,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<Product> products;

        if (name != null && !name.trim().isEmpty()) {
            products = productRepository.findByNameIgnoreCase(name, pageable);
        } else {
            products = productRepository.findAll(pageable);
        }

        return ResponseEntity.ok(products);
    }

    // READ (Single Product by ID)
    @GetMapping("/{id}")
    @Cacheable(value = "products", key = "#id")
    @Transactional(readOnly = true)
    public ResponseEntity<Product> getProductById(@PathVariable String id) {
        var u = UUID.fromString(id);
        return productRepository.findById(u)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // UPDATE
    @PutMapping("/{id}")
    public ResponseEntity<Product> updateProduct(@PathVariable String id, @RequestBody ProductDTO productDetails) {
        var u = UUID.fromString(id);
        return productRepository.findById(u).map(existingProduct -> {
            existingProduct.setName(productDetails.name());
            existingProduct.setPrice(productDetails.price());
            Product updatedProduct = productRepository.save(existingProduct);
            return ResponseEntity.ok(updatedProduct);
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable String id) {
        var u = UUID.fromString(id);
        try {
            productRepository.deleteById(u);
            return ResponseEntity.noContent().build();
        } catch (EmptyResultDataAccessException e) {
            return ResponseEntity.notFound().build();
        }
    }
}