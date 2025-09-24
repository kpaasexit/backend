package com.exit.user.service.user;

import com.exit.common.grpc.*;
import com.exit.user.domain.Users;
import com.exit.user.domain.repository.UserRepository;
import com.google.protobuf.Empty;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class UserGrpcService extends UserServiceGrpc.UserServiceImplBase {
    private UserService userService;
    private final UserRepository userRepository;

    @Override
    public void getUserName(GetUserNameRequest request, StreamObserver<GetUserNameResponse> responseObserver) {
        try {
            log.info("Get user name request received for userId: {}", request.getUserId());

            Optional<Users> userOptional = userRepository.findById(request.getUserId());

            if (userOptional.isPresent()) {
                Users user = userOptional.get();
                String userName = user.getUserNickname(); // using userNickname field

                GetUserNameResponse grpcResponse = GetUserNameResponse.newBuilder()
                        .setUserName(userName)
                        .setSuccess(true)
                        .setMessage("User name retrieved successfully")
                        .build();

                responseObserver.onNext(grpcResponse);
                responseObserver.onCompleted();

                log.info("User name retrieved successfully for userId: {}, userName: {}", request.getUserId(), userName);
            } else {
                log.warn("User not found for userId: {}", request.getUserId());

                GetUserNameResponse grpcResponse = GetUserNameResponse.newBuilder()
                        .setUserName("")
                        .setSuccess(false)
                        .setMessage("User not found")
                        .build();

                responseObserver.onNext(grpcResponse);
                responseObserver.onCompleted();
            }

        } catch (Exception e) {
            log.error("Get user name failed for userId: {}", request.getUserId(), e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("사용자 이름 조회 중 오류가 발생했습니다")
                    .asRuntimeException());
        }
    }

    @Override
    public void getUserNames(GetUserNamesRequest request, StreamObserver<GetUserNamesResponse> responseObserver) {
        try {
            List<Users> users = userRepository.findAllByUserIdIn(request.getUserIdList());
            ArrayList<UserIdAndNameInfo> userIdAndNameInfos = new ArrayList<>();
            users.forEach(user -> {
                UserIdAndNameInfo info = UserIdAndNameInfo.newBuilder()
                        .setUserId(user.getUserId())
                        .setUserName(user.getUserNickname())
                        .build();

                userIdAndNameInfos.add(info);
            });

                GetUserNamesResponse grpcResponse = GetUserNamesResponse.newBuilder()
                        .addAllUserInfo(userIdAndNameInfos)
                        .build();

                responseObserver.onNext(grpcResponse);
                responseObserver.onCompleted();

        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL
                    .withDescription("배치 사용자 이름 조회 중 오류가 발생했습니다")
                    .asRuntimeException());
        }
    }

    @Override
    public void increaseReportCount(IncreaseReportCountRequest request, StreamObserver<Empty> responseObserver) {
        try {
            userService.increaseReportCount(request);

            responseObserver.onNext(Empty.getDefaultInstance());
            responseObserver.onCompleted();

        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL
                    .withDescription("신고 횟수 증가 중 오류가 발생했습니다")
                    .asRuntimeException());
        }
    }

    @Override
    public void updateAdditionalUserInfo(com.exit.common.grpc.UpdateAdditionalUserInfoRequest request,
                                         StreamObserver<UpdateAdditionalUserInfoResponse> responseObserver) {
        UpdateAdditionalUserInfoResponse response = userService.updateAdditionalUserInfo(request);
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}