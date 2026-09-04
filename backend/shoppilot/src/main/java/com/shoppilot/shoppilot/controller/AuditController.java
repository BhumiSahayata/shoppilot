package com.shoppilot.shoppilot.controller;

import com.shoppilot.shoppilot.audit.AuditService;
import com.shoppilot.shoppilot.dto.ApiResponse;
import com.shoppilot.shoppilot.model.AuditLog;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
public class AuditController {

    private final AuditService auditService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<AuditLog>>> getRecentAuditLogs() {
        List<AuditLog> logs = auditService.getRecentLogs();
        return ResponseEntity.ok(ApiResponse.ok(logs));
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<ApiResponse<List<AuditLog>>> getOrderAuditLogs(@PathVariable String orderId) {
        List<AuditLog> logs = auditService.getLogsForOrder(orderId);
        return ResponseEntity.ok(ApiResponse.ok(logs));
    }
}
