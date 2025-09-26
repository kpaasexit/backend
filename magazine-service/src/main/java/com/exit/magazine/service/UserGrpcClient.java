package com.exit.magazine.service;

import com.exit.common.grpc.GetUserNameRequest;
import com.exit.common.grpc.UpdateAdditionalUserInfoResponse;
import com.exit.common.grpc.UserServiceGrpc;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class UserGrpcClient {

    @GrpcClient("user-service")
    private UserServiceGrpc.UserServiceBlockingStub userServiceStub;

    public UpdateAdditionalUserInfoResponse getUserNameAndProfile(Long userId) {
        try {
            GetUserNameRequest request = GetUserNameRequest.newBuilder()
                    .setUserId(userId)
                    .build();

            return userServiceStub.getUserNameAndProfile(request);
        } catch (StatusRuntimeException e) {
            log.error("gRPC get user name failed for userId {}: {}", userId, e.getStatus(), e);
            return null;
        }
    }
}