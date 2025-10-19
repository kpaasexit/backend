package com.exit.gateway.global.util.mapper.error;

import com.exit.common.response.error.rest.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;

@Slf4j
public class DefaultGrpcErrorMapper implements GrpcErrorMapper {
    @Override
    public ErrorCode mapToErrorCode(String developCode, String grpcStatusCode) {
        log.warn("특정 개발 에러 코드를 찾을 수 없습니다. developCode: {}. 기본 에러 코드 사용", developCode);
        return getDefaultErrorCode();
    }

    @Override
    public ErrorCode getDefaultErrorCode() {
        return new ErrorCode() {
            @Override
            public String getDevelopCode() {
                return "COMMON_ERR_001";
            }

            @Override
            public HttpStatus getHttpStatus() {
                return HttpStatus.INTERNAL_SERVER_ERROR;
            }

            @Override
            public String getErrorDescription() {
                return "서버 내부 오류가 발생했습니다.";
            }
        };
    }
}
