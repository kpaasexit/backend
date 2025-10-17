package com.exit.gateway.global.util.mapper.error.question;

import com.exit.common.response.error.rest.ErrorCode;
import com.exit.common.response.error.rest.magazine.MagazineErrorCode;
import com.exit.common.response.error.rest.question.CommentErrorCode;
import com.exit.common.response.error.rest.question.QuestionErrorCode;
import com.exit.gateway.global.util.mapper.error.GrpcErrorMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class CommentGrpcErrorMapper implements GrpcErrorMapper {

    private static final Map<String, CommentErrorCode> ERROR_CODE_MAP = Map.ofEntries(
            Map.entry("COMMENT_ERR_010", CommentErrorCode.CREATE_COMMENT_FAIL),        // CREATE_COMMENT_FAILED
            Map.entry("COMMENT_ERR_011", CommentErrorCode.DELETE_COMMENT_FAIL),                    // DELETE_COMMENT_FAILED
            Map.entry("COMMENT_ERR_012", CommentErrorCode.GET_COMMENT_FAIL)                      // GET_COMMENT_FAILED
    );

    @Override
    public ErrorCode mapToErrorCode(String developCode, String grpcStatusCode) {
        CommentErrorCode errorCode = ERROR_CODE_MAP.get(developCode);

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
