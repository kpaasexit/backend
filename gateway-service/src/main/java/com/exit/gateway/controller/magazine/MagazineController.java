package com.exit.gateway.controller.magazine;

import com.exit.common.exception.rest.RestApiException;
import com.exit.common.grpc.*;
import com.exit.common.response.SuccessResponse;
import com.exit.common.response.error.rest.magazine.MagazineErrorCode;
import com.exit.common.response.success.MagazineSuccessCode;
import com.exit.gateway.controller.magazine.dto.response.*;
import com.exit.gateway.global.annotation.LoginUser;
import com.exit.gateway.service.magazine.MagazineGrpcClient;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/magazine")
@RequiredArgsConstructor
@Slf4j
public class MagazineController {
    private final MagazineGrpcClient magazineGrpcClient;

    @GetMapping("/category/{categoryId}")
    public SuccessResponse<MagazineItemListDto> getMagazinesByCategory(
            @PathVariable Long categoryId,
            @RequestParam(defaultValue = "1") Integer pageNum) {
        try {
            log.info("Get magazines by category request received: categoryId={}, pageNum={}", categoryId, pageNum - 1);
            GetMagazinesByCategoryRequest request = GetMagazinesByCategoryRequest.newBuilder()
                    .setCategoryId(categoryId)
                    .setPageNum(pageNum - 1)
                    .build();

            return SuccessResponse.of(MagazineSuccessCode.GET_MAGAZINE_LIST_BY_CATEGORY_SUCCESS,
                    magazineGrpcClient.getMagazinesByCategory(request));
        } catch (StatusRuntimeException e) {
            log.error("Get magazines by category failed via gRPC: {}", e.getStatus(), e);
            String errorMessage = getGrpcErrorMessage(e);
            throw new RestApiException(MagazineErrorCode.GET_MAGAZINES_BY_CATEGORY_FAIL, errorMessage);
        } catch (Exception e) {
            log.error("Get magazines by category failed", e);
            throw new RestApiException(MagazineErrorCode.GET_MAGAZINES_BY_CATEGORY_FAIL);
        }
    }

    @GetMapping("/{magazineId}")
    public SuccessResponse<MagazineItemDto> getMagazine(
            @PathVariable Long magazineId) {
        try {
            log.info("Get magazine request received: magazineId={}", magazineId);
            GetMagazineRequest request = GetMagazineRequest.newBuilder()
                    .setMagazineId(magazineId)
                    .build();

            return SuccessResponse.of(MagazineSuccessCode.GET_MAGAZINE_SUCCESS,
                    magazineGrpcClient.getMagazine(request));
        } catch (StatusRuntimeException e) {
            log.error("Get magazine failed via gRPC: {}", e.getStatus(), e);
            String errorMessage = getGrpcErrorMessage(e);
            throw new RestApiException(MagazineErrorCode.NOT_FOUND_MAGAZINE, errorMessage);
        } catch (Exception e) {
            log.error("Get magazine failed", e);
            throw new RestApiException(MagazineErrorCode.NO);
        }
    }

    @PostMapping("/{magazineId}/scrap")
    public SuccessResponse<ScrapMagazineResponseDto> scrapMagazine(
            @PathVariable Long magazineId,
            @LoginUser Long userId) {
        try {
            log.info("Scrap magazine request received: magazineId={}", magazineId);
            ScrapMagazineRequest request = ScrapMagazineRequest.newBuilder()
                    .setMagazineId(magazineId)
                    .setUserId(userId)
                    .build();

            return SuccessResponse.of(MagazineSuccessCode.SCRAP_MAGAZINE_SUCCESS,
                    magazineGrpcClient.scrapMagazine(request));
        } catch (StatusRuntimeException e) {
            log.error("Scrap magazine failed via gRPC: {}", e.getStatus(), e);
            String errorMessage = getGrpcErrorMessage(e);
            throw new RestApiException(MagazineErrorCode.SCRAP_MAGAZINE_FAIL, errorMessage);
        } catch (Exception e) {
            log.error("Scrap magazine failed", e);
            throw new RestApiException(MagazineErrorCode.SCRAP_MAGAZINE_FAIL);
        }
    }

    @GetMapping("/scrap-box")
    public SuccessResponse<GetScrapBoxResponseDto> getScrapBox(
            @LoginUser Long userId,
            @RequestParam Integer pageNum
    ) {
        try {
            log.info("Get scrap-box request received");
            GetScrapBoxRequest request = GetScrapBoxRequest.newBuilder()
                    .setUserId(userId)
                    .setPageNum(pageNum-1)
                    .build();

            return SuccessResponse.of(MagazineSuccessCode.GET_SCRAP_BOX_SUCCESS,
                    magazineGrpcClient.getScrapBox(request));
        } catch (StatusRuntimeException e) {
            log.error("Get magazine failed via gRPC: {}", e.getStatus(), e);
            String errorMessage = getGrpcErrorMessage(e);
            throw new RestApiException(MagazineErrorCode.GET_SCRAPBOX_FAIL, errorMessage);
        } catch (Exception e) {
            log.error("Get magazine failed", e);
            throw new RestApiException(MagazineErrorCode.GET_SCRAPBOX_FAIL);
        }
    }

    @GetMapping("/recommend")
    public SuccessResponse<GetRecommendedMagazineResponseDto> getScrapBox(
            @LoginUser Long userId
    ) {
        try {
            log.info("Get scrap-box request received");
            GetRecommendedMagazineRequest request = GetRecommendedMagazineRequest.newBuilder()
                    .setUserId(userId)
                    .build();

            return SuccessResponse.of(MagazineSuccessCode.GET_RECOMMENDED_MAGAZINE_SUCCESS,
                    magazineGrpcClient.getRecommendedMagazine(request));
        } catch (StatusRuntimeException e) {
            log.error("Get magazine failed via gRPC: {}", e.getStatus(), e);
            String errorMessage = getGrpcErrorMessage(e);
            throw new RestApiException(MagazineErrorCode.GET_RECOMMENDED_MAGAZINE_FAIL, errorMessage);
        } catch (Exception e) {
            log.error("Get magazine failed", e);
            throw new RestApiException(MagazineErrorCode.GET_RECOMMENDED_MAGAZINE_FAIL);
        }
    }

    private String getGrpcErrorMessage(StatusRuntimeException e) {
        Status status = e.getStatus();
        switch (status.getCode()) {
            case INVALID_ARGUMENT:
                return "잘못된 요청입니다.";
            case NOT_FOUND:
                return "매거진을 찾을 수 없습니다.";
            case INTERNAL:
                return "서버 내부 오류가 발생했습니다.";
            default:
                return status.getDescription() != null ? status.getDescription() : "서버 오류가 발생했습니다.";
        }
    }
}
