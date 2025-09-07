package com.pw.analyticsservice.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
public class EventEnvelope {
    private static final Logger log = LoggerFactory.getLogger(EventEnvelope.class);
    private String eventType;
    private String eventId;
    private Instant timestamp;
    private Long userId;           // nullable for system events
    private Map<String, Object> payload;

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Map<String, Object> getPayload() { return payload; }
    public void setPayload(Map<String, Object> payload) { this.payload = payload; }

    @Override
    public String toString() {
        return "EventEnvelope{" +
                "eventType='" + eventType + '\'' +
                ", eventId='" + eventId + '\'' +
                ", timestamp=" + timestamp +
                ", userId=" + userId +
                ", payload=" + payload +
                '}';
    }


}

