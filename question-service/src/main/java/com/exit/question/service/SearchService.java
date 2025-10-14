package com.exit.question.service;

import com.exit.common.grpc.*;
import com.exit.common.util.time.TimeStampUtil;
import com.exit.question.controller.dto.response.QuestionListQueryResponseDto;
import com.exit.question.domain.question.repository.QuestionRepository;
import com.exit.question.service.client.UserGrpcClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class SearchService {

    private final QuestionRepository questionRepository;
    private final UserGrpcClient userGrpcClient;

    public SearchQuestionsResponse searchQuestions(SearchQuestionsRequest request) {
        log.info("Searching questions with keyword: {}, page: {}, size: {}",
                request.getKeyword(), request.getPage(), request.getSize());

        PageRequest pageRequest = PageRequest.of(request.getPage(), request.getSize());

        String searchKeyword = request.getKeyword().trim().isEmpty() ? null : request.getKeyword();

        Slice<QuestionListQueryResponseDto> slice = questionRepository.findQuestionsByFilter(
                new ArrayList<>(List.of(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L)), // 모든 카테고리
                searchKeyword,
                false,
                pageRequest
        );

        // 작성자 정보 조회
        Set<Long> writerIds = slice.getContent().stream()
                .map(QuestionListQueryResponseDto::questionWriterId)
                .collect(Collectors.toSet());

        Map<Long, UpdateAdditionalUserInfoResponse> userInfoMap = getUserInfoMap(writerIds);

        // QuestionSearchItem 생성
        List<QuestionSearchItem> searchItems = slice.getContent().stream()
                .map(question -> {
                    UpdateAdditionalUserInfoResponse userInfo = userInfoMap.get(question.questionWriterId());
                    return QuestionSearchItem.newBuilder()
                            .setQuestionId(question.questionId())
                            .setQuestionCategory(question.questionCategoryId())
                            .setQuestionWriterId(question.questionWriterId())
                            .setQuestionWriterName(userInfo != null ? userInfo.getUserName() : "Unknown")
                            .setQuestionTitle(question.questionTitle())
                            .setQuestionContent(question.questionContent())
                            .setQuestionUrgency(question.questionUrgency())
                            .setQuestionAnswerType(question.questionAnswerType().name())
                            .setQuestionAnswerAdopt(question.questionAnswerAdopt())
                            .setAnswerCount(question.answerCount().intValue())
                            .setCreatedAt(TimeStampUtil.toGrpcTimestamp(question.createdAt()))
                            .build();
                })
                .collect(Collectors.toList());

        return SearchQuestionsResponse.newBuilder()
                .addAllQuestions(searchItems)
                .setTotalCount(searchItems.size())
                .setHasNext(slice.hasNext())
                .build();
    }

    private Map<Long, UpdateAdditionalUserInfoResponse> getUserInfoMap(Set<Long> writerIds) {
        if (writerIds.isEmpty()) {
            return Map.of();
        }

        List<UpdateAdditionalUserInfoResponse> userInfoList =
                userGrpcClient.getUsersNameAndProfile(new ArrayList<>(writerIds)).getUserInfoList();

        return userInfoList.stream()
                .collect(Collectors.toMap(
                        UpdateAdditionalUserInfoResponse::getUserId,
                        Function.identity()
                ));
    }
}
