package com.exit.gateway.global.util.mapper.error.question;

import com.exit.common.response.error.rest.ErrorCode;
import com.exit.common.response.error.rest.magazine.MagazineErrorCode;
import com.exit.common.response.error.rest.question.AdditionalQuestionErrorCode;
import com.exit.common.response.error.rest.question.QuestionErrorCode;
import com.exit.gateway.global.util.mapper.error.GrpcErrorMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class AdditionalQuestionGrpcErrorMapper implements GrpcErrorMapper {

    private static final Map<String, AdditionalQuestionErrorCode> ERROR_CODE_MAP = Map.ofEntries(
            Map.entry("ADDITIONAL_QUESTION_ERR_010", AdditionalQuestionErrorCode.CREATE_ADDITIONAL_QUESTION_FAIL),  // CREATE_ADDITIONAL_QUESTION_MESSAGE_FAILED
            Map.entry("ADDITIONAL_QUESTION_ERR_011", AdditionalQuestionErrorCode.GET_ADDITIONAL_QUESTION_LIST_FAIL)               // GET_ADDITIONAL_QUESTION_FAILED
    );

    @Override
    public ErrorCode mapToErrorCode(String developCode, String grpcStatusCode) {
        AdditionalQuestionErrorCode errorCode = ERROR_CODE_MAP.get(developCode);

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
