package com.exit.question.service.util;

import com.exit.common.grpc.*;
import com.exit.common.util.time.TimeStampUtil;
import com.exit.question.controller.dto.response.QuestionListQueryResponseDto;
import com.exit.question.domain.question.Question;
import com.exit.question.domain.question.QuestionReport;
import com.exit.question.domain.response.Response;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

import static com.exit.common.util.time.TimeStampUtil.toGrpcTimestamp;

@Component
public class QuestionGrpcMapper {

    public QuestionReportResponse getQuestionReportResponse(QuestionReport report) {
        return QuestionReportResponse.newBuilder()
                .setQuestionReportId(report.getQuestionReportId())
                .setQuestionId(report.getQuestionId())
                .setQuestionReportTitle(report.getQuestionReportTitle())
                .setQuestionReportContent(report.getQuestionReportContent())
                .setQuestionReportWriterId(report.getQuestionReportWriterId())
                .setCreatedAt(toGrpcTimestamp(report.getCreatedAt()))
                .setUpdatedAt(toGrpcTimestamp(report.getUpdatedAt()))
                .build();
    }

    public QuestionListResponse getQuestionListResponse(Page<QuestionListQueryResponseDto> page, Map<Long, UpdateAdditionalUserInfoResponse> userInfoMap) {
        List<QuestionListItem> questionListItems = page.getContent().stream()
                .map(dto -> QuestionListQueryResponseDto.toQuestionListItem(dto, userInfoMap))
                .toList();

        return QuestionListResponse.newBuilder()
                .addAllQuestions(questionListItems)
                .setCurrentPage(page.getNumber() + 1)
                .setHasNext(page.hasNext())
                .setTotalPageNum(page.getTotalPages())
                .build();
    }

    public SimilarQuestionResponse getSimilarQuestionResponse(List<Question> questions) {
        List<SimilarQuestionItem> items = questions.stream().map(this::createSimilarQuestion).toList();
        return SimilarQuestionResponse.newBuilder()
                .addAllSimilarQuestions(items)
                .build();
    }

    private SimilarQuestionItem createSimilarQuestion(Question question) {
        return SimilarQuestionItem.newBuilder()
                .setQuestionId(question.getQuestionId())
                .setQuestionTitle(question.getQuestionTitle())
                .setQuestionContent(question.getQuestionContent())
                .setQuestionCategory(question.getQuestionCategory().getQuestionCategoryId())
                .setQuestionUrgency(question.getQuestionUrgency())
                .setQuestionAnswerType(question.getQuestionAnswerType().name())
                .setQuestionAnswerAdopt(question.getQuestionAnswerAdopt())
                .setCreatedAt(TimeStampUtil.toGrpcTimestamp(question.getCreatedAt()))
                .build();
    }

    public QuestionCreateResponse getQuestionCreateResponse(Question question, List<String> urls, String questionWriterName) {
        QuestionCreateResponse.Builder builder = QuestionCreateResponse.newBuilder();

        if (urls != null && !urls.isEmpty()) {
            builder.addAllImageUrls(urls);
        }

        return builder
                .setQuestionId(question.getQuestionId())
                .setQuestionWriterId(question.getQuestionWriterId())
                .setQuestionWriterName(questionWriterName)
                .setQuestionTitle(question.getQuestionTitle())
                .setQuestionContent(question.getQuestionContent())
                .setQuestionCategory(question.getQuestionCategory().getQuestionCategoryId())
                .setQuestionUrgency(question.getQuestionUrgency())
                .setQuestionAnswerType(question.getQuestionAnswerType().name())
                .setQuestionDisclosureType(question.getQuestionDisclosure().name())
                .setCreatedAt(toGrpcTimestamp(question.getCreatedAt()))
                .build();
    }

    public QuestionDetailResponse getQuestionDetailResponse(QuestionCreateResponse questionCreateResponse, Authority authority) {
        return QuestionDetailResponse.newBuilder()
                .setQuestion(questionCreateResponse)
                .setAuthority(authority)
                .build();
    }
}
