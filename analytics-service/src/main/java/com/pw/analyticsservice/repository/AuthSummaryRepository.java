package com.pw.analyticsservice.repository;

import com.pw.analyticsservice.entity.AuthSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;

public interface AuthSummaryRepository extends JpaRepository<AuthSummary, Long> {

    @Modifying
    @Query(value = """
        INSERT INTO auth_summary (date, user_id, logins, last_updated)
        VALUES (?1, ?2, 1, NOW())
        ON CONFLICT (user_id, date) DO UPDATE SET
          logins = auth_summary.logins + 1,
          last_updated = NOW()
        """, nativeQuery = true)
    void incrementLogin(LocalDate date, Long userId);

    @Modifying
    @Query(value = """
        INSERT INTO auth_summary (date, user_id, logouts, last_updated)
        VALUES (?1, ?2, 1, NOW())
        ON CONFLICT (user_id, date) DO UPDATE SET
          logouts = auth_summary.logouts + 1,
          last_updated = NOW()
        """, nativeQuery = true)
    void incrementLogout(LocalDate date, Long userId);

    @Modifying
    @Query(value = """
        INSERT INTO auth_summary (date, user_id, failed_logins, last_updated)
        VALUES (?1, ?2, 1, NOW())
        ON CONFLICT (user_id, date) DO UPDATE SET
          failed_logins = auth_summary.failed_logins + 1,
          last_updated = NOW()
        """, nativeQuery = true)
    void incrementFailedLogin(LocalDate date, Long userId);

    @Modifying
    @Query(value = """
        INSERT INTO auth_summary (date, user_id, token_refreshes, last_updated)
        VALUES (?1, ?2, 1, NOW())
        ON CONFLICT (user_id, date) DO UPDATE SET
          token_refreshes = auth_summary.token_refreshes + 1,
          last_updated = NOW()
        """, nativeQuery = true)
    void incrementTokenRefresh(LocalDate date, Long userId);
}

