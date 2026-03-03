package com.api.FastShopping.products.dtos;

import com.api.FastShopping.products.models.Product;

import java.util.List;

public record CachedProductPage(
        List<Product> content,
        long totalElements,
        int pageNumber,
        int pageSize
) {
}
