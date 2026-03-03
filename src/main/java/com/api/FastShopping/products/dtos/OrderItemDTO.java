package com.api.FastShopping.products.dtos;

import java.util.UUID;

public record OrderItemDTO(
        UUID productId, int quantity
) {
}
