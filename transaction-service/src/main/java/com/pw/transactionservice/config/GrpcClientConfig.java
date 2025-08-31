package com.pw.transactionservice.config;

import com.walletservice.grpc.WalletServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GrpcClientConfig {

    @Bean
    public ManagedChannel walletChannel() {
        return ManagedChannelBuilder
                .forAddress("wallet-service", 9001) // hostname + port of WalletService
                .usePlaintext()
                .build();
    }

    @Bean
    public WalletServiceGrpc.WalletServiceBlockingStub walletStub(ManagedChannel walletChannel) {
        return WalletServiceGrpc.newBlockingStub(walletChannel);
    }
}
