package com.exit.user.service;

import com.exit.common.auth.jwt.dto.UserIdRequest;
import com.exit.common.exception.grpc.GrpcException;
import com.exit.user.controller.dto.request.LoginRequestDto;
import com.exit.user.controller.dto.request.RefreshTokenRequestDto;
import com.exit.user.controller.dto.request.SignUpRequestDto;
import com.exit.user.controller.dto.response.LoginSuccessResponse;
import com.exit.user.grpc.*;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class UserGrpcService extends UserServiceGrpc.UserServiceImplBase {

    private final UserService userService;

    @Override
    public void signUp(SignUpRequest request, StreamObserver<SignUpResponse> responseObserver) {
        try {
            log.info("SignUp request received for email: {}", request.getEmail());

            // 기존 서비스 호출 (DTO 변환 필요)
            SignUpRequestDto signUpDto = SignUpRequestDto.from(request);

            LoginSuccessResponse serviceResponse = userService.signUp(signUpDto);

            // gRPC 응답으로 변환
            SignUpResponse grpcResponse = SignUpResponse.newBuilder()
                    .setAccessToken(serviceResponse.accessToken())
                    .setRefreshToken(serviceResponse.refreshToken())
                    .setUserInfo(buildUserInfo(serviceResponse))
                    .build();

            responseObserver.onNext(grpcResponse);
            responseObserver.onCompleted();

        } catch (GrpcException e) {
            log.error("User already exists: {}", request.getEmail());
            responseObserver.onError(Status.ALREADY_EXISTS
                    .withDescription("이미 존재하는 사용자입니다")
                    .asRuntimeException());
        } catch (Exception e) {
            log.error("SignUp failed", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("회원가입 처리 중 오류가 발생했습니다")
                    .asRuntimeException());
        }
    }

    @Override
    public void login(LoginRequest request, StreamObserver<LoginResponse> responseObserver) {
        try {
            log.info("Login request received for email: {}", request.getEmail());

            // 기존 서비스 호출
            LoginRequestDto loginDto = LoginRequestDto.from(request);

            LoginSuccessResponse serviceResponse = userService.login(loginDto);

            // gRPC 응답으로 변환
            LoginResponse grpcResponse = LoginResponse.newBuilder()
                    .setAccessToken(serviceResponse.accessToken())
                    .setRefreshToken(serviceResponse.refreshToken())
                    .setUserInfo(buildUserInfo(serviceResponse))
                    .build();

            responseObserver.onNext(grpcResponse);
            responseObserver.onCompleted();

        } catch (GrpcException e) {
            log.error("User not found: {}", request.getEmail());
            responseObserver.onError(Status.NOT_FOUND
                    .withDescription("사용자를 찾을 수 없습니다")
                    .asRuntimeException());
        } catch (IllegalAccessError e) {
            log.error("Invalid password for user: {}", request.getEmail());
            responseObserver.onError(Status.UNAUTHENTICATED
                    .withDescription("잘못된 비밀번호입니다")
                    .asRuntimeException());
        } catch (Exception e) {
            log.error("Login failed", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("로그인 처리 중 오류가 발생했습니다")
                    .asRuntimeException());
        }
    }

    @Override
    public void refresh(RefreshTokenRequest request, StreamObserver<RefreshTokenResponse> responseObserver) {
        try {
            log.info("Refresh token request received");

            // 기존 서비스 호출
            RefreshTokenRequestDto refreshDto = RefreshTokenRequestDto.from(request);

            LoginSuccessResponse serviceResponse = userService.refreshAuthToken(refreshDto);

            // gRPC 응답으로 변환
            RefreshTokenResponse grpcResponse = RefreshTokenResponse.newBuilder()
                    .setAccessToken(serviceResponse.accessToken())
                    .setRefreshToken(serviceResponse.refreshToken())
                    .build();

            responseObserver.onNext(grpcResponse);
            responseObserver.onCompleted();

        } catch (GrpcException e) {
            log.error("Invalid refresh token");
            responseObserver.onError(Status.UNAUTHENTICATED
                    .withDescription("유효하지 않은 토큰입니다")
                    .asRuntimeException());
        } catch (IllegalAccessError e) {
            log.error("Refresh token expired");
            responseObserver.onError(Status.UNAUTHENTICATED
                    .withDescription("토큰이 만료되었습니다")
                    .asRuntimeException());
        } catch (Exception e) {
            log.error("Refresh token failed", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("토큰 갱신 중 오류가 발생했습니다")
                    .asRuntimeException());
        }
    }

    @Override
    public void logout(LogoutRequest request, StreamObserver<LogoutResponse> responseObserver) {
        try {
            log.info("Logout request received for userId: {}", request.getUserId());

            // 기존 서비스 호출
            UserIdRequest userIdRequest = new UserIdRequest(request.getUserId());

            userService.logout(userIdRequest);

            // gRPC 응답
            LogoutResponse grpcResponse = LogoutResponse.newBuilder()
                    .setMessage("로그아웃이 완료되었습니다")
                    .build();

            responseObserver.onNext(grpcResponse);
            responseObserver.onCompleted();

        } catch (GrpcException e) {
            log.error("User not found for logout: {}", request.getUserId());
            responseObserver.onError(Status.NOT_FOUND
                    .withDescription("사용자를 찾을 수 없습니다")
                    .asRuntimeException());
        } catch (Exception e) {
            log.error("Logout failed", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("로그아웃 처리 중 오류가 발생했습니다")
                    .asRuntimeException());
        }
    }

    // UserInfo 빌더 헬퍼 메서드
    private UserInfo buildUserInfo(LoginSuccessResponse serviceResponse) {
        return UserInfo.newBuilder()
                .setUserId(serviceResponse.userId())
                .setEmail(serviceResponse.email())
                .setName(serviceResponse.name())
                .setPhone(serviceResponse.phone() != null ? serviceResponse.phone() : "")
                .setProfileImageUrl(serviceResponse.profileImageUrl() != null ? serviceResponse.profileImageUrl() : "")
                .setCreatedAt(convertToTimestamp(serviceResponse.createdAt()))
                .build();
    }

    // 날짜 변환 헬퍼 메서드
    private com.google.protobuf.Timestamp convertToTimestamp(java.time.LocalDateTime dateTime) {
        if (dateTime == null) {
            return com.google.protobuf.Timestamp.getDefaultInstance();
        }

        java.time.Instant instant = dateTime.atZone(java.time.ZoneId.systemDefault()).toInstant();
        return com.google.protobuf.Timestamp.newBuilder()
                .setSeconds(instant.getEpochSecond())
                .setNanos(instant.getNano())
                .build();
    }
}