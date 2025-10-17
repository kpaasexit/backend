package com.exit.gateway.global.util.mapper.error.user;

import com.exit.common.response.error.rest.ErrorCode;
import com.exit.common.response.error.rest.user.UserErrorCode;
import com.exit.gateway.global.util.mapper.error.GrpcErrorMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class UserGrpcErrorMapper implements GrpcErrorMapper {

    private static final Map<String, UserErrorCode> ERROR_CODE_MAP = Map.ofEntries(
            // 사용자 정보 조회 실패
            Map.entry("USER_ERR_019", UserErrorCode.GET_USER_INFO_FAIL),               // GET_USER_NAME_AND_PROFILE_FAILED

            // 사용자 정보 업데이트 실패
            Map.entry("USER_ERR_012", UserErrorCode.UPDATE_DEVICE_FAIL),               // UPDATE_DEVICE_FAILED
            Map.entry("USER_ERR_022", UserErrorCode.UPDATE_ADDITIONAL_INFO_FAIL),      // UPDATE_ADDITIONAL_INFO_FAILED

            // 기타 작업 실패
            Map.entry("USER_ERR_024", UserErrorCode.CHECK_NICKNAME_DUPLICATE_FAIL)     // CHECK_NICKNAME_DUPLICATE_FAILED
    );

    @Override
    public ErrorCode mapToErrorCode(String developCode, String grpcStatusCode) {
        UserErrorCode errorCode = ERROR_CODE_MAP.get(developCode);

        if (errorCode == null) {
            log.warn("알 수 없는 User gRPC 에러코드: {}. 기본 유저 에러 코드 사용", developCode);
            return getDefaultErrorCode();
        }

        log.debug("gRPC 에러 코드 변환: {} -> {}", developCode, errorCode.getDevelopCode());
        return errorCode;
    }

    @Override
    public ErrorCode getDefaultErrorCode() {
        return UserErrorCode.AVAILABLE_USER_SERVER;
    }
}
