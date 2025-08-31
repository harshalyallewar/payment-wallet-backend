package com.pw.analyticsservice.controller;

import com.pw.analyticsservice.entity.DailySystemSummary;
import com.pw.analyticsservice.entity.DailyUserSummary;
import com.pw.analyticsservice.repository.DailySystemSummaryRepository;
import com.pw.analyticsservice.repository.DailyUserSummaryRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/analytics")
public class AnalyticsController {

    private final EntityManager em;
    private final DailyUserSummaryRepository userRepo;
    private final DailySystemSummaryRepository sysRepo;

    public AnalyticsController(EntityManager em,
                               DailyUserSummaryRepository userRepo,
                               DailySystemSummaryRepository sysRepo) {
        this.em = em;
        this.userRepo = userRepo;
        this.sysRepo = sysRepo;
    }

    @GetMapping("/users/{userId}/daily")
    public ResponseEntity<List<DailyUserSummary>> userDaily(
            @PathVariable Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        TypedQuery<DailyUserSummary> q = em.createQuery("""
            SELECT d FROM DailyUserSummary d
            WHERE d.userId = :uid AND d.date BETWEEN :f AND :t
            ORDER BY d.date ASC
            """, DailyUserSummary.class);
        q.setParameter("uid", userId);
        q.setParameter("f", from);
        q.setParameter("t", to);
        return ResponseEntity.ok(q.getResultList());
    }

    @GetMapping("/system/daily")
    public ResponseEntity<List<DailySystemSummary>> systemDaily(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @NotNull LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @NotNull LocalDate to) {
        TypedQuery<DailySystemSummary> q = em.createQuery("""
            SELECT s FROM DailySystemSummary s
            WHERE s.date BETWEEN :f AND :t
            ORDER BY s.date ASC
            """, DailySystemSummary.class);
        q.setParameter("f", from);
        q.setParameter("t", to);
        return ResponseEntity.ok(q.getResultList());
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }
}
