package com.exit.notification.service;

import com.exit.common.grpc.*;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class UserGrpcClient {

    @GrpcClient("user-service")
    private UserServiceGrpc.UserServiceBlockingStub userServiceStub;

    public List<String> getFcmToken(Long userId) {
        try {
            GetFcmTokenRequest request = GetFcmTokenRequest.newBuilder()
                    .setUserId(userId)
                    .build();

            log.debug("Sending get user fcmToken request via gRPC for userId: {}", userId);
            GetFcmTokenResponse response = userServiceStub.getFcmToken(request);
            return response.getFcmTokenList();
        } catch (StatusRuntimeException e) {
            log.error("gRPC get user fcmToken failed for userId {}: {}", userId, e.getStatus(), e);
            return null;
        }
    }
}