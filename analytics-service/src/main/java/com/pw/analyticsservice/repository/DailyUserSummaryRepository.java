package com.pw.analyticsservice.repository;

import com.pw.analyticsservice.entity.DailyUserSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface DailyUserSummaryRepository extends JpaRepository<DailyUserSummary, Long> {

    @Modifying
    @Query(value = """
        INSERT INTO daily_user_summary 
            (user_id, date, total_credits, total_debits, failed_txns, net_change, last_updated)
        VALUES 
            (?1, ?2, ?3, 0, 0, ?3, NOW())
        ON CONFLICT (user_id, date) DO UPDATE SET
          total_credits = daily_user_summary.total_credits + EXCLUDED.total_credits,
          net_change = daily_user_summary.net_change + EXCLUDED.net_change,
          last_updated = NOW()
        """, nativeQuery = true)
    void incrementCredits(Long userId, LocalDate date, BigDecimal amount);

    @Modifying
    @Query(value = """
        INSERT INTO daily_user_summary 
            (user_id, date, total_credits, total_debits, failed_txns, net_change, last_updated)
        VALUES 
            (?1, ?2, 0, ?3, 0, (?3 * -1), NOW())
        ON CONFLICT (user_id, date) DO UPDATE SET
          total_debits = daily_user_summary.total_debits + EXCLUDED.total_debits,
          net_change = daily_user_summary.net_change + EXCLUDED.net_change,
          last_updated = NOW()
        """, nativeQuery = true)
    void incrementDebits(Long userId, LocalDate date, BigDecimal amount);

    @Modifying
    @Query(value = """
        INSERT INTO daily_user_summary 
            (user_id, date, total_credits, total_debits, failed_txns, net_change, last_updated)
        VALUES 
            (?1, ?2, 0, 0, 1, 0, NOW())
        ON CONFLICT (user_id, date) DO UPDATE SET
          failed_txns = daily_user_summary.failed_txns + 1,
          last_updated = NOW()
        """, nativeQuery = true)
    void incrementFailed(Long userId, LocalDate date);
}
