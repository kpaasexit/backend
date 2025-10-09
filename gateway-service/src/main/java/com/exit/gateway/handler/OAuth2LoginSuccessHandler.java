package com.exit.gateway.handler;

import com.exit.gateway.controller.user.dto.response.auth.oauth2.KakaoOAuth2UserInfo;
import com.exit.gateway.controller.user.dto.response.auth.oauth2.NaverOAuth2UserInfo;
import com.exit.gateway.controller.user.dto.response.auth.oauth2.OAuth2UserInfo;
import com.exit.gateway.entity.CustomOAuth2User;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final HttpCookieOAuth2AuthorizationRequestRepository authorizationRequestRepository;

    @Value("${app.oauth2.client-url:https://localhost:5173}")
    private String clientUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        CustomOAuth2User oauth2User = (CustomOAuth2User) authentication.getPrincipal();

        OAuth2UserInfo userInfo;
        if (oauth2User.getRegistrationId().equals("naver")) {
            userInfo = new NaverOAuth2UserInfo(oauth2User.getAttributes());
        } else {
            userInfo = new KakaoOAuth2UserInfo(oauth2User.getAttributes());
        }

        log.info("OAuth2 로그인 성공 - Provider: {}, User: {}", userInfo.getProvider(), userInfo.getName());

        try {
            deleteCookie(response, "device_id");
            deleteCookie(response, "device_type");

            String returnTo = getCookie(request, HttpCookieOAuth2AuthorizationRequestRepository.RETURN_TO_URI_PARAM_COOKIE_NAME)
                    .map(Cookie::getValue)
                    .orElse("/");

            String finalRedirectUrl = String.format(
                    "%s/oauth/callback?token=%s&refresh=%s&returnTo=%s",
                    clientUrl,
                    URLEncoder.encode(oauth2User.getAccessToken(), StandardCharsets.UTF_8),
                    URLEncoder.encode(oauth2User.getRefreshToken(), StandardCharsets.UTF_8),
                    URLEncoder.encode(returnTo, StandardCharsets.UTF_8)
            );

            authorizationRequestRepository.removeAuthorizationRequestCookies(request, response);

            log.info("OAuth2 로그인 완료 - Client URL: {}", clientUrl);
            log.info("OAuth2 로그인 완료 - Return To: {}", returnTo);
            log.info("OAuth2 로그인 완료 - 최종 리다이렉트: {}", finalRedirectUrl);

            response.sendRedirect(finalRedirectUrl);

        } catch (Exception e) {
            log.error("OAuth2 로그인 처리 중 오류 발생", e);
            response.sendRedirect(clientUrl + "/login?error=login_failed");
        }
    }

    private void deleteCookie(HttpServletResponse response, String name) {
        Cookie cookie = new Cookie(name, null);
        cookie.setMaxAge(0);
        cookie.setPath("/");
        response.addCookie(cookie);
    }

    private Optional<Cookie> getCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            return Arrays.stream(cookies)
                    .filter(cookie -> name.equals(cookie.getName()))
                    .findFirst();
        }
        return Optional.empty();
    }
}