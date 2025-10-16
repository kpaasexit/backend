package com.exit.user.service.user;

import com.exit.common.grpc.*;
import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.List;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class UserGrpcService extends UserServiceGrpc.UserServiceImplBase {
    private final UserService userService;

    @Override
    public void getUserName(GetUserNameRequest request, StreamObserver<GetUserNameResponse> responseObserver) {
        String userName = userService.getUserName(request.getUserId());

        GetUserNameResponse grpcResponse = GetUserNameResponse.newBuilder()
                .setUserName(userName)
                .setSuccess(true)
                .setMessage("User name retrieved successfully")
                .build();

        responseObserver.onNext(grpcResponse);
        responseObserver.onCompleted();
    }

    @Override
    public void getUserNames(GetUserNamesRequest request, StreamObserver<GetUserNamesResponse> responseObserver) {
        List<UserIdAndNameInfo> userIdAndNameInfos = userService.getUserNames(request.getUserIdList());

        GetUserNamesResponse grpcResponse = GetUserNamesResponse.newBuilder()
                .addAllUserInfo(userIdAndNameInfos)
                .build();

        responseObserver.onNext(grpcResponse);
        responseObserver.onCompleted();
    }

    @Override
    public void increaseReportCount(IncreaseReportCountRequest request, StreamObserver<Empty> responseObserver) {
        userService.increaseReportCount(request);

        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }

    @Override
    public void updateAdditionalUserInfo(com.exit.common.grpc.UpdateAdditionalUserInfoRequest request,
                                         StreamObserver<UpdateAdditionalUserInfoResponse> responseObserver) {
        UpdateAdditionalUserInfoResponse response = userService.updateAdditionalUserInfo(request);
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void getUserNameAndProfile(GetUserNameRequest request, StreamObserver<UpdateAdditionalUserInfoResponse> responseObserver) {
        log.info("Get user name and profile request received for userId: {}", request.getUserId());
        UpdateAdditionalUserInfoResponse response = userService.getUserNameAndProfile(request.getUserId());

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void getFcmToken(GetFcmTokenRequest request, StreamObserver<GetFcmTokenResponse> responseObserver) {
        log.info("Get fcmToken request received for userId: {}", request.getUserId());
        GetFcmTokenResponse response = userService.getFcmToken(request);

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void updateDevice(UpdateDeviceRequest request, StreamObserver<Empty> responseObserver) {
        log.info("Update device request received for userId: {}", request.getUserId());
        userService.updateDevice(request);

        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }

    @Override
    public void getUsersNameAndProfile(GetUsersNameAndProfileRequest request, StreamObserver<GetUsersNameAndProfileResponse> responseObserver) {
        log.info("Get users name and profile received for userIds: {}", request.getUserIdList());
        GetUsersNameAndProfileResponse response = userService.getUsersNameAndProfile(request.getUserIdList());

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void checkNicknameDuplicate(CheckNicknameDuplicateRequest request, StreamObserver<CheckNicknameDuplicateResponse> responseObserver) {
        log.info("Check Nickname Duplicate received for nickname: {}", request.getNickname());
        CheckNicknameDuplicateResponse response = userService.checkNicknameDuplicate(request.getNickname());

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}