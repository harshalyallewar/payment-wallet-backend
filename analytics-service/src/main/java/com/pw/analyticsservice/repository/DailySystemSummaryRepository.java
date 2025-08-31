package com.pw.analyticsservice.repository;

import com.pw.analyticsservice.entity.DailySystemSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface DailySystemSummaryRepository extends JpaRepository<DailySystemSummary, Long> {

    @Modifying
    @Query(value = """
        INSERT INTO daily_system_summary (date, total_txns, total_volume, last_updated)
        VALUES (?1, 1, ?2, NOW())
        ON CONFLICT (date) DO UPDATE SET
          total_txns = daily_system_summary.total_txns + 1,
          total_volume = daily_system_summary.total_volume + EXCLUDED.total_volume,
          last_updated = NOW()
        """, nativeQuery = true)
    void incrementSystemTxn(LocalDate date, BigDecimal amount);

    @Modifying
    @Query(value = """
        INSERT INTO daily_system_summary (date, failed_txns, last_updated)
        VALUES (?1, 1, NOW())
        ON CONFLICT (date) DO UPDATE SET
          failed_txns = daily_system_summary.failed_txns + 1,
          last_updated = NOW()
        """, nativeQuery = true)
    void incrementSystemFailed(LocalDate date);

    @Modifying
    @Query(value = """
        INSERT INTO daily_system_summary (date, new_users, total_users, last_updated)
        VALUES (?1, 1, 1, NOW())
        ON CONFLICT (date) DO UPDATE SET
          new_users = daily_system_summary.new_users + 1,
          total_users = daily_system_summary.total_users + 1,
          last_updated = NOW()
        """, nativeQuery = true)
    void incrementUserCreated(LocalDate date);
}

