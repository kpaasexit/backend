package com.exit.gateway.service.user;

import com.exit.common.grpc.SocialLoginResponse;
import com.exit.gateway.controller.user.dto.response.auth.oauth2.OAuth2UserInfo;
import com.exit.gateway.entity.CustomOAuth2User;
import com.exit.gateway.entity.OAuth2UserInfoFactory;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserGrpcClient userGrpcClient;

    @Override
    public CustomOAuth2User loadUser(OAuth2UserRequest request) throws OAuth2AuthenticationException {
        // 1. 기본 OAuth2UserService로 사용자 정보 가져오기
        OAuth2User oauth2User = super.loadUser(request);

        String registrationId = request.getClientRegistration().getRegistrationId();
        OAuth2UserInfo userInfo = OAuth2UserInfoFactory.getOAuth2UserInfo(registrationId, oauth2User.getAttributes());

        // 2. Cookie에서 deviceId와 deviceType 추출
        DeviceInfo deviceInfo = extractDeviceInfoFromCookie();

        // 3. gRPC 호출 시 deviceId와 deviceType 전달
        SocialLoginResponse socialLoginResponse = userGrpcClient.socialLogin(
                userInfo,
                deviceInfo.deviceId(),
                deviceInfo.deviceType()
        );

        return createCustomOAuth2User(socialLoginResponse);
    }

    private DeviceInfo extractDeviceInfoFromCookie() {
        String deviceId = null;
        String deviceType = "WEB";

        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest httpRequest = attributes.getRequest();
                Cookie[] cookies = httpRequest.getCookies();

                if (cookies != null) {
                    for (Cookie cookie : cookies) {
                        if ("device_id".equals(cookie.getName())) {
                            deviceId = cookie.getValue();
                        } else if ("device_type".equals(cookie.getName())) {
                            deviceType = cookie.getValue();
                        }
                    }
                }
            }

            if (deviceId != null) {
                log.info("Extracted device info from cookie - deviceId: {}, deviceType: {}", deviceId, deviceType);
            }
        } catch (Exception e) {
            log.warn("Failed to extract device info from cookie", e);
        }

        // deviceId가 없으면 UUID 생성
        if (deviceId == null || deviceId.isEmpty()) {
            deviceId = UUID.randomUUID().toString();
            log.info("No deviceId in cookie, generated UUID: {}", deviceId);
        }

        return new DeviceInfo(deviceId, deviceType);
    }

    private CustomOAuth2User createCustomOAuth2User(SocialLoginResponse response) {
        return CustomOAuth2User.builder()
                .userId(response.getUserId())
                .nickname(response.getNickname())
                .email(response.getEmail())
                .registrationId(response.getSocialId())
                .accessToken(response.getAccessToken())
                .refreshToken(response.getRefreshToken())
                .build();
    }

    private record DeviceInfo(String deviceId, String deviceType) {
    }
}