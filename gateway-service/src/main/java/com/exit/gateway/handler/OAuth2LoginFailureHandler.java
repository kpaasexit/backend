package com.exit.gateway.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Slf4j
public class OAuth2LoginFailureHandler implements AuthenticationFailureHandler {

    @Value("${app.oauth2.redirect-url:http://localhost:3000/oauth2/redirect}")
    private String redirectUrl;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {

        log.error("OAuth2 로그인 실패", exception);

        // 실패 시 에러 파라미터와 함께 리다이렉트
        String errorRedirectUrl = redirectUrl + "?error=authentication_failed&message=" +
                exception.getMessage();

        response.sendRedirect(errorRedirectUrl);
    }
}