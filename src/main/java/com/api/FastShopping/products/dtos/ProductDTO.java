package com.api.FastShopping.products.dtos;

import java.math.BigDecimal;

public record ProductDTO(
        String name,
        BigDecimal price
) {
}
