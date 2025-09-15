package com.exit.gateway.config.filter;

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
                filterChain.doFilter(request, response);
                return;
            }

            String accessToken = jwtTokenProvider.extractAccessToken(request);
            if (accessToken != null) {
                if (jwtTokenProvider.isExpiredToken(accessToken)) {
                    throw new RestApiException(UserErrorCode.EXPIRED_TOKEN);
                }

                Authentication authentication = jwtAuthenticationProvider.authenticate(new JwtAuthenticationToken(accessToken));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }

            filterChain.doFilter(request, response);
        } catch (RestApiException ex) {
            response.setStatus(ex.getErrorCode().getHttpStatus().value());
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");

            Map<String, Object> errorResponse = Map.of(
                    "httpStatus", ex.getErrorCode().getHttpStatus().value(),
                    "errorCodeResponse", ex.getErrorCode(),
                    "errorMessage", ex.getErrorCode().getErrorDescription()
            );

            response.getWriter().write(new ObjectMapper().writeValueAsString(errorResponse));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/api/auth/refresh");
    }
}
