package com.exit.gateway.global.util.mapper.error.question;

import com.exit.common.response.error.rest.ErrorCode;
import com.exit.common.response.error.rest.magazine.MagazineErrorCode;
import com.exit.common.response.error.rest.question.QuestionErrorCode;
import com.exit.gateway.global.util.mapper.error.GrpcErrorMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class QuestionGrpcErrorMapper implements GrpcErrorMapper {

    private static final Map<String, QuestionErrorCode> ERROR_CODE_MAP = Map.ofEntries(
            Map.entry("QUESTION_ERR_010", QuestionErrorCode.CREATE_QUESTION_FAIL),        // CREATE_QUESTION_FAILED
            Map.entry("QUESTION_ERR_011", QuestionErrorCode.QUESTION_REPORT_FAIL),                    // QUESTION_REPORT_FAILED
            Map.entry("QUESTION_ERR_012", QuestionErrorCode.GET_QUESTION_LIST_FAIL),                      // GET_QUESTION_LIST_FAILED
            Map.entry("QUESTION_ERR_013", QuestionErrorCode.GET_QUESTION_DETAIL_FAIL),          // GET_QUESTION_DETAIL_FAILED
            Map.entry("QUESTION_ERR_014", QuestionErrorCode.GET_POPULAR_POST_FAIL),                    // GET_POPULAR_POST_FAILED
            Map.entry("QUESTION_ERR_015", QuestionErrorCode.GET_MY_QUESTION_FAIL),                    // GET_MY_QUESTION_FAILED
            Map.entry("QUESTION_ERR_016", QuestionErrorCode.CATEGORY_RECOMMEND_FAIL),                    // CATEGORY_RECOMMEND_FAILED
            Map.entry("QUESTION_ERR_017", QuestionErrorCode.SIMILAR_QUESTION_FAIL)                    // SIMILAR_QUESTION_FAILED
    );

    @Override
    public ErrorCode mapToErrorCode(String developCode, String grpcStatusCode) {
        QuestionErrorCode errorCode = ERROR_CODE_MAP.get(developCode);

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
