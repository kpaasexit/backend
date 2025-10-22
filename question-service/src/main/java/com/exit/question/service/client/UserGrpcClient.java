package com.exit.question.service.client;

import com.exit.common.grpc.GetUserNameRequest;
import com.exit.common.grpc.GetUserNameResponse;
import com.exit.common.grpc.GetUsersNameAndProfileRequest;
import com.exit.common.grpc.GetUsersNameAndProfileResponse;
import com.exit.common.grpc.IncreaseReportCountRequest;
import com.exit.common.grpc.UpdateAdditionalUserInfoResponse;
import com.exit.common.grpc.UserServiceGrpc;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class UserGrpcClient {

    @GrpcClient("user-service")
    private UserServiceGrpc.UserServiceBlockingStub userServiceStub;

    public String getUserName(Long userId) {
        GetUserNameRequest request = GetUserNameRequest.newBuilder()
                .setUserId(userId)
                .build();

        log.debug("Sending get user name request via gRPC for userId: {}", userId);
        GetUserNameResponse response = userServiceStub.getUserName(request);

        if (response.getSuccess()) {
            log.debug("Received user name via gRPC: {}", response.getUserName());
            return response.getUserName();
        } else {
            log.warn("Failed to get user name: {}", response.getMessage());
            return null;
        }
    }

    public void increaseReportCount(Long userId) {
        IncreaseReportCountRequest request = IncreaseReportCountRequest.newBuilder()
                .setUserId(userId)
                .build();

        log.debug("Sending increase user report request via gRPC for userId: {}", userId);
        userServiceStub.increaseReportCount(request);
    }

    public GetUsersNameAndProfileResponse getUsersNameAndProfile(Set<Long> userIds) {
        GetUsersNameAndProfileRequest request = GetUsersNameAndProfileRequest.newBuilder()
                .addAllUserId(userIds)
                .build();

        log.debug("Sending get useName and profile request via gRPC for userId: {}", userIds);
        return userServiceStub.getUsersNameAndProfile(request);
    }

    public UpdateAdditionalUserInfoResponse getUserNameAndProfile(Long questionWriterId) {
        GetUserNameRequest request = GetUserNameRequest.newBuilder()
                .setUserId(questionWriterId)
                .build();

        log.debug("Sending get useName and profile request via gRPC for userId: {}", questionWriterId);
        UpdateAdditionalUserInfoResponse response = userServiceStub.getUserNameAndProfile(request);
        return response;
    }
}