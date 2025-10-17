package com.exit.gateway.global.util.mapper.error.quiz;

import com.exit.common.response.error.rest.ErrorCode;
import com.exit.common.response.error.rest.quiz.QuizErrorCode;
import com.exit.gateway.global.util.mapper.error.GrpcErrorMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class QuizGrpcErrorMapper implements GrpcErrorMapper {

    private static final Map<String, QuizErrorCode> ERROR_CODE_MAP = Map.ofEntries(
            // 카테고리 통계 조회 실패
            Map.entry("QUIZ_ERR_020", QuizErrorCode.GET_CATEGORY_STATISTICS_FAIL),      // GET_CATEGORY_STATISTICS_FAILED

            // 퀴즈 조회 실패
            Map.entry("QUIZ_ERR_001", QuizErrorCode.GET_QUIZ_FAIL),                     // NO_AVAILABLE_QUIZ
            Map.entry("QUIZ_ERR_021", QuizErrorCode.GET_QUIZ_FAIL),                     // GET_QUIZ_FAILED

            // 오늘의 퀴즈 조회 실패
            Map.entry("QUIZ_ERR_022", QuizErrorCode.GET_TODAY_QUIZ_FAIL),               // GET_TODAY_QUIZ_FAILED

            // 풀었던 퀴즈 조회 실패
            Map.entry("QUIZ_ERR_023", QuizErrorCode.GET_SOLVED_QUIZ_FAIL),              // GET_SOLVED_QUIZ_FAILED

            // 퀴즈 제출 실패
            Map.entry("QUIZ_ERR_002", QuizErrorCode.SUBMIT_ANSWER_FAIL),                // NOT_FOUND_QUIZ
            Map.entry("QUIZ_ERR_030", QuizErrorCode.SUBMIT_ANSWER_FAIL),                // SUBMIT_ANSWER_FAILED
            Map.entry("QUIZ_ERR_031", QuizErrorCode.SUBMIT_ANSWER_FAIL)                 // SAVE_QUIZ_ATTEMPT_FAILED
    );

    @Override
    public ErrorCode mapToErrorCode(String developCode, String grpcStatusCode) {
        QuizErrorCode errorCode = ERROR_CODE_MAP.get(developCode);

        if (errorCode == null) {
            log.warn("알 수 없는 User gRPC 에러코드: {}. 기본 퀴즈 에러 코드 사용", developCode);
            return getDefaultErrorCode();
        }

        log.debug("gRPC 에러 코드 변환: {} -> {}", developCode, errorCode.getDevelopCode());
        return errorCode;
    }

    @Override
    public ErrorCode getDefaultErrorCode() {
        return QuizErrorCode.AVAILABLE_QUIZ_SERVER;
    }
}
