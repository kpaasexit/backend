package com.exit.gateway.global.util.mapper.error.search;

import com.exit.common.response.error.rest.ErrorCode;
import com.exit.common.response.error.rest.question.QuestionErrorCode;
import com.exit.common.response.error.rest.search.SearchErrorCode;
import com.exit.common.response.error.rest.user.AuthErrorCode;
import com.exit.common.response.error.rest.user.UserErrorCode;
import com.exit.gateway.global.util.mapper.error.GrpcErrorMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SearchGrpcErrorMapper implements GrpcErrorMapper {
    // developCode -> REST ErrorCode 매핑
    private static final Map<String, SearchErrorCode> ERROR_CODE_MAP = Map.ofEntries(
            Map.entry("QUESTION_ERR_012", SearchErrorCode.INTEGRATED_SEARCH_FAIL),                      // GET_QUESTION_LIST_FAILED
            Map.entry("MAGAZINE_ERR_060", SearchErrorCode.INTEGRATED_SEARCH_FAIL)                      // SEARCH_INTEGRATED_FAILED
    );

    @Override
    public ErrorCode mapToErrorCode(String developCode, String grpcStatusCode) {
        SearchErrorCode errorCode = ERROR_CODE_MAP.get(developCode);

        if (errorCode == null) {
            log.warn("알 수 없는 에러코드: {}. 기본 유저 에러 코드 사용", developCode);
            return getDefaultErrorCode();
        }

        return errorCode;
    }

    @Override
    public ErrorCode getDefaultErrorCode() {
        return SearchErrorCode.SEARCH_SERVICE_UNAVAILABLE;
    }
}