package com.exit.user.service.user;

import com.exit.common.exception.grpc.GrpcException;
import com.exit.common.grpc.IncreaseReportCountRequest;
import com.exit.common.grpc.UpdateAdditionalUserInfoRequest;
import com.exit.common.grpc.UpdateAdditionalUserInfoResponse;
import com.exit.common.util.file.FileUploadUtil;
import com.exit.user.domain.Users;
import com.exit.user.domain.repository.UserRepository;
import com.exit.user.exception.GrpcUserErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {
    private final UserRepository userRepository;
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
}

