package com.exit.gateway.handler;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.SerializationUtils;

import java.util.Base64;

@Component
@Slf4j
public class HttpCookieOAuth2AuthorizationRequestRepository implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    public static final String OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME = "oauth2_auth_request";
    public static final String RETURN_TO_URI_PARAM_COOKIE_NAME = "return_to";
    public static final String CLIENT_ORIGIN_COOKIE_NAME = "client_origin";
    private static final int cookieExpireSeconds = 180;

    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        return getCookie(request, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME)
                .map(cookie -> deserialize(cookie.getValue(), OAuth2AuthorizationRequest.class))
                .orElse(null);
    }

    @Override
    public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest,
                                         HttpServletRequest request,
                                         HttpServletResponse response) {
        if (authorizationRequest == null) {
            deleteCookie(request, response, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME);
            deleteCookie(request, response, RETURN_TO_URI_PARAM_COOKIE_NAME);
            deleteCookie(request, response, CLIENT_ORIGIN_COOKIE_NAME);
            return;
        }

        addCookie(response, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME,
                serialize(authorizationRequest), cookieExpireSeconds);

        String returnTo = request.getParameter("returnTo");
        if (returnTo != null && !returnTo.isEmpty()) {
            addCookie(response, RETURN_TO_URI_PARAM_COOKIE_NAME, returnTo, cookieExpireSeconds);
            log.info("Saved returnTo in cookie: {}", returnTo);
        }

        String clientOrigin = extractClientOrigin(request);
        if (clientOrigin != null) {
            addCookie(response, CLIENT_ORIGIN_COOKIE_NAME, clientOrigin, cookieExpireSeconds);
            log.info("Saved client origin in cookie: {}", clientOrigin);
        }
    }

    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request, HttpServletResponse response) {
        return loadAuthorizationRequest(request);
    }

    public void removeAuthorizationRequestCookies(HttpServletRequest request, HttpServletResponse response) {
        deleteCookie(request, response, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME);
        deleteCookie(request, response, RETURN_TO_URI_PARAM_COOKIE_NAME);
        deleteCookie(request, response, CLIENT_ORIGIN_COOKIE_NAME);
    }

    private String extractClientOrigin(HttpServletRequest request) {
        String origin = request.getHeader("Origin");

        if (origin == null || origin.isEmpty()) {
            String referer = request.getHeader("Referer");
            if (referer != null && !referer.isEmpty()) {
                try {
                    java.net.URL url = new java.net.URL(referer);
                    int port = url.getPort();
                    if (port == -1) {
                        origin = url.getProtocol() + "://" + url.getHost();
                    } else {
                        origin = url.getProtocol() + "://" + url.getHost() + ":" + port;
                    }
                } catch (Exception e) {
                    log.warn("Failed to parse referer: {}", referer);
                }
            }
        }

        log.info("Extracted client origin: {}", origin);
        return origin;
    }

    private java.util.Optional<Cookie> getCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (name.equals(cookie.getName())) {
                    return java.util.Optional.of(cookie);
                }
            }
        }
        return java.util.Optional.empty();
    }

    private void addCookie(HttpServletResponse response, String name, String value, int maxAge) {
        Cookie cookie = new Cookie(name, value);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(maxAge);
        response.addCookie(cookie);
    }

    private void deleteCookie(HttpServletRequest request, HttpServletResponse response, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (name.equals(cookie.getName())) {
                    cookie.setValue("");
                    cookie.setPath("/");
                    cookie.setMaxAge(0);
                    response.addCookie(cookie);
                }
            }
        }
    }

    private String serialize(Object object) {
        return Base64.getUrlEncoder().encodeToString(SerializationUtils.serialize(object));
    }

    private <T> T deserialize(String value, Class<T> cls) {
        return cls.cast(SerializationUtils.deserialize(Base64.getUrlDecoder().decode(value)));
    }
}