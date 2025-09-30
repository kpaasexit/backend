package com.exit.user.service.user;

import com.exit.common.exception.grpc.GrpcException;
import com.exit.common.grpc.*;
import com.exit.common.util.file.FileUploadUtil;
import com.exit.user.domain.Users;
import com.exit.user.domain.repository.UserFcmTokenRepository;
import com.exit.user.domain.repository.UserRepository;
import com.exit.user.exception.GrpcUserErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {
    private final UserRepository userRepository;
    private final UserFcmTokenRepository userFcmTokenRepository;
    private final FileUploadUtil fileUploadUtil;

    private final String PROFILE_FOLDER = "profile";

    public void increaseReportCount(IncreaseReportCountRequest request) {
        Users user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new GrpcException(GrpcUserErrorCode.USER_NOT_FOUND));

        user.increaseReportCount();
        userRepository.save(user);
    }

    public UpdateAdditionalUserInfoResponse updateAdditionalUserInfo(UpdateAdditionalUserInfoRequest request) {
        Users user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new GrpcException(GrpcUserErrorCode.USER_NOT_FOUND));

        String imagePath = fileUploadUtil.uploadImage(request.getImageFile(), PROFILE_FOLDER);
        user.updateProfile(request.getUserName(), imagePath);
        Users savedUser = userRepository.save(user);

        return UpdateAdditionalUserInfoResponse.newBuilder()
                .setUserId(savedUser.getUserId())
                .setUserName(savedUser.getUserNickname())
                .setUserProfile(savedUser.getUserProfileUrl())
                .build();
    }

    public UpdateAdditionalUserInfoResponse getUserNameAndProfile(Long userId) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new GrpcException(GrpcUserErrorCode.USER_NOT_FOUND));

        return UpdateAdditionalUserInfoResponse.newBuilder()
                .setUserId(user.getUserId())
                .setUserName(user.getUserNickname())
                .setUserProfile(user.getUserProfileUrl())
                .build();
    }


    public GetFcmTokenResponse getFcmToken(GetFcmTokenRequest request) {
        List<String> fcmTokens = userFcmTokenRepository.findFcmTokenByUserId(request.getUserId());

        return GetFcmTokenResponse.newBuilder()
                .addAllFcmToken(fcmTokens)
                .build();
    }
}

