package com.pw.analyticsservice.repository;


import com.pw.analyticsservice.entity.RawEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RawEventRepository extends JpaRepository<RawEvent, Long> {
    Optional<RawEvent> findByEventId(UUID eventId);
}
