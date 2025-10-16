package com.exit.gateway.service.user;

import com.exit.common.exception.rest.RestApiException;
import com.exit.common.grpc.*;
import com.exit.common.response.error.rest.user.UserErrorCode;
import com.exit.gateway.controller.user.dto.request.user.UpdateAdditionalUserInfoRequestDto;
import com.exit.gateway.controller.user.dto.response.user.CheckNicknameDuplicateResponseDto;
import com.exit.gateway.controller.user.dto.response.user.GetUserInfoResponseDto;
import com.exit.gateway.global.annotation.GrpcToRest;
import com.exit.gateway.global.util.mapper.error.user.UserGrpcErrorMapper;
import com.google.protobuf.ByteString;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
@Slf4j
@RequiredArgsConstructor
@GrpcToRest(mapper = UserGrpcErrorMapper.class)
public class UserGrpcClient {
    @GrpcClient("user-service")
    private UserServiceGrpc.UserServiceBlockingStub userServiceStub;

    public UpdateAdditionalUserInfoResponse updateAdditionalUserInfo(Long userId, UpdateAdditionalUserInfoRequestDto requestDto) {
        MultipartFile image = requestDto.image();
        UpdateAdditionalUserInfoRequest.Builder builder = UpdateAdditionalUserInfoRequest.newBuilder();
        try {
            if (image != null) {
                ImageMetadata metaData = ImageMetadata.newBuilder()
                        .setFilename(image.getOriginalFilename())
                        .setContentType(image.getContentType())
                        .build();
                UploadBytesRequest uploadBytesRequest = UploadBytesRequest.newBuilder()
                        .setMeta(metaData)
                        .setData(ByteString.copyFrom(image.getBytes()))
                        .build();

                builder.setImageFile(uploadBytesRequest);
            }
        } catch (IOException e) {
            // todo 파일 입출력 전용 에러코드로 변경
            throw new RestApiException(UserErrorCode.UPDATE_ADDITIONAL_INFO_FAIL);
        }

        if (requestDto.nickname() != null && !requestDto.nickname().isEmpty()) {
            builder.setUserName(requestDto.nickname());
        }

        UpdateAdditionalUserInfoRequest request = builder
                .setUserId(userId)
                .setIsProfileImageDeleted(requestDto.isProfileImageDeleted())
                .build();

        log.debug("Sending updateAdditionalUserInfo request via gRPC: {}", request);
        UpdateAdditionalUserInfoResponse response = userServiceStub.updateAdditionalUserInfo(request);
        log.debug("Received updateAdditionalUserInfo response via gRPC: {}", response);

        return response;
    }

    public GetUserInfoResponseDto getUserInfo(Long userId) {
        GetUserNameRequest request = GetUserNameRequest.newBuilder()
                .setUserId(userId)
                .build();

        log.debug("Get user info request via gRPC: {}", request);
        UpdateAdditionalUserInfoResponse userInfo = userServiceStub.getUserNameAndProfile(request);
        return GetUserInfoResponseDto.from(userInfo);
    }

    public CheckNicknameDuplicateResponseDto checkNicknameDuplicate(String nickname) {
        CheckNicknameDuplicateRequest request = CheckNicknameDuplicateRequest.newBuilder()
                .setNickname(nickname)
                .build();

        log.debug("Check Nickname Duplicate request via gRPC: {}", request);
        CheckNicknameDuplicateResponse response = userServiceStub.checkNicknameDuplicate(request);
        return CheckNicknameDuplicateResponseDto.from(response);
    }

    public void updateDevice(Long userId, String deviceId, String fcmToken) {
        UpdateDeviceRequest request = UpdateDeviceRequest.newBuilder()
                .setUserId(userId)
                .setDeviceId(deviceId)
                .setFcmToken(fcmToken)
                .build();

        log.debug("Sending refresh token request via gRPC: {}", request);
        userServiceStub.updateDevice(request);
    }
}