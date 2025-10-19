package com.exit.magazine.service;

import com.exit.common.grpc.*;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

import java.util.Set;

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

    public GetUsersNameAndProfileResponse getUsersNameAndProfile(Set<Long> userIds) {
        try {
            GetUsersNameAndProfileRequest request = GetUsersNameAndProfileRequest.newBuilder()
                    .addAllUserId(userIds)
                    .build();

            return userServiceStub.getUsersNameAndProfile(request);
        } catch (StatusRuntimeException e) {
            log.error("gRPC get users name and profile failed", e);
            return null;
        }
    }
}