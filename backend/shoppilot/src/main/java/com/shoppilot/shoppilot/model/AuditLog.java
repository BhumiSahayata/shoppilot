package com.shoppilot.shoppilot.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "audit_logs")
public class AuditLog {

    @Id
    private String id;

    @Builder.Default
    private Instant timestamp = Instant.now();

    private AuditEventType eventType;
    private ActorType actor;

    private String orderId;
    private String productId;
    private Double amount;
    private String reason;
    private Double confidence;
    private String status;
    private String detail;
}
