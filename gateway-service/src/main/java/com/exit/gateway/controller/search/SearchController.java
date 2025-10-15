package com.exit.gateway.controller.search;

import com.exit.common.exception.rest.RestApiException;
import com.exit.common.grpc.SearchMagazinesResponse;
import com.exit.common.grpc.SearchQuestionsResponse;
import com.exit.common.response.SuccessResponse;
import com.exit.common.response.error.rest.SearchErrorCode;
import com.exit.common.response.success.SearchSuccessCode;
import com.exit.gateway.controller.search.dto.response.IntegratedSearchResponseDto;
import com.exit.gateway.controller.search.dto.response.MagazineSearchItemDto;
import com.exit.gateway.controller.search.dto.response.QuestionSearchItemDto;
import com.exit.gateway.controller.search.dto.response.RecommendedSearchTermsDto;
import com.exit.gateway.service.search.SearchGrpcClient;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
@Slf4j
public class SearchController {

    private final SearchGrpcClient searchGrpcClient;

    @GetMapping
    public SuccessResponse<IntegratedSearchResponseDto> integratedSearch(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        try {
            log.info("Integrated search request - keyword: {}, page: {}, size: {}", keyword, page, size);

            List<QuestionSearchItemDto> questions = Collections.emptyList();
            List<MagazineSearchItemDto> magazines = Collections.emptyList();
            int questionTotalCount = 0;
            int magazineTotalCount = 0;
            boolean questionHasNext = false;
            boolean magazineHasNext = false;

            // 병렬로 질문과 매거진 검색
            CompletableFuture<SearchQuestionsResponse> questionsFuture = CompletableFuture.supplyAsync(() ->
                    searchGrpcClient.searchQuestions(keyword, page, size)
            );
            CompletableFuture<SearchMagazinesResponse> magazinesFuture = CompletableFuture.supplyAsync(() ->
                    searchGrpcClient.searchMagazines(keyword, page, size)
            );

            // 두 결과 모두 기다림
            SearchQuestionsResponse questionsResponse = questionsFuture.join();
            SearchMagazinesResponse magazinesResponse = magazinesFuture.join();

            questions = questionsResponse.getQuestionsList().stream()
                    .map(SearchGrpcClient::toQuestionDto)
                    .toList();
            magazines = magazinesResponse.getMagazinesList().stream()
                    .map(SearchGrpcClient::toMagazineDto)
                    .toList();
            questionTotalCount = questionsResponse.getTotalCount();
            magazineTotalCount = magazinesResponse.getTotalCount();
            questionHasNext = questionsResponse.getHasNext();
            magazineHasNext = magazinesResponse.getHasNext();


            IntegratedSearchResponseDto responseDto = IntegratedSearchResponseDto.builder()
                    .questions(questions)
                    .magazines(magazines)
                    .questionTotalCount(questionTotalCount)
                    .magazineTotalCount(magazineTotalCount)
                    .questionHasNext(questionHasNext)
                    .magazineHasNext(magazineHasNext)
                    .build();

            return SuccessResponse.of(SearchSuccessCode.SEARCH_SUCCESS, responseDto);

        } catch (StatusRuntimeException e) {
            log.error("Search failed via gRPC: {}", e.getStatus(), e);
            throw new RestApiException(SearchErrorCode.SEARCH_FAIL, getGrpcErrorMessage(e));
        } catch (Exception e) {
            log.error("Search failed", e);
            throw new RestApiException(SearchErrorCode.SEARCH_FAIL);
        }
    }

    @GetMapping("/recommend")
    public SuccessResponse<RecommendedSearchTermsDto> integratedSearch(){
        List<String> terms = List.of("감자", "고구마", "강아지", "고양이");
        return SuccessResponse.of(SearchSuccessCode.SEARCH_SUCCESS, new RecommendedSearchTermsDto(terms));
    }

    private String getGrpcErrorMessage(StatusRuntimeException e) {
        Status status = e.getStatus();
        return switch (status.getCode()) {
            case INVALID_ARGUMENT -> "잘못된 검색 요청입니다.";
            case NOT_FOUND -> "검색 결과를 찾을 수 없습니다.";
            case UNAVAILABLE -> "검색 서비스를 일시적으로 사용할 수 없습니다.";
            default -> status.getDescription() != null ? status.getDescription() : "검색 중 오류가 발생했습니다.";
        };
    }
}
