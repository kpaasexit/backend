package com.exit.question.service.util;

import com.exit.common.grpc.AnswerAdoptResponse;
import com.exit.common.grpc.AnswerCreateResponse;
import com.exit.common.grpc.AnswerRecommendResponse;
import com.exit.common.grpc.AnswerReportResponse;
import com.exit.question.domain.response.Response;
import com.exit.question.domain.response.ResponseReport;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.exit.common.util.time.TimeStampUtil.toGrpcTimestamp;

@Component
public class ResponseGrpcMapper {

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

    public AnswerAdoptResponse getAnswerAdoptResponse(Response response) {
        return AnswerAdoptResponse.newBuilder()
                .setResponseId(response.getResponseId())
                .setResponseAdopt(response.getResponseAdopt())
                .setUpdatedAt(toGrpcTimestamp(response.getUpdatedAt()))
                .build();
    }

    public AnswerCreateResponse getAnswerCreateResponse(Response response, List<String> urls) {

        AnswerCreateResponse.Builder builder = AnswerCreateResponse.newBuilder();

        if (urls != null && !urls.isEmpty()) {
            builder.addAllImageUrls(urls);
        }

        return builder
                .setResponseId(response.getResponseId())
                .setQuestionId(response.getQuestionId())
                .setResponseContent(response.getResponseContent())
                .setResponseWriterId(response.getResponseWriterId())
                .setCreatedAt(toGrpcTimestamp(response.getCreatedAt()))
                .build();
    }

    public AnswerRecommendResponse getAnswerRecommendResponse(Long responseId, int likeCount, boolean isLiked) {
        return AnswerRecommendResponse.newBuilder()
                .setResponseId(responseId)
                .setCount(likeCount)
                .setIsRecommended(isLiked)
                .build();
    }
}
