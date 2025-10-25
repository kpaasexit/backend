package com.exit.gateway.global.config;

import com.exit.gateway.global.filter.JwtAuthenticationFilter;
import com.exit.gateway.handler.HttpCookieOAuth2AuthorizationRequestRepository;
import com.exit.gateway.handler.OAuth2LoginFailureHandler;
import com.exit.gateway.handler.OAuth2LoginSuccessHandler;
import com.exit.gateway.service.user.CustomOAuth2UserService;
import io.grpc.netty.shaded.io.netty.handler.codec.http.HttpMethod;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.util.Arrays;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
    private final OAuth2LoginFailureHandler oAuth2LoginFailureHandler;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final HttpCookieOAuth2AuthorizationRequestRepository authorizationRequestRepository;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptionHandling ->
                        exceptionHandling.authenticationEntryPoint(new FailedAuthenticationEntryPoint()))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(CorsUtils::isPreFlightRequest).permitAll()
                        .requestMatchers("/oauth2/**", "/login/**", "/api/auth/refresh").permitAll()
                        .requestMatchers("/actuator/health/**", "/actuator/info", "/actuator").permitAll()
                        .requestMatchers("/back-docs/**", "/favicon.ico").permitAll()
                        .requestMatchers(getPublicApiEndpoints()).permitAll()
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2
                        .authorizationEndpoint(authorizationEndpoint ->
                                authorizationEndpoint
                                        .baseUri("/oauth2/authorization")
                                        .authorizationRequestRepository(authorizationRequestRepository))
                        .redirectionEndpoint(redirectEndpoint ->
                                redirectEndpoint.baseUri("/login/oauth2/code/*"))
                        .userInfoEndpoint(userInfoEndpoint ->
                                userInfoEndpoint.userService(customOAuth2UserService))
                        .successHandler(oAuth2LoginSuccessHandler)
                        .failureHandler(oAuth2LoginFailureHandler)
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.asList(
                "https://localhost:*",
                "http://localhost:*",
                "https://house-it.*"
        ));
        configuration.setAllowedMethods(Arrays.asList("HEAD", "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    static class FailedAuthenticationEntryPoint implements AuthenticationEntryPoint {
        @Override
        public void commence(HttpServletRequest request,
                             HttpServletResponse response,
                             AuthenticationException authException) throws IOException {
            response.setContentType("application/json");
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);

            response.getWriter().write("{\"code\": \"NP\", \"message\": \"No Permission.\"}");
        }
    }

    private String[] getPublicApiEndpoints() {
        return new String[]{
                "/api/magazine/category/*",
                "/api/magazine/{id:\\d+}",
                "/api/magazine/recommend",
                "/api/questions",
                "/api/questions/{id:\\d+}",
                "/api/questions/popular-post",
                "/api/responses/{id:\\d+}/responses",
                "/api/responses/ai",
                "/api/comments/{id:\\d+}",
                "/api/additional-question/{id:\\d+}",
                "/api/search",
                "/api/search/recommend",
                "/api/search/magazines",
                "/api/search/questions"
        };
    }
}