package com.exit.gateway.global.util.mapper.error.magazine;

import com.exit.common.response.error.rest.ErrorCode;
import com.exit.common.response.error.rest.magazine.MagazineErrorCode;
import com.exit.gateway.global.util.mapper.error.GrpcErrorMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class MagazineGrpcErrorMapper implements GrpcErrorMapper {

    private static final Map<String, MagazineErrorCode> ERROR_CODE_MAP = Map.ofEntries(
            Map.entry("MAGAZINE_ERR_020", MagazineErrorCode.GET_MAGAZINES_BY_CATEGORY_FAIL),        // GET_MAGAZINES_BY_CATEGORY_FAILED
            Map.entry("MAGAZINE_ERR_021", MagazineErrorCode.GET_MAGAZINES_FAIL),                    // GET_MAGAZINE_FAILED
            Map.entry("MAGAZINE_ERR_022", MagazineErrorCode.GET_SCRAPBOX_FAIL),                      // GET_SCRAP_BOX_FAILED
            Map.entry("MAGAZINE_ERR_023", MagazineErrorCode.GET_RECOMMENDED_MAGAZINE_FAIL),          // GET_RECOMMENDED_MAGAZINE_FAILED
            Map.entry("MAGAZINE_ERR_030", MagazineErrorCode.SCRAP_MAGAZINE_FAIL)                    // SCRAP_MAGAZINE_FAILED
    );

    @Override
    public ErrorCode mapToErrorCode(String developCode, String grpcStatusCode) {
        MagazineErrorCode errorCode = ERROR_CODE_MAP.get(developCode);

        if (errorCode == null) {
            log.warn("알 수 없는 User gRPC 에러코드: {}. 기본 퀴즈 에러 코드 사용", developCode);
            return getDefaultErrorCode();
        }

        log.debug("gRPC 에러 코드 변환: {} -> {}", developCode, errorCode.getDevelopCode());
        return errorCode;
    }

    @Override
    public ErrorCode getDefaultErrorCode() {
        return MagazineErrorCode.AVAILABLE_MAGAZINE_SERVER;
    }
}
