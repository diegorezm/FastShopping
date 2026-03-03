package com.api.FastShopping.products.dtos;

import java.util.List;

public record PlaceOrderDTO(
        List<OrderItemDTO> items
) {
}
