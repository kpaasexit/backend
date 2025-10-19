package com.exit.gateway.global.util.mapper.error.question;

import com.exit.common.response.error.rest.ErrorCode;
import com.exit.common.response.error.rest.question.QuestionErrorCode;
import com.exit.common.response.error.rest.question.ResponseErrorCode;
import com.exit.gateway.global.util.mapper.error.GrpcErrorMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class ResponseGrpcErrorMapper implements GrpcErrorMapper {

    private static final Map<String, ResponseErrorCode> ERROR_CODE_MAP = Map.ofEntries(
            Map.entry("RESPONSE_ERR_010", ResponseErrorCode.CREATE_RESPONSE_FAIL),        // CREATE_ANSWER_FAILED
            Map.entry("RESPONSE_ERR_011", ResponseErrorCode.RECOMMEND_RESPONSE_FAIL),                    // TOGGLE_ANSWER_LIKE_FAILED
            Map.entry("RESPONSE_ERR_012", ResponseErrorCode.UPDATE_RESPONSE_FAIL),                      // UPDATE_RESPONSE_FAILED
            Map.entry("RESPONSE_ERR_013", ResponseErrorCode.ADOPT_RESPONSE_FAIL),          // ANSWER_ADOPT_FAILED
            Map.entry("RESPONSE_ERR_014", ResponseErrorCode.DELETE_RESPONSE_FAIL),          // DELETE_RESPONSE_FAILED
            Map.entry("RESPONSE_ERR_015", ResponseErrorCode.RESPONSE_REPORT_FAIL),          // ANSWER_REPORT_FAILED
            Map.entry("RESPONSE_ERR_016", ResponseErrorCode.GET_DETAIL_RESPONSE_FAIL)                    // GET_DETAIL_RESPONSE_FAILED
    );

    @Override
    public ErrorCode mapToErrorCode(String developCode, String grpcStatusCode) {
        ResponseErrorCode errorCode = ERROR_CODE_MAP.get(developCode);

        if (errorCode == null) {
            log.warn("알 수 없는 User gRPC 에러코드: {}. 기본 질문 에러 코드 사용", developCode);
            return getDefaultErrorCode();
        }

        log.debug("gRPC 에러 코드 변환: {} -> {}", developCode, errorCode.getDevelopCode());
        return errorCode;
    }

    @Override
    public ErrorCode getDefaultErrorCode() {
        return QuestionErrorCode.AVAILABLE_QUESTION_SERVER;
    }
}
