package com.exit.gateway.service.search;

import com.exit.common.grpc.QuestionListRequest;
import com.exit.common.grpc.SearchMagazinesResponse;
import com.exit.gateway.controller.magazine.dto.response.MagazineListDto;
import com.exit.gateway.controller.magazine.dto.response.MagazineListItemDto;
import com.exit.gateway.controller.question.dto.response.question.QuestionListQueryResponseDto;
import com.exit.gateway.controller.question.dto.response.question.QuestionListResponseDto;
import com.exit.gateway.controller.search.dto.response.IntegratedSearchResponseDto;
import com.exit.gateway.global.annotation.GrpcToRest;
import com.exit.gateway.global.util.mapper.error.search.SearchGrpcErrorMapper;
import com.exit.gateway.service.magazine.MagazineGrpcClient;
import com.exit.gateway.service.question.QuestionGrpcClient;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
@GrpcToRest(mapper = SearchGrpcErrorMapper.class)
public class SearchService {
    private final QuestionGrpcClient questionGrpcClient;
    private final MagazineGrpcClient magazineGrpcClient;

    public IntegratedSearchResponseDto integratedSearch(String keyword, int page, int size) {
        log.info("Integrated search request - keyword: {}, page: {}, size: {}", keyword, page, size);
        QuestionListRequest questionListRequest = QuestionListRequest.newBuilder()
                .addAllCategoryIds(new ArrayList<>(List.of(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L)))
                .setKeyword(keyword)
                .setPageNum(page)
                .setSize(size)
                .setIsExist(false)
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

        List<QuestionListQueryResponseDto> questions = questionsResponse.questionListItems();
        List<MagazineListItemDto> magazines = magazinesResponse.getMagazinesList().stream()
                .map(MagazineListItemDto::from)
                .toList();
        boolean questionHasNext = questionsResponse.hasNext();
        boolean magazineHasNext = magazinesResponse.getHasNext();

        return IntegratedSearchResponseDto.builder()
                .questions(questions)
                .magazines(magazines)
                .questionHasNext(questionHasNext)
                .magazineHasNext(magazineHasNext)
                .build();
    }

    public MagazineListDto searchMagazine(String keyword, int pageNum, int size) {
        SearchMagazinesResponse response = magazineGrpcClient.searchMagazines(keyword, pageNum, size);

        return MagazineListDto.from(response);
    }

    public QuestionListResponseDto searchQuestion(String keyword, int pageNum, int size) {
        QuestionListRequest questionListRequest = QuestionListRequest.newBuilder()
                .addAllCategoryIds(new ArrayList<>(List.of(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L)))
                .setKeyword(keyword)
                .setPageNum(pageNum)
                .setSize(size)
                .setIsExist(false)
                .build();
        return questionGrpcClient.getQuestionList(questionListRequest);
    }
}
