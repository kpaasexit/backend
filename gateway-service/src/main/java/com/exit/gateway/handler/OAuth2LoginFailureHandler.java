package com.exit.gateway.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class OAuth2LoginFailureHandler implements AuthenticationFailureHandler {

    @Value("${app.oauth2.redirect-url}")
    private String redirectUrl;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {

        log.error("OAuth2 로그인 실패", exception);

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8");

        Map<String,Object> body = new HashMap<>();
        body.put("errorCode", "AUTH_ERR_015".toUpperCase());
        body.put("errorDescription", "소셜 로그인에 실패했습니다.");
        body.put("errors", 1001);
        body.put("details", exception.getMessage());
        response.getWriter().write(new ObjectMapper().writeValueAsString(body));
        response.getWriter().flush();
//        // 실패 시 에러 파라미터와 함께 리다이렉트
//        String errorRedirectUrl = redirectUrl + "?error=authentication_failed&message=" +
//                exception.getMessage();
//
//        response.sendRedirect(errorRedirectUrl);
    }
}