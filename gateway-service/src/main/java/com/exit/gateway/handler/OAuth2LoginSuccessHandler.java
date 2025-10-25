package com.exit.gateway.handler;

import com.exit.common.exception.rest.RestApiException;
import com.exit.common.properties.JwtProperties;
import com.exit.gateway.controller.user.dto.response.auth.oauth2.KakaoOAuth2UserInfo;
import com.exit.gateway.controller.user.dto.response.auth.oauth2.NaverOAuth2UserInfo;
import com.exit.gateway.controller.user.dto.response.auth.oauth2.OAuth2UserInfo;
import com.exit.gateway.entity.CustomOAuth2User;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final HttpCookieOAuth2AuthorizationRequestRepository authorizationRequestRepository;
    private final JwtProperties jwtProperties;

    @Value("${app.oauth2.allowed-origins}")
    private String allowedOriginsRaw;

    @Value("${app.oauth2.default-client-url:https://localhost:5761}")
    private String defaultClientUrl;

    @Value("${app.oauth2.cookie-secure:true}")
    private boolean cookieSecure;

    private List<String> allowedOrigins;


    @PostConstruct
    public void init() {
        this.allowedOrigins = Arrays.stream(allowedOriginsRaw.split("\\s*,\\s*")).toList();

        log.info("=== OAuth2LoginSuccessHandler 초기화 ===");
        log.info("Allowed Origins: {}", allowedOrigins);
        log.info("Default Client URL: {}", defaultClientUrl);
        log.info("Cookie Secure: {}", cookieSecure);
        log.info("=======================================");
    }

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

            String clientUrl = determineClientUrl(request);
            addCookie(response, "refreshToken", oauth2User.getRefreshToken(), jwtProperties.getRefreshTokenExpiration().intValue());

            String finalRedirectUrl = String.format(
                    "%s/oauth/callback?returnTo=%s&accessToken=%s", clientUrl,
                    URLEncoder.encode(returnTo, StandardCharsets.UTF_8),
                    URLEncoder.encode(oauth2User.getAccessToken(), StandardCharsets.UTF_8)
            );

            authorizationRequestRepository.removeAuthorizationRequestCookies(request, response);
            log.info("OAuth2 로그인 완료 - 최종 리다이렉트: {}", finalRedirectUrl);

            response.sendRedirect(finalRedirectUrl);

        } catch (Exception e) {
            log.error("OAuth2 로그인 처리 중 오류 발생", e);
            response.sendRedirect(defaultClientUrl + "/oauth/callback?success=false");
        }
    }

    private String determineClientUrl(HttpServletRequest request) {
        // 1. 쿠키에서 저장된 client origin 확인 (가장 우선)
        String savedOrigin = getCookie(request, HttpCookieOAuth2AuthorizationRequestRepository.CLIENT_ORIGIN_COOKIE_NAME)
                .map(Cookie::getValue)
                .orElse(null);

        log.info("Saved client origin from cookie: {}", savedOrigin);

        if (savedOrigin != null && isAllowedOrigin(savedOrigin)) {
            log.info("✅ Using saved origin: {}", savedOrigin);
            return savedOrigin;
        }

        // 2. Origin 헤더 확인
        String origin = request.getHeader("Origin");
        log.info("Origin Header: {}", origin);

        if (origin != null && !origin.isEmpty() && isAllowedOrigin(origin)) {
            log.info("✅ Using Origin: {}", origin);
            return origin;
        }

        // 3. 기본값
        log.info("⚠️ Using default: {}", defaultClientUrl);
        return defaultClientUrl;
    }

    private boolean isAllowedOrigin(String origin) {
        if (origin == null) return false;

        return allowedOrigins.stream()
                .anyMatch(pattern -> {
                    if (pattern.contains("*")) {
                        String regex = pattern.replace(".", "\\.").replace("*", ".*");
                        return origin.matches(regex);
                    } else {
                        return pattern.equals(origin);
                    }
                });
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

    private void addCookie(HttpServletResponse response, String name, String value, int maxAge) {
        ResponseCookie cookie = ResponseCookie
                .from(name, value)
                .path("/")
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(cookieSecure ? "None" : "Lax")
                .maxAge(Duration.ofSeconds(maxAge))
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}