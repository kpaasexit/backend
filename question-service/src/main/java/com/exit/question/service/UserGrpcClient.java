package com.exit.question.service;

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

    public String getUserName(Long userId) {
        try {
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
        } catch (StatusRuntimeException e) {
            log.error("gRPC get user name failed for userId {}: {}", userId, e.getStatus(), e);
            return null;
        }
    }

    public List<UserIdAndNameInfo> getUserNames(List<Long> userIds) {
        try {
            GetUserNamesRequest request = GetUserNamesRequest.newBuilder()
                    .addAllUserId(userIds)
                    .build();

            GetUserNamesResponse response = userServiceStub.getUserNames(request);

            return response.getUserInfoList();
        }
        catch (StatusRuntimeException e) {
            log.error("gRPC get user name list failed {}", e.getStatus(), e);
            return null;
        }
    }

    public void increaseReportCount(Long userId) {
        try {
            IncreaseReportCountRequest request = IncreaseReportCountRequest.newBuilder()
                    .setUserId(userId)
                    .build();

            log.debug("Sending increase user report request via gRPC for userId: {}", userId);
            userServiceStub.increaseReportCount(request);
        } catch (StatusRuntimeException e) {
            log.error("gRPC get user name failed for userId {}: {}", userId, e.getStatus(), e);
        }
    }
}