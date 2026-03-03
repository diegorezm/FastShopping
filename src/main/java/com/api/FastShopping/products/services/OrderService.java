package com.api.FastShopping.products.services;

import com.api.FastShopping.products.dtos.*;
import com.api.FastShopping.products.models.Order;
import com.api.FastShopping.products.models.OrderItem;
import com.api.FastShopping.products.models.OrderStatus;
import com.api.FastShopping.products.models.Product;
import com.api.FastShopping.products.repositories.OrderRepository;
import com.api.FastShopping.products.repositories.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;


@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    @Transactional
    @CachePut(value = "orders", key = "#result.id", unless = "#result == null")
    public OrderResponseDTO placeOrder(PlaceOrderDTO dto) {
        Order order = new Order();
        BigDecimal total = BigDecimal.ZERO;

        for (OrderItemDTO itemDTO : dto.items()) {
            Product product = productRepository.findById(itemDTO.productId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.BAD_REQUEST, "Product not found: " + itemDTO.productId()
                    ));

            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setProduct(product);
            item.setQuantity(itemDTO.quantity());
            item.setUnitPrice(product.getPrice());  // snapshot price

            order.getItems().add(item);
            total = total.add(product.getPrice()
                    .multiply(BigDecimal.valueOf(itemDTO.quantity())));
        }

        order.setTotal(total);
        Order saved =  orderRepository.save(order);
        return toResponse(saved);
    }


    @Transactional
    @CachePut(value = "orders", key = "#result.id", unless = "#result == null")
    public OrderResponseDTO cancel(UUID id) {
        Order order = orderRepository.findWithItemsById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Order not found"
                ));
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Only PENDING orders can be cancelled"
            );
        }
        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
        return toResponse(order);
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