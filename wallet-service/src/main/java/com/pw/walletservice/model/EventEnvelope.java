package com.pw.walletservice.model;
import lombok.*;
import java.time.Instant;
import java.util.Map;

/**
 * Generic envelope each producer should send.
 * Example JSON:
 * {
 *   "eventType": "WALLET_CREDITED",
 *   "eventId": "uuid",
 *   "timestamp": "2025-08-31T12:30:00Z",
 *   "userId": 101,
 *   "payload": { "amount": 200, "balanceAfter": 800, "requestId": "..." }
 * }
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventEnvelope {
    private String eventType;
    private String eventId;
    private Instant timestamp;
    private Long userId;           // nullable for system events
    private Map<String, Object> payload;
}

