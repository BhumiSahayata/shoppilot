package com.shoppilot.shoppilot.audit;

import com.shoppilot.shoppilot.model.ActorType;
import com.shoppilot.shoppilot.model.AuditEventType;
import com.shoppilot.shoppilot.model.AuditLog;
import com.shoppilot.shoppilot.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditLog logEvent(AuditEventType eventType,
                             ActorType actor,
                             String orderId,
                             String productId,
                             Double amount,
                             String reason,
                             Double confidence,
                             String status,
                             String detail) {
        AuditLog auditLog = AuditLog.builder()
                .timestamp(Instant.now())
                .eventType(eventType)
                .actor(actor)
                .orderId(orderId)
                .productId(productId)
                .amount(amount)
                .reason(reason)
                .confidence(confidence)
                .status(status)
                .detail(detail)
                .build();

        AuditLog saved = auditLogRepository.save(auditLog);
        log.info("[AUDIT] {} | Actor: {} | Order: {} | Amount: ₹{} | Status: {} | Reason: {}",
                eventType, actor, orderId, amount != null ? amount : 0, status, reason);
        return saved;
    }

    public List<AuditLog> getLogsForOrder(String orderId) {
        return auditLogRepository.findByOrderIdOrderByTimestampAsc(orderId);
    }

    public List<AuditLog> getRecentLogs() {
        return auditLogRepository.findAllByOrderByTimestampDesc();
    }
}
