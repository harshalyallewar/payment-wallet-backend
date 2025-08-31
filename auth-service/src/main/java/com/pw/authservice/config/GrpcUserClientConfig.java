package com.pw.authservice.config;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.userservice.grpc.UserServiceGrpc;

@Configuration
public class GrpcUserClientConfig {

    @Bean
    public ManagedChannel userChannel() {
        return ManagedChannelBuilder
                .forAddress("user-service", 9003) // hostname + port of WalletService
                .usePlaintext()
                .build();
    }

    @Bean
    public UserServiceGrpc.UserServiceBlockingStub userStub(ManagedChannel userChannel) {
        return UserServiceGrpc.newBlockingStub(userChannel);
    }
}
