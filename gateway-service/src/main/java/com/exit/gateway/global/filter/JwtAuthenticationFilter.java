package com.exit.gateway.global.filter;

import com.exit.common.auth.jwt.JwtAuthenticationProvider;
import com.exit.common.auth.jwt.JwtAuthenticationToken;
import com.exit.common.auth.jwt.JwtTokenProvider;
import com.exit.common.exception.rest.RestApiException;
import com.exit.common.response.error.rest.UserErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtAuthenticationProvider jwtAuthenticationProvider;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        try {
            if (shouldNotFilter(request)) {
                log.debug("Skipping JWT filter for path: {}", request.getRequestURI());
                filterChain.doFilter(request, response);
                return;
            }

            log.debug("Processing JWT for path: {}", request.getRequestURI());
            String accessToken = jwtTokenProvider.extractAccessToken(request);
            if (accessToken != null) {
                log.debug("JWT token found, validating...");
                if (jwtTokenProvider.isExpiredToken(accessToken)) {
                    throw new RestApiException(UserErrorCode.EXPIRED_TOKEN);
                }

                Authentication authentication = jwtAuthenticationProvider.authenticate(new JwtAuthenticationToken(accessToken));
                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.debug("JWT authentication successful");
            } else {
                log.debug("No JWT token found, proceeding without authentication");
            }

            filterChain.doFilter(request, response);
        } catch (RestApiException ex) {
            log.error("JWT authentication failed: {}", ex.getMessage());
            response.setStatus(ex.getErrorCode().getHttpStatus().value());
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");

            Map<String, Object> errorResponse = Map.of(
                    "httpStatus", ex.getErrorCode().getHttpStatus().value(),
                    "errorCodeResponse", ex.getErrorCode(),
                    "errorMessage", ex.getErrorCode().getErrorDescription()
            );

            response.getWriter().write(new ObjectMapper().writeValueAsString(errorResponse));
        } catch (org.springframework.security.core.AuthenticationException ex) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");

            // 표준 인증 예외도 동일 포맷으로 응답 (원하면 401로)
            Map<String, Object> errorResponse = Map.of("errorCode", UserErrorCode.INVALID_TOKEN.getDevelopCode(),
                    "errorDescription", UserErrorCode.INVALID_TOKEN.getErrorDescription(),
                    "details", null,
                    "errors", null
            );

            response.getWriter().write(new ObjectMapper().writeValueAsString(errorResponse));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/api/auth/refresh") ||
                path.startsWith("/oauth2/") ||
                path.startsWith("/login/") ||
                path.startsWith("/back-docs") ||
                path.startsWith("/favicon.ico");

    }
}
