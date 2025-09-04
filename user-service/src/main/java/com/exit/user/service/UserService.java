package com.exit.user.service;

import com.exit.common.auth.jwt.JwtTokenProvider;
import com.exit.common.auth.jwt.dto.UserIdRequest;
import com.exit.common.exception.grpc.GrpcException;
import com.exit.user.domain.RefreshToken;
import com.exit.user.domain.Users;
import com.exit.user.domain.repository.UserRepository;
import com.exit.user.controller.dto.request.LoginRequestDto;
import com.exit.user.controller.dto.request.RefreshTokenRequestDto;
import com.exit.user.controller.dto.request.SignUpRequestDto;
import com.exit.user.controller.dto.response.LoginSuccessResponse;
import com.exit.user.exception.GrpcUserErrorCode;
import com.exit.user.util.ImageSaveUtil;
import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {
    private final UserRepository userRepository;
    private final ImageSaveUtil imageSaveUtil;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public LoginSuccessResponse signUp(SignUpRequestDto request) {
        if (userRepository.existsByUserEmail(request.email())) {
            throw new GrpcException(GrpcUserErrorCode.EXISTING_USER);
        }
        // 이미지 마운트 경로를 지정해서 이미지 경로 지정
        String profileImagePath = null;
        if (request.profileImage() != null && request.profileImage().length > 0) {
            profileImagePath = imageSaveUtil.processProfileImage(request.profileImage(), request.email(), request.email());
        }

        String encodedPassword = passwordEncoder.encode(request.password());
        Users user = SignUpRequestDto.toUser(request, profileImagePath, encodedPassword);
        Users savedUser = userRepository.save(user);

        String accessToken = jwtTokenProvider.generateAccessToken(new UserIdRequest(savedUser.getUserId()));
        RefreshToken tokenInfo = RefreshToken.builder()
                .userId(user.getUserId())
                .jti(UUID.randomUUID().toString())
                .issuedAt(System.currentTimeMillis())
                .expiresAt(System.currentTimeMillis() + Duration.ofDays(7).toMillis())
                .deviceId(generateDeviceId(request))
                .build();

        jwtTokenProvider.generateRefreshToken(new UserIdRequest(savedUser.getUserId()), tokenInfo.getJti());

        return new LoginSuccessResponse(null, null, null, null, null, null, null, null);
    }


    public LoginSuccessResponse login(LoginRequestDto request) {

        return new LoginSuccessResponse(null, null, null, null, null, null, null, null);
    }


    public LoginSuccessResponse refreshAuthToken(RefreshTokenRequestDto request) {

        return new LoginSuccessResponse(null, null, null, null, null, null, null, null);
    }


    public LoginSuccessResponse logout(UserIdRequest request) {

        return new LoginSuccessResponse(null, null, null, null, null, null, null, null);
    }

    private String generateDeviceId(SignUpRequestDto request) {
        String userAgent = request.userAgent() != null ? request.userAgent() : "unknown";
        String ipAddress = request.ipAddress() != null ? request.ipAddress() : "0.0.0.0";

        String deviceFingerprint = userAgent + ipAddress + request.email();

        return DigestUtils.sha256Hex(deviceFingerprint).substring(0, 16);
    }
}
