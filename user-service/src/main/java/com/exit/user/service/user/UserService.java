package com.exit.user.service.user;

import com.exit.common.exception.grpc.GrpcException;
import com.exit.common.grpc.*;
import com.exit.common.util.file.FileUploadUtil;
import com.exit.user.domain.UserFcmToken;
import com.exit.user.domain.Users;
import com.exit.user.domain.repository.UserFcmTokenRepository;
import com.exit.user.domain.repository.UserRepository;
import com.exit.user.exception.GrpcUserErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserService {
    private final UserRepository userRepository;
    private final UserFcmTokenRepository userFcmTokenRepository;
    private final FileUploadUtil fileUploadUtil;

    private final String PROFILE_FOLDER = "profile";

    public void increaseReportCount(IncreaseReportCountRequest request) {
        try {
            Users user = userRepository.findById(request.getUserId())
                    .orElseThrow(() -> new GrpcException(GrpcUserErrorCode.USER_NOT_FOUND));

            user.increaseReportCount();
            userRepository.save(user);
        } catch (GrpcException e) {
            throw new GrpcException(GrpcUserErrorCode.INCREASE_REPORT_COUNT_FAILED, e.getGrpcErrorCode().getErrorDescription());
        } catch (Exception e) {
            log.error("Increase report count failed for userId: {}", request.getUserId(), e);
            throw new GrpcException(GrpcUserErrorCode.INCREASE_REPORT_COUNT_FAILED, e.getMessage());
        }
    }

    public UpdateAdditionalUserInfoResponse updateAdditionalUserInfo(UpdateAdditionalUserInfoRequest request) {
        try {
            Users user = userRepository.findById(request.getUserId())
                    .orElseThrow(() -> new GrpcException(GrpcUserErrorCode.USER_NOT_FOUND));

            // 1. 먼저 이미지 삭제 처리
            if (request.getIsProfileImageDeleted()) {
                if (user.getUserProfileUrl() != null && !user.getUserProfileUrl().isEmpty()) {
                    try {
                        fileUploadUtil.deleteFile(user.getUserProfileUrl());
                    } catch (Exception e) {
                        // 삭제 실패해도 계속 진행 (파일이 이미 없을 수 있음)
                        log.warn("Failed to delete old profile image: {}", user.getUserProfileUrl(), e);
                    }
                }
                user.updateProfile(null);
            }

            // 2. 새 이미지 업로드 (기존 이미지가 있으면 먼저 삭제)
            if (request.hasImageFile()) {
                // 기존 프로필 이미지가 있으면 삭제
                if (user.getUserProfileUrl() != null && !user.getUserProfileUrl().isEmpty()) {
                    try {
                        fileUploadUtil.deleteFile(user.getUserProfileUrl());
                    } catch (Exception e) {
                        log.warn("Failed to delete old profile image: {}", user.getUserProfileUrl(), e);
                    }
                }
                String imagePath = fileUploadUtil.uploadImage(request.getImageFile(), PROFILE_FOLDER);
                user.updateProfile(imagePath);
            }

            // 3. 닉네임 업데이트
            if (request.hasUserName()) {
                user.updateNickname(request.getUserName());
            }

            Users savedUser = userRepository.save(user);

            UpdateAdditionalUserInfoResponse.Builder builder = UpdateAdditionalUserInfoResponse.newBuilder();

            if (savedUser.getUserProfileUrl() != null && !savedUser.getUserProfileUrl().isEmpty()) {
                builder.setUserProfile(user.getUserProfileUrl());
            }

            return builder
                    .setUserId(savedUser.getUserId())
                    .setUserName(savedUser.getUserNickname())
                    .build();
        } catch (GrpcException e) {
            throw new GrpcException(GrpcUserErrorCode.UPDATE_ADDITIONAL_INFO_FAILED, e.getGrpcErrorCode().getErrorDescription());
        } catch (Exception e) {
            log.error("Update additional user info failed for userId: {}", request.getUserId(), e);
            throw new GrpcException(GrpcUserErrorCode.UPDATE_ADDITIONAL_INFO_FAILED, e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public UpdateAdditionalUserInfoResponse getUserNameAndProfile(Long userId) {
        try {
            Users user = userRepository.findById(userId)
                    .orElseThrow(() -> new GrpcException(GrpcUserErrorCode.USER_NOT_FOUND));

            UpdateAdditionalUserInfoResponse.Builder builder = UpdateAdditionalUserInfoResponse.newBuilder();
            if (user.getUserProfileUrl() != null && !user.getUserProfileUrl().isEmpty()) {
                builder.setUserProfile(user.getUserProfileUrl());
            }

            return builder
                    .setUserId(user.getUserId())
                    .setUserName(user.getUserNickname())
                    .build();
        } catch (GrpcException e) {
            throw new GrpcException(GrpcUserErrorCode.GET_USER_NAME_AND_PROFILE_FAILED, e.getGrpcErrorCode().getErrorDescription());
        } catch (Exception e) {
            log.error("Get user name and profile failed for userId: {}", userId, e);
            throw new GrpcException(GrpcUserErrorCode.GET_USER_NAME_AND_PROFILE_FAILED, e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public GetFcmTokenResponse getFcmToken(GetFcmTokenRequest request) {
        try {
            List<String> fcmTokens = userFcmTokenRepository.findFcmTokenByUserId(request.getUserId());

            return GetFcmTokenResponse.newBuilder()
                    .addAllFcmToken(fcmTokens)
                    .build();
        } catch (Exception e) {
            log.error("Get FCM token failed for userId: {}", request.getUserId(), e);
            throw new GrpcException(GrpcUserErrorCode.GET_FCM_TOKEN_FAILED, e.getMessage());
        }
    }

    public void updateDevice(UpdateDeviceRequest request) {
        try {
            Users user = userRepository.findById(request.getUserId())
                    .orElseThrow(() -> new GrpcException(GrpcUserErrorCode.USER_NOT_FOUND));
            createAndSaveFcmToken(request.getFcmToken(), user, request.getDeviceId());
        } catch (GrpcException e) {
            throw new GrpcException(GrpcUserErrorCode.UPDATE_DEVICE_FAILED, e.getGrpcErrorCode().getErrorDescription());
        } catch (Exception e) {
            log.error("Update device failed for userId: {}", request.getUserId(), e);
            throw new GrpcException(GrpcUserErrorCode.UPDATE_DEVICE_FAILED, e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public GetUsersNameAndProfileResponse getUsersNameAndProfile(List<Long> userIdList) {
        try {
            List<Users> users = userRepository.findAllByUserIdIn(userIdList);
            List<UpdateAdditionalUserInfoResponse> response = users.stream()
                    .map(user -> {
                        UpdateAdditionalUserInfoResponse.Builder builder = UpdateAdditionalUserInfoResponse.newBuilder();
                        if (user.getUserProfileUrl() != null && !user.getUserProfileUrl().isEmpty()) {
                            builder.setUserProfile(user.getUserProfileUrl());
                        }

                        return builder
                                .setUserId(user.getUserId())
                                .setUserName(user.getUserNickname())
                                .build();
                    })
                    .toList();

            return GetUsersNameAndProfileResponse.newBuilder()
                    .addAllUserInfo(response)
                    .build();
        } catch (GrpcException e) {
            throw new GrpcException(GrpcUserErrorCode.GET_USERS_NAME_AND_PROFILE_FAILED, e.getGrpcErrorCode().getErrorDescription());
        } catch (Exception e) {
            log.error("Get users name and profile failed for userIds: {}", userIdList, e);
            throw new GrpcException(GrpcUserErrorCode.GET_USERS_NAME_AND_PROFILE_FAILED, e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public CheckNicknameDuplicateResponse checkNicknameDuplicate(String nickname) {
        try {
            Optional<Users> exists = userRepository.findByUserNickname(nickname);
            boolean flag = exists.isEmpty();

            return CheckNicknameDuplicateResponse.newBuilder()
                    .setIsAvailable(flag)
                    .build();
        } catch (Exception e) {
            log.error("Check nickname duplicate failed for nickname: {}", nickname, e);
            throw new GrpcException(GrpcUserErrorCode.CHECK_NICKNAME_DUPLICATE_FAILED, e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public String getUserName(Long userId) {
        try {
            Users user = userRepository.findById(userId)
                    .orElseThrow(() -> new GrpcException(GrpcUserErrorCode.USER_NOT_FOUND));
            return user.getUserNickname();
        } catch (GrpcException e) {
            throw new GrpcException(GrpcUserErrorCode.GET_USER_NAME_FAILED, e.getGrpcErrorCode().getErrorDescription());
        } catch (Exception e) {
            throw new GrpcException(GrpcUserErrorCode.GET_USER_NAME_FAILED, e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public List<UserIdAndNameInfo> getUserNames(List<Long> userIds) {
        try {
            List<Users> users = userRepository.findAllByUserIdIn(userIds);
            return users.stream()
                    .map(user -> UserIdAndNameInfo.newBuilder()
                            .setUserId(user.getUserId())
                            .setUserName(user.getUserNickname())
                            .build())
                    .toList();
        } catch (Exception e) {
            throw new GrpcException(GrpcUserErrorCode.GET_USER_NAMES_FAILED, e.getMessage());
        }
    }

    private void createAndSaveFcmToken(String fcmToken, Users user, String deviceId) {
        Optional<UserFcmToken> existFcmToken = userFcmTokenRepository.findByDeviceId(deviceId);
        existFcmToken.ifPresent(userFcmTokenRepository::delete);

        UserFcmToken userFcmToken = UserFcmToken.builder()
                .user(user)
                .token(fcmToken)
                .deviceId(deviceId)
                .active(true)
                .build();

        userFcmTokenRepository.save(userFcmToken);
    }
}