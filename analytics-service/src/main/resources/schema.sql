-- Raw event log (immutable)
CREATE TABLE IF NOT EXISTS raw_events (
                                          id BIGSERIAL PRIMARY KEY,
                                          event_type VARCHAR(50) NOT NULL,
    event_id UUID NOT NULL UNIQUE,
    user_id BIGINT NULL,
    payload TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
    );

-- Per-user, per-day summary
CREATE TABLE IF NOT EXISTS daily_user_summary (
                                                  id BIGSERIAL PRIMARY KEY,
                                                  user_id BIGINT NOT NULL,
                                                  date DATE NOT NULL,
                                                  total_credits NUMERIC(18,2) NOT NULL DEFAULT 0,
    total_debits NUMERIC(18,2) NOT NULL DEFAULT 0,
    failed_txns INT NOT NULL DEFAULT 0,
    net_change NUMERIC(18,2) NOT NULL DEFAULT 0,
    last_updated TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_user_date UNIQUE (user_id, date)
    );

-- System-wide daily summary
CREATE TABLE IF NOT EXISTS daily_system_summary (
                                                    id BIGSERIAL PRIMARY KEY,
                                                    date DATE NOT NULL UNIQUE,
                                                    total_users INT NOT NULL DEFAULT 0,
                                                    new_users INT NOT NULL DEFAULT 0,
                                                    total_txns INT NOT NULL DEFAULT 0,
                                                    failed_txns INT NOT NULL DEFAULT 0,
                                                    total_volume NUMERIC(18,2) NOT NULL DEFAULT 0,
    last_updated TIMESTAMP NOT NULL DEFAULT NOW()
    );

-- Auth activity per user per day
CREATE TABLE IF NOT EXISTS auth_summary (
                                            id BIGSERIAL PRIMARY KEY,
                                            date DATE NOT NULL,
                                            user_id BIGINT,
                                            logins INT NOT NULL DEFAULT 0,
                                            logouts INT NOT NULL DEFAULT 0,
                                            failed_logins INT NOT NULL DEFAULT 0,
                                            token_refreshes INT NOT NULL DEFAULT 0,
                                            last_updated TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_auth_user_date UNIQUE (user_id, date)
    );
