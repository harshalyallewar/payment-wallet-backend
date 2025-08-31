package com.pw.walletservice.repository;

import com.pw.walletservice.model.Wallet;
import com.pw.walletservice.model.WalletType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WalletRepository extends JpaRepository<Wallet, Long> {
    Optional<Wallet> findByUserId(Long userId);
    Optional<Wallet> findByRequestId(String requestId);
}
