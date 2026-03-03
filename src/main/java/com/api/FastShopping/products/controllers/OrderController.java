package com.api.FastShopping.products.controllers;

import com.api.FastShopping.products.dtos.CachedOrderPage;
import com.api.FastShopping.products.dtos.OrderResponseDTO;
import com.api.FastShopping.products.dtos.PlaceOrderDTO;
import com.api.FastShopping.products.models.Order;
import com.api.FastShopping.products.services.OrderQueryService;
import com.api.FastShopping.products.services.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;
    private final OrderQueryService orderQueryService;

    @PostMapping
    public ResponseEntity<OrderResponseDTO> placeOrder(@RequestBody PlaceOrderDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(orderService.placeOrder(dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponseDTO> getOrder(@PathVariable UUID id) {
        return ResponseEntity.ok(orderQueryService.findById(id));
    }

    @GetMapping
    public ResponseEntity<CachedOrderPage> getOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(orderQueryService.findAll(page, size));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<OrderResponseDTO> cancelOrder(@PathVariable UUID id) {
        return ResponseEntity.ok(orderService.cancel(id));
    }
}