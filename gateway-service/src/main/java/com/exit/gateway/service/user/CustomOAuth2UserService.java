package com.exit.gateway.service.user;

import com.exit.common.grpc.SocialLoginResponse;
import com.exit.gateway.controller.user.dto.response.auth.oauth2.OAuth2UserInfo;
import com.exit.gateway.entity.CustomOAuth2User;
import com.exit.gateway.entity.OAuth2UserInfoFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

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

        SocialLoginResponse socialLoginResponse = userGrpcClient.socialLogin(userInfo);

        return createCustomOAuth2User(socialLoginResponse);
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
}