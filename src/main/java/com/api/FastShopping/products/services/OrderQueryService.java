package com.api.FastShopping.products.services;

import com.api.FastShopping.products.dtos.CachedOrderPage;
import com.api.FastShopping.products.dtos.OrderItemResponseDTO;
import com.api.FastShopping.products.dtos.OrderResponseDTO;
import com.api.FastShopping.products.models.Order;
import com.api.FastShopping.products.repositories.OrderRepository;
import com.api.FastShopping.products.repositories.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderQueryService {
    private final OrderRepository orderRepository;

    // proxy
    @Autowired
    @Lazy
    private OrderQueryService self;

    @Transactional(readOnly = true)
    @Cacheable(value = "orderPages", key = "#page + '_' + #size")
    public CachedOrderPage findAll(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        List<UUID> ids = orderRepository.findPagedIds(pageable);
        if (ids.isEmpty()) {
            return new CachedOrderPage(List.of(), 0L, page, size);
        }

        List<Order> orders = orderRepository.findWithItemsByIds(ids);
        Map<UUID, Order> orderMap = orders.stream()
                .collect(Collectors.toMap(Order::getId, o -> o));

        List<OrderResponseDTO> sorted = ids.stream()
                .map(orderMap::get)
                .filter(Objects::nonNull)
                .map(this::toResponse)
                .toList();

        // calling this from the proxy so i can get the cached count
        long total = self.getCachedOrderCount();
        return new CachedOrderPage(sorted, total, page, size);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "orders", key = "#id", unless = "#result == null")
    public OrderResponseDTO findById(UUID id) {
        Order order = orderRepository.findWithItemsById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Order not found"
                ));
        return toResponse(order);
    }


    @Cacheable(value = "orderCount")
    public long getCachedOrderCount() {
        return orderRepository.count();
    }

    private OrderResponseDTO toResponse(Order order) {
        List<OrderItemResponseDTO> items = order.getItems().stream()
                .map(item -> new OrderItemResponseDTO(
                        item.getId(),
                        item.getProduct().getId(),
                        item.getProduct().getName(),
                        item.getQuantity(),
                        item.getUnitPrice()
                ))
                .toList();

        return new OrderResponseDTO(
                order.getId(),
                order.getStatus(),
                order.getTotal(),
                order.getCreatedAt(),
                items
        );
    }
}
