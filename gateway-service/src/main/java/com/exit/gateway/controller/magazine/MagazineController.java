package com.exit.gateway.controller.magazine;

import com.exit.common.exception.rest.RestApiException;
import com.exit.common.grpc.GetMagazineRequest;
import com.exit.common.grpc.GetMagazinesByCategoryRequest;
import com.exit.common.grpc.GetScrapBoxRequest;
import com.exit.common.grpc.ScrapMagazineRequest;
import com.exit.common.response.SuccessResponse;
import com.exit.common.response.error.rest.MagazineErrorCode;
import com.exit.common.response.success.MagazineSuccessCode;
import com.exit.gateway.controller.magazine.dto.response.MagazineItemDto;
import com.exit.gateway.controller.magazine.dto.response.MagazineItemListDto;
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

            return SuccessResponse.of(MagazineSuccessCode.GET_MAGAZINE_LIST_SUCCESS,
                    magazineGrpcClient.getMagazinesByCategory(request));
        } catch (StatusRuntimeException e) {
            log.error("Get magazines by category failed via gRPC: {}", e.getStatus(), e);
            String errorMessage = getGrpcErrorMessage(e);
            throw new RestApiException(MagazineErrorCode.GET_MAGAZINE_LIST_FAIL, errorMessage);
        } catch (Exception e) {
            log.error("Get magazines by category failed", e);
            throw new RestApiException(MagazineErrorCode.GET_MAGAZINE_LIST_FAIL);
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
            throw new RestApiException(MagazineErrorCode.NULL_MAGAZINE, errorMessage);
        } catch (Exception e) {
            log.error("Get magazine failed", e);
            throw new RestApiException(MagazineErrorCode.NULL_MAGAZINE);
        }
    }

    @PostMapping("/{magazineId}/scrap")
    public SuccessResponse<MagazineItemDto> scrapMagazine(
            @PathVariable Long magazineId,
            @LoginUser Long userId) {
        try {
            log.info("Scrap magazine request received: magazineId={}", magazineId);
            ScrapMagazineRequest request = ScrapMagazineRequest.newBuilder()
                    .setMagazineId(magazineId)
                    .setUserId(userId)
                    .build();

            return SuccessResponse.of(MagazineSuccessCode.GET_MAGAZINE_SUCCESS,
                    magazineGrpcClient.scrapMagazine(request));
        } catch (StatusRuntimeException e) {
            log.error("Scrap magazine failed via gRPC: {}", e.getStatus(), e);
            String errorMessage = getGrpcErrorMessage(e);
            throw new RestApiException(MagazineErrorCode.NULL_MAGAZINE, errorMessage);
        } catch (Exception e) {
            log.error("Scrap magazine failed", e);
            throw new RestApiException(MagazineErrorCode.NULL_MAGAZINE);
        }
    }

    @GetMapping("/scrap-box")
    public SuccessResponse<MagazineItemDto> getScrapBox(
            @PathVariable Long magazineId) {
        try {
            log.info("Get magazine request received: magazineId={}", magazineId);
            GetScrapBoxRequest request = GetScrapBoxRequest.newBuilder()
                    .setMagazineId(magazineId)
                    .build();

            return SuccessResponse.of(MagazineSuccessCode.GET_MAGAZINE_SUCCESS,
                    magazineGrpcClient.getScrapBox(request));
        } catch (StatusRuntimeException e) {
            log.error("Get magazine failed via gRPC: {}", e.getStatus(), e);
            String errorMessage = getGrpcErrorMessage(e);
            throw new RestApiException(MagazineErrorCode.NULL_MAGAZINE, errorMessage);
        } catch (Exception e) {
            log.error("Get magazine failed", e);
            throw new RestApiException(MagazineErrorCode.NULL_MAGAZINE);
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
