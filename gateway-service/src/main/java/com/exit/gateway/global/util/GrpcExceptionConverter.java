package com.exit.gateway.global.util;

import com.exit.common.exception.rest.RestApiException;
import com.exit.common.response.error.rest.ErrorCode;
import com.exit.common.response.error.rest.UserErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * gRPC StatusRuntimeException을 REST API Exception으로 변환하는 유틸리티
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GrpcExceptionConverter {

    private final ObjectMapper objectMapper;

    // developCode -> REST ErrorCode 매핑 테이블
    private static final Map<String, ErrorCode> ERROR_CODE_MAPPING = new HashMap<>();

    static {
        // User/Auth 관련 에러 매핑
        ERROR_CODE_MAPPING.put("USER_ERR_001", UserErrorCode.NULL_USER);
        ERROR_CODE_MAPPING.put("USER_ERR_002", UserErrorCode.INVALID_TOKEN);
        ERROR_CODE_MAPPING.put("USER_ERR_003", UserErrorCode.EXPIRED_TOKEN);
        ERROR_CODE_MAPPING.put("USER_ERR_004", UserErrorCode.NULL_USER);
        ERROR_CODE_MAPPING.put("USER_ERR_005", UserErrorCode.INVALID_REFRESH_TOKEN);
        ERROR_CODE_MAPPING.put("USER_ERR_006", UserErrorCode.LOGOUT_FAIL);
        ERROR_CODE_MAPPING.put("USER_ERR_007", UserErrorCode.NULL_USER);

        // 필요한 다른 매핑들 추가...
    }

    /**
     * gRPC 예외를 REST API 예외로 변환합니다.
     *
     * @param e                 gRPC StatusRuntimeException
     * @param defaultErrorCode  매핑되지 않은 경우 사용할 기본 ErrorCode
     * @return RestApiException
     */
    public RestApiException convert(StatusRuntimeException e, ErrorCode defaultErrorCode) {
        String developCode = extractDevelopCode(e);

        if (developCode != null) {
            ErrorCode mappedErrorCode = ERROR_CODE_MAPPING.get(developCode);
            if (mappedErrorCode != null) {
                log.debug("Converted gRPC error {} to REST error {}", developCode, mappedErrorCode);
                return new RestApiException(mappedErrorCode);
            }
        }

        // 매핑되지 않은 경우 기본 에러 코드 사용
        String errorMessage = getGrpcErrorMessage(e);
        log.debug("Using default error code {} for gRPC error", defaultErrorCode);
        return new RestApiException(defaultErrorCode, errorMessage);
    }

    /**
     * gRPC 예외를 REST API 예외로 변환합니다. (기본 에러 메시지 포함)
     *
     * @param e                 gRPC StatusRuntimeException
     * @param defaultErrorCode  매핑되지 않은 경우 사용할 기본 ErrorCode
     * @param defaultMessage    기본 에러 메시지
     * @return RestApiException
     */
    public RestApiException convert(StatusRuntimeException e, ErrorCode defaultErrorCode, String defaultMessage) {
        String developCode = extractDevelopCode(e);

        if (developCode != null) {
            ErrorCode mappedErrorCode = ERROR_CODE_MAPPING.get(developCode);
            if (mappedErrorCode != null) {
                log.debug("Converted gRPC error {} to REST error {}", developCode, mappedErrorCode);
                return new RestApiException(mappedErrorCode);
            }
        }

        // 매핑되지 않은 경우 기본 에러 코드와 메시지 사용
        log.debug("Using default error code {} for gRPC error", defaultErrorCode);
        return new RestApiException(defaultErrorCode, defaultMessage);
    }

    /**
     * gRPC 에러로부터 developCode를 추출합니다.
     */
    private String extractDevelopCode(StatusRuntimeException e) {
        try {
            String description = e.getStatus().getDescription();
            if (description == null || description.isEmpty()) {
                return null;
            }

            // JSON 파싱 시도
            var errorBody = objectMapper.readTree(description);
            return errorBody.has("developCode") ? errorBody.get("developCode").asText() : null;
        } catch (Exception ex) {
            log.debug("Failed to parse gRPC error description: {}", ex.getMessage());
            return null;
        }
    }

    /**
     * gRPC Status Code에 기반한 기본 에러 메시지를 반환합니다.
     */
    private String getGrpcErrorMessage(StatusRuntimeException e) {
        Status status = e.getStatus();
        return switch (status.getCode()) {
            case INVALID_ARGUMENT -> "잘못된 요청입니다.";
            case UNAUTHENTICATED -> "인증에 실패했습니다.";
            case PERMISSION_DENIED -> "권한이 없습니다.";
            case NOT_FOUND -> "요청한 리소스를 찾을 수 없습니다.";
            case UNAVAILABLE -> "서비스를 일시적으로 사용할 수 없습니다.";
            case INTERNAL -> "서버 내부 오류가 발생했습니다.";
            default -> status.getDescription() != null ? status.getDescription() : "서버 오류가 발생했습니다.";
        };
    }

    /**
     * 새로운 에러 코드 매핑을 추가합니다. (런타임에 동적으로 추가 가능)
     */
    public static void addMapping(String developCode, ErrorCode errorCode) {
        ERROR_CODE_MAPPING.put(developCode, errorCode);
    }
}
