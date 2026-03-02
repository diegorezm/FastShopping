package com.api.FastShopping.products.controllers;

import com.api.FastShopping.products.dtos.ProductDTO;
import com.api.FastShopping.products.models.Product;
import com.api.FastShopping.products.repositories.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<Page<Product>> getProducts(
            @RequestParam(required = false) String name,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<Product> products;

        if (name != null && !name.trim().isEmpty()) {
            products = productRepository.findByNameContainingIgnoreCase(name, pageable);
        } else {
            products = productRepository.findAll(pageable);
        }

        return ResponseEntity.ok(products);
    }

    // READ (Single Product by ID)
    @GetMapping("/{id}")
    public ResponseEntity<Product> getProductById(@PathVariable String id) {
        return productRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // UPDATE
    @PutMapping("/{id}")
    public ResponseEntity<Product> updateProduct(@PathVariable String id, @RequestBody ProductDTO productDetails) {
        return productRepository.findById(id).map(existingProduct -> {
            existingProduct.setName(productDetails.name());
            existingProduct.setPrice(productDetails.price());
            Product updatedProduct = productRepository.save(existingProduct);
            return ResponseEntity.ok(updatedProduct);
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable String id) {
        if (productRepository.existsById(id)) {
            productRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}