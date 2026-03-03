package com.api.FastShopping.products.dtos;

import java.util.List;

public record CachedOrderPage(
        List<OrderResponseDTO> content,
        long totalElements,
        int pageNumber,
        int pageSize
) {
}
