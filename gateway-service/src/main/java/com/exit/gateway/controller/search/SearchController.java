package com.exit.gateway.controller.search;

import com.exit.common.exception.rest.RestApiException;
import com.exit.common.grpc.QuestionListRequest;
import com.exit.common.grpc.SearchMagazinesResponse;
import com.exit.common.response.SuccessResponse;
import com.exit.common.response.error.rest.search.SearchErrorCode;
import com.exit.common.response.success.SearchSuccessCode;
import com.exit.gateway.controller.question.dto.response.question.QuestionListQueryResponseDto;
import com.exit.gateway.controller.question.dto.response.question.QuestionListResponseDto;
import com.exit.gateway.controller.search.dto.response.IntegratedSearchResponseDto;
import com.exit.gateway.controller.search.dto.response.MagazineSearchItemDto;
import com.exit.gateway.controller.search.dto.response.RecommendedSearchTermsDto;
import com.exit.gateway.service.magazine.MagazineGrpcClient;
import com.exit.gateway.service.question.QuestionGrpcClient;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
@Slf4j
public class SearchController {

    private final QuestionGrpcClient questionGrpcClient;
    private final MagazineGrpcClient magazineGrpcClient;

    @GetMapping
    public SuccessResponse<IntegratedSearchResponseDto> integratedSearch(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        try {
            log.info("Integrated search request - keyword: {}, page: {}, size: {}", keyword, page, size);
            QuestionListRequest questionListRequest = QuestionListRequest.newBuilder()
                    .addAllCategoryIds(new ArrayList<>(List.of(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L)))
                    .setKeyword(keyword)
                    .setPage(page)
                    .setSize(size)
                    .setIsAdopted(false)
                    .build();
            // 병렬로 질문과 매거진 검색
            CompletableFuture<QuestionListResponseDto> questionsFuture = CompletableFuture.supplyAsync(() ->
                    questionGrpcClient.getQuestionList(questionListRequest)
            );
            CompletableFuture<SearchMagazinesResponse> magazinesFuture = CompletableFuture.supplyAsync(() ->
                    magazineGrpcClient.searchMagazines(keyword, page, size)
            );

            // 두 결과 모두 기다림
            QuestionListResponseDto questionsResponse = questionsFuture.join();
            SearchMagazinesResponse magazinesResponse = magazinesFuture.join();

            List<QuestionListQueryResponseDto> questions = questionsResponse.questionList();
            List<MagazineSearchItemDto> magazines = magazinesResponse.getMagazinesList().stream()
                    .map(MagazineSearchItemDto::toMagazineDto)
                    .toList();
            boolean questionHasNext = questionsResponse.hasNext();
            boolean magazineHasNext = magazinesResponse.getHasNext();


            IntegratedSearchResponseDto responseDto = IntegratedSearchResponseDto.builder()
                    .questions(questions)
                    .magazines(magazines)
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
    public SuccessResponse<RecommendedSearchTermsDto> integratedSearch() {
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
