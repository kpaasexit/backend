package com.exit.gateway.service.search;

import com.exit.common.grpc.*;
import com.exit.gateway.controller.search.dto.response.MagazineSearchItemDto;
import com.exit.gateway.controller.search.dto.response.QuestionSearchItemDto;
import com.google.protobuf.Timestamp;
import io.grpc.StatusRuntimeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchGrpcClient {

    @GrpcClient("question-service")
    private SearchServiceGrpc.SearchServiceBlockingStub questionSearchStub;

    @GrpcClient("magazine-service")
    private SearchServiceGrpc.SearchServiceBlockingStub magazineSearchStub;

    public SearchQuestionsResponse searchQuestions(String keyword, int page, int size) {
        try {
            // 빈 문자열이나 공백만 있는 경우 빈 문자열로 정규화
            String normalizedKeyword = (keyword == null || keyword.trim().isEmpty()) ? "" : keyword.trim();
            log.debug("Sending search questions request via gRPC for keyword: '{}'", normalizedKeyword);

            SearchQuestionsRequest request = SearchQuestionsRequest.newBuilder()
                    .setKeyword(normalizedKeyword)
                    .setPage(page)
                    .setSize(size)
                    .build();

            SearchQuestionsResponse response = questionSearchStub.searchQuestions(request);
            log.debug("Received search questions response via gRPC with {} results", response.getQuestionsCount());
            return response;
        } catch (StatusRuntimeException e) {
            log.error("gRPC search questions failed: {}", e.getStatus(), e);
            throw e;
        }
    }

    public SearchMagazinesResponse searchMagazines(String keyword, int page, int size) {
        try {
            // 빈 문자열이나 공백만 있는 경우 빈 문자열로 정규화
            String normalizedKeyword = (keyword == null || keyword.trim().isEmpty()) ? "" : keyword.trim();
            log.debug("Sending search magazines request via gRPC for keyword: '{}'", normalizedKeyword);

            SearchMagazinesRequest request = SearchMagazinesRequest.newBuilder()
                    .setKeyword(normalizedKeyword)
                    .setPage(page)
                    .setSize(size)
                    .build();

            SearchMagazinesResponse response = magazineSearchStub.searchMagazines(request);
            log.debug("Received search magazines response via gRPC with {} results", response.getMagazinesCount());
            return response;
        } catch (StatusRuntimeException e) {
            log.error("gRPC search magazines failed: {}", e.getStatus(), e);
            throw e;
        }
    }

    // Helper method to convert gRPC QuestionSearchItem to DTO
    public static QuestionSearchItemDto toQuestionDto(QuestionSearchItem item) {
        return QuestionSearchItemDto.builder()
                .questionId(item.getQuestionId())
                .questionCategory(item.getQuestionCategory())
                .questionWriterId(item.getQuestionWriterId())
                .questionWriterName(item.getQuestionWriterName())
                .questionTitle(item.getQuestionTitle())
                .questionContent(item.getQuestionContent())
                .questionUrgency(item.getQuestionUrgency())
                .questionAnswerType(item.getQuestionAnswerType())
                .questionAnswerAdopt(item.getQuestionAnswerAdopt())
                .answerCount(item.getAnswerCount())
                .createdAt(toLocalDateTime(item.getCreatedAt()))
                .build();
    }

    // Helper method to convert gRPC MagazineSearchItem to DTO
    public static MagazineSearchItemDto toMagazineDto(MagazineSearchItem item) {
        return MagazineSearchItemDto.builder()
                .magazineId(item.getMagazineId())
                .magazineCategoryId(item.getMagazineCategoryId())
                .magazineTitle(item.getMagazineTitle())
                .magazineSubtitle(item.getMagazineSubtitle())
                .magazineContent(item.getMagazineContent())
                .magazineAuthor(item.getMagazineAuthor())
                .authorProfileUrl(item.getAuthorProfileUrl())
                .magazineThumbnailUrl(item.getMagazineThumbnailUrl())
                .createdAt(toLocalDateTime(item.getCreatedAt()))
                .build();
    }

    private static LocalDateTime toLocalDateTime(Timestamp timestamp) {
        if (timestamp == null) {
            return null;
        }
        Instant instant = Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos());
        return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }
}
