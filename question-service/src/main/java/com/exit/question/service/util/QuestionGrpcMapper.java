package com.exit.question.service.util;

import com.exit.common.grpc.*;
import com.exit.common.util.time.TimeStampUtil;
import com.exit.question.controller.dto.response.AnswerReportResponseDto;
import com.exit.question.controller.dto.response.QuestionListQueryResponseDto;
import com.exit.question.controller.dto.response.QuestionReportResponseDto;
import com.exit.question.controller.dto.response.ResponseDetailDto;
import com.exit.question.domain.question.Question;
import com.exit.question.domain.question.QuestionReport;
import com.exit.question.domain.response.Response;
import com.exit.question.domain.response.ResponseReport;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.exit.common.util.time.TimeStampUtil.toGrpcTimestamp;

@Component
public class QuestionGrpcMapper {
    public SendNotificationRequest getSendNotificationRequest(String deviceId, String body, Question question) {
        return SendNotificationRequest.newBuilder()
                .setBody(body)
                .setType("ANSWER_ADOPTED")
                .setTargetId(question.getQuestionId())
                .setReceiverId(question.getQuestionWriterId())
                .setDeviceId(deviceId)
                .build();
    }

    public AnswerAdoptResponse getAnswerAdoptResponse(Response response) {
        return AnswerAdoptResponse.newBuilder()
                .setResponseId(response.getResponseId())
                .setResponseAdopt(response.getResponseAdopt())
                .setUpdatedAt(toGrpcTimestamp(response.getUpdatedAt()))
                .build();
    }

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

    public AnswerReportResponse getAnswerReportResponse(ResponseReport report) {
        return AnswerReportResponse.newBuilder()
                .setResponseReportId(report.getResponseReportId())
                .setResponseId(report.getResponseId())
                .setResponseReportTitle(report.getResponseReportTitle())
                .setResponseReportContent(report.getResponseReportContent())
                .setResponseReportWriterId(report.getResponseReportWriterId())
                .setCreatedAt(toGrpcTimestamp(report.getCreatedAt()))
                .setUpdatedAt(toGrpcTimestamp(report.getUpdatedAt()))
                .build();
    }

    public QuestionListResponse getQuestionListResponse(List<QuestionListQueryResponseDto> content, boolean hasNext) {
        List<QuestionListItem> allQuestions = content.stream().map(QuestionListQueryResponseDto::toQuestionListItem).toList();
        return QuestionListResponse.newBuilder()
                .addAllQuestions(allQuestions)
                .setHasNext(hasNext)
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

    public ResponseDetail getResponseDetail(Response response, List<String> urls, Integer likeCount, String writerName) {
        ResponseDetail.Builder builder = ResponseDetail.newBuilder();

        if (urls != null && !urls.isEmpty()) {
            builder.addAllUrls(urls);
        }

        return builder
                .setResponseId(response.getResponseId())
                .setResponseWriterId(response.getResponseWriterId())
                .setResponseWriterName(writerName)
                .setResponseContent(response.getResponseContent())
                .setResponseAdopt(response.getResponseAdopt())
                .setLikeCount(likeCount)
                .setCreatedAt(toGrpcTimestamp(response.getCreatedAt()))
                .setUpdatedAt(toGrpcTimestamp(response.getUpdatedAt()))
                .build();
    }

    public QuestionDetailResponse getQuestionDetailResponse(QuestionCreateResponse questionCreateResponse, List<ResponseDetail> responseDetails, boolean hasMore) {
        return QuestionDetailResponse.newBuilder()
                .setQuestion(questionCreateResponse)
                .addAllResponses(responseDetails)
                .setHasNext(hasMore)
                .build();
    }
}
