package com.shoppilot.shoppilot.controller;

import com.shoppilot.shoppilot.dto.ApiResponse;
import com.shoppilot.shoppilot.dto.CreateDraftOrderRequest;
import com.shoppilot.shoppilot.dto.OrderApprovalRequest;
import com.shoppilot.shoppilot.model.Order;
import com.shoppilot.shoppilot.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/draft")
    public ResponseEntity<ApiResponse<Order>> createDraftOrder(@Valid @RequestBody CreateDraftOrderRequest request) {
        Order draft = orderService.createDraftOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Draft order created awaiting customer approval", draft));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Order>> getOrder(@PathVariable String id) {
        Order order = orderService.getOrder(id);
        return ResponseEntity.ok(ApiResponse.ok(order));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<Order>> approveOrder(
            @PathVariable String id,
            @Valid @RequestBody OrderApprovalRequest request) {
        Order approved = orderService.approveOrder(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Customer approval processed successfully", approved));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Order>>> getAllOrders() {
        List<Order> orders = orderService.getAllOrders();
        return ResponseEntity.ok(ApiResponse.ok(orders));
    }
}
