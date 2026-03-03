package com.api.FastShopping.products.services;

import com.api.FastShopping.products.dtos.ProductDTO;
import com.api.FastShopping.products.models.Product;
import com.api.FastShopping.products.repositories.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CacheManager cacheManager;

    @Transactional
    @CachePut(value = "products", key = "#result.id", unless = "#result == null")
    public Product create(ProductDTO productDTO) {
        Product product = new Product(productDTO);
        return productRepository.save(product);
    }

    @Transactional(readOnly = true)
    public Page<Product> findAll(String name, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        if (name != null && !name.trim().isEmpty()) {
            return productRepository.fuzzySearch(name, pageable);
        }

        return productRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "products", key = "#id", unless = "#result == null")
    public Optional<Product> findById(String id) {
        UUID uuid = UUID.fromString(id);
        return productRepository.findById(uuid);
    }

    @Transactional
    @CachePut(value = "products", key = "#result.id", unless = "#result == null")
    public Product update(String id, ProductDTO productDTO) {
        UUID uuid = UUID.fromString(id);
        return productRepository.findById(uuid)
                .map(existing -> {
                    existing.setName(productDTO.name());
                    existing.setPrice(productDTO.price());
                    return productRepository.save(existing);
                })
                .orElse(null);
    }

    @Transactional
    @CacheEvict(value = "products", key = "#id", condition = "#result == true")
    public boolean delete(String id) {
        UUID uuid = UUID.fromString(id);

        List<UUID> affectedOrderIds = productRepository.findOrderIdsByProductId(uuid);
        Cache ordersCache = cacheManager.getCache("orders");
        if (ordersCache != null) {
            affectedOrderIds.forEach(ordersCache::evict);
        }

        try {
            productRepository.deleteById(uuid);
            return true;
        } catch (EmptyResultDataAccessException e) {
            return false;
        }
    }
}