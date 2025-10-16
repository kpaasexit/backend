package com.exit.gateway.global.util.mapper.error.auth;

import com.exit.common.response.error.rest.ErrorCode;
import com.exit.common.response.error.rest.user.AuthErrorCode;
import com.exit.common.response.error.rest.user.UserErrorCode;
import com.exit.gateway.global.util.mapper.error.GrpcErrorMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
public class AuthGrpcErrorMapper implements GrpcErrorMapper {
    // developCode -> REST ErrorCode 매핑
    private static final Map<String, AuthErrorCode> ERROR_CODE_MAP = Map.ofEntries(
            Map.entry("AUTH_ERR_005", AuthErrorCode.REFRESH_FAIL),          // INVALID_REFRESH_TOKEN
            Map.entry("AUTH_ERR_015", AuthErrorCode.REFRESH_FAIL),          // REFRESH_TOKEN_FAILED
            Map.entry("AUTH_ERR_006", AuthErrorCode.LOGOUT_FAIL),           // LOGOUT_FAILED
            Map.entry("AUTH_ERR_007", AuthErrorCode.SOCIAL_LOGIN_FAIL),     // SOCIAL_LOGIN_FAILED
            Map.entry("AUTH_ERR_012", AuthErrorCode.DELETED_USER_FAIL),     // WITHDRAW_FAILED
            Map.entry("AUTH_ERR_013", AuthErrorCode.SOCIAL_LOGIN_FAIL)      // USER_CREATION_FAILED
    );

    @Override
    public ErrorCode mapToErrorCode(String developCode, String grpcStatusCode) {
        AuthErrorCode errorCode = ERROR_CODE_MAP.get(developCode);

        if (errorCode == null) {
            log.warn("알 수 없는 에러코드: {}. 기본 유저 에러 코드 사용", developCode);
            return getDefaultErrorCode();
        }

        return errorCode;
    }

    @Override
    public ErrorCode getDefaultErrorCode() {
        return UserErrorCode.AVAILABLE_USER_SERVER;
    }
}