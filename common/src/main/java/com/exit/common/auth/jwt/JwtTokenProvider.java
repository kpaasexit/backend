package com.exit.common.auth.jwt;

import com.exit.common.auth.jwt.dto.UserIdRequest;
import com.exit.common.exception.rest.RestApiException;
import com.exit.common.properties.JwtProperties;
import com.exit.common.response.error.rest.UserErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.util.Date;

@Component
@RequiredArgsConstructor
@Validated
public class JwtTokenProvider {
    private final JwtProperties jwtProperties;

    // AccessToken 생성
    public String generateAccessToken(UserIdRequest userDetail) {
        Claims claims = getClaimsFrom(userDetail);
        return getTokenFrom(claims, jwtProperties.getAccessTokenValidTime() * 1000);
    }

    // AccessToken용 Claim 생성
    private Claims getClaimsFrom(@Valid UserIdRequest userDetail) {
        Claims claims = Jwts.claims();
        claims.put("userId", userDetail.userId());
        return claims;
    }

    // RefrshToken 생성
    public String generateRefreshToken(@Valid UserIdRequest user, String tokenId) {
        Claims claims = getClaimsFrom(user, tokenId);
        return getTokenFrom(claims, jwtProperties.getRefreshTokenValidTime() * 1000);
    }

    // RefreshToken용 Claim 생성
    private Claims getClaimsFrom(@Valid UserIdRequest user, String tokenId) {
        Claims claims = Jwts.claims();
        claims.put("userId", user.userId());
        claims.put("tokenId", tokenId);
        return claims;
    }

    // claim 정보로 Token 얻기
    private String getTokenFrom(Claims claims, Long validTime) {
        Date now = new Date();
        return Jwts.builder()
                .setHeaderParam("type", "JWT")
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + validTime))
                .signWith(
                        Keys.hmacShaKeyFor(jwtProperties.getBytesSecretKey()),
                        SignatureAlgorithm.HS256
                )
                .compact();
    }

    // AccessToken 값만 남도록 접두사 삭제
    public String extractAccessToken(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            return token.substring(7);
        }
        return token;
    }

    public boolean isExpiredToken(String token) {
        try {
            Claims claims = getClaimsByToken(token);
            return claims.getExpiration().before(new Date());
        } catch (ExpiredJwtException e) {
            return false;
        }
    }

    // 토큰으로부터 유저 ID 얻기
    public Long getUserIdFromToken(String token) {
        try {
            Claims claims = getClaimsByToken(token);
            return claims.get("userId", Long.class);
        } catch (ExpiredJwtException e) {
            throw new RestApiException(UserErrorCode.EXPIRED_TOKEN);
        } catch (Exception e) {
            throw new RestApiException(UserErrorCode.INVALID_TOKEN);
        }
    }

    // 토큰으로부터 토큰 ID 얻기
    public Long getTokenIdFromToken(String token) {
        try {
            Claims claims = getClaimsByToken(token);
            return Long.parseLong(String.valueOf(claims.get("tokenId")));
        } catch (ExpiredJwtException e) {
            throw new RestApiException(UserErrorCode.EXPIRED_TOKEN);
        } catch (Exception e) {
            throw new RestApiException(UserErrorCode.INVALID_TOKEN);
        }
    }

    private Claims getClaimsByToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(Keys.hmacShaKeyFor(jwtProperties.getBytesSecretKey()))
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
