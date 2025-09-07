package com.pw.analyticsservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pw.analyticsservice.model.EventEnvelope;
import com.pw.analyticsservice.entity.RawEvent;
import com.pw.analyticsservice.repository.*;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.*;
import java.util.Map;
import java.util.UUID;

@Service
public class EventProcessor {
    private static final Logger log = LoggerFactory.getLogger(EventProcessor.class);

    private final RawEventRepository rawEventRepository;
    private final DailyUserSummaryRepository dailyUserSummaryRepository;
    private final DailySystemSummaryRepository dailySystemSummaryRepository;
    private final AuthSummaryRepository authSummaryRepository;
    private final ObjectMapper objectMapper;

    public EventProcessor(RawEventRepository rawEventRepository,
                          DailyUserSummaryRepository dailyUserSummaryRepository,
                          DailySystemSummaryRepository dailySystemSummaryRepository,
                          AuthSummaryRepository authSummaryRepository,
                          ObjectMapper objectMapper) {
        this.rawEventRepository = rawEventRepository;
        this.dailyUserSummaryRepository = dailyUserSummaryRepository;
        this.dailySystemSummaryRepository = dailySystemSummaryRepository;
        this.authSummaryRepository = authSummaryRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void process(EventEnvelope env) throws JsonProcessingException {
        // 1) idempotency for raw_events
        UUID eventId = UUID.fromString(env.getEventId());
        if (rawEventRepository.findByEventId(eventId).isPresent()) {
            log.info("Duplicate event received, skipping: {}", env.getEventId());
            return;
        }

        log.info("Processing : event: {}", env.toString());

        // 2) persist raw event
        String payloadJson = objectMapper.writeValueAsString(env.getPayload());
        rawEventRepository.save(new RawEvent(env.getEventType(), eventId, env.getUserId(), payloadJson));

        log.info("Successfully processed event and saved to raw repo: {}", env.toString());

        // 3) route to handlers
        LocalDate eventDate = toLocalDate(env);
        switch (env.getEventType()) {
            case "USER_CREATED" -> handleUserCreated(eventDate);
            case "WALLET_CREDITED" -> handleWalletCredit(env, eventDate);
            case "WALLET_DEBITED" -> handleWalletDebit(env, eventDate);
            case "WALLET_TRANSFER" -> handleWalletTransfer(env, eventDate);
            case "WALLET_FAILED" -> handleWalletFailed(env, eventDate);
            case "TRANSACTION_RECORDED" -> handleTxnRecorded(env, eventDate);
            case "USER_LOGGED_IN" -> handleLogin(env, eventDate);
            case "USER_LOGGED_OUT" -> handleLogout(env, eventDate);
            case "TOKEN_REFRESHED" -> handleTokenRefresh(env, eventDate);
            case "AUTH_FAILED" -> handleAuthFailed(env, eventDate);
            default -> log.warn("Unhandled event type: {}", env.getEventType());
        }
    }

    private LocalDate toLocalDate(EventEnvelope env) {
        Instant ts = env.getTimestamp() != null ? env.getTimestamp() : Instant.now();
        // Store aggregates in UTC date for consistency
        return ts.atZone(ZoneOffset.UTC).toLocalDate();
    }

    private BigDecimal amountFrom(EventEnvelope env) {
        Object amt = env.getPayload() != null ? env.getPayload().get("amount") : null;
        if (amt == null) return BigDecimal.ZERO;
        if (amt instanceof Number n) return BigDecimal.valueOf(n.longValue());
        return new BigDecimal(amt.toString());
    }

    // Handlers

    private void handleUserCreated(LocalDate date) {
        dailySystemSummaryRepository.incrementUserCreated(date);
    }

    private void handleWalletCredit(EventEnvelope env, LocalDate date) {
        if (env.getUserId() == null) return;
        BigDecimal amount = amountFrom(env);
        dailyUserSummaryRepository.incrementCredits(env.getUserId(), date, amount);
        dailySystemSummaryRepository.incrementSystemTxn(date, amount);
    }

    private void handleWalletDebit(EventEnvelope env, LocalDate date) {
        if (env.getUserId() == null) return;
        BigDecimal amount = amountFrom(env);
        dailyUserSummaryRepository.incrementDebits(env.getUserId(), date, amount);
        log.info("dailyUserSummaryRepository Debit processed for user {}, amount {}", env.getUserId(), amount);
        dailySystemSummaryRepository.incrementSystemTxn(date, amount);
    }

    private void handleWalletTransfer(EventEnvelope env, LocalDate date) {
        // payload: { fromUserId, toUserId, amount, success }
        Map<String,Object> p = env.getPayload();
        if (p == null) return;
        BigDecimal amount = amountFrom(env);
        Boolean success = p.get("success") instanceof Boolean b ? b : Boolean.TRUE;

        if (Boolean.TRUE.equals(success)) {
            Object fromId = p.get("fromUserId");
            Object toId   = p.get("toUserId");
            if (fromId != null)
                dailyUserSummaryRepository.incrementDebits(Long.valueOf(fromId.toString()), date, amount);
            if (toId != null)
                dailyUserSummaryRepository.incrementCredits(Long.valueOf(toId.toString()), date, amount);
            dailySystemSummaryRepository.incrementSystemTxn(date, amount);
        } else {
            dailySystemSummaryRepository.incrementSystemFailed(date);
        }
    }

    private void handleWalletFailed(EventEnvelope env, LocalDate date) {
        if (env.getUserId() != null)
            dailyUserSummaryRepository.incrementFailed(env.getUserId(), date);
        dailySystemSummaryRepository.incrementSystemFailed(date);
    }

    private void handleTxnRecorded(EventEnvelope env, LocalDate date) {
        // payload should include { type: CREDIT/DEBIT, status: SUCCESS/FAILED, amount }
        Map<String,Object> p = env.getPayload();
        if (p == null) return;
        String status = String.valueOf(p.getOrDefault("status","SUCCESS"));
        BigDecimal amount = amountFrom(env);

        if ("FAILED".equalsIgnoreCase(status)) {
            if (env.getUserId() != null)
                dailyUserSummaryRepository.incrementFailed(env.getUserId(), date);
            dailySystemSummaryRepository.incrementSystemFailed(date);
            return;
        }

        String type = String.valueOf(p.getOrDefault("type",""));
        if ("CREDIT".equalsIgnoreCase(type) && env.getUserId()!=null) {
            dailyUserSummaryRepository.incrementCredits(env.getUserId(), date, amount);
            dailySystemSummaryRepository.incrementSystemTxn(date, amount);
        } else if ("DEBIT".equalsIgnoreCase(type) && env.getUserId()!=null) {
            dailyUserSummaryRepository.incrementDebits(env.getUserId(), date, amount);
            dailySystemSummaryRepository.incrementSystemTxn(date, amount);
        }
    }

    private void handleLogin(EventEnvelope env, LocalDate date) {
        if (env.getUserId() != null) authSummaryRepository.incrementLogin(date, env.getUserId());
    }
    private void handleLogout(EventEnvelope env, LocalDate date) {
        if (env.getUserId() != null) authSummaryRepository.incrementLogout(date, env.getUserId());
    }
    private void handleTokenRefresh(EventEnvelope env, LocalDate date) {
        if (env.getUserId() != null) authSummaryRepository.incrementTokenRefresh(date, env.getUserId());
    }
    private void handleAuthFailed(EventEnvelope env, LocalDate date) {
        if (env.getUserId() != null) {
            authSummaryRepository.incrementFailedLogin(date, env.getUserId());
        } else {
            // anonymous failed login (by email) -> ignore per-user, but still a system failure
            dailySystemSummaryRepository.incrementSystemFailed(date);
        }
    }
}
