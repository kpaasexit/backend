package com.exit.question.service.util;

import com.exit.common.grpc.*;
import com.exit.question.controller.dto.response.CommentAndAdditionalQuestionNum;
import com.exit.question.domain.response.Response;
import com.exit.question.domain.response.ResponseReport;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.exit.common.util.time.TimeStampUtil.toGrpcTimestamp;

@Component
public class ResponseGrpcMapper {

    public AnswerReportResponse getAnswerReportResponse(ResponseReport report) {
        return AnswerReportResponse.newBuilder()
                .setResponseReportId(report.getResponseReportId())
                .setResponseId(report.getResponseId())
                .setResponseReportReason(report.getResponseReportReason())
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

    public AnswerCreateResponse getAnswerCreateResponse(Response response, List<ImageObject> imageObjects) {

        AnswerCreateResponse.Builder builder = AnswerCreateResponse.newBuilder();
        if (imageObjects != null && !imageObjects.isEmpty()) {
            builder.addAllImage(imageObjects);
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

    public ResponseDetail getResponseDetail(Response response, List<ImageObject> imageObjects, Integer likeCount,
                                            UpdateAdditionalUserInfoResponse writerNameProfile, Authority responseAuthority,
                                            CommentAndAdditionalQuestionNum commentAndAdditionalQuestionNum,
                                            Long followUpRoomId) {
        ResponseDetail.Builder builder = ResponseDetail.newBuilder();

        if (imageObjects != null && !imageObjects.isEmpty()) {
            builder.addAllImage(imageObjects);
        }

        if(!writerNameProfile.getUserProfile().isEmpty()){
            builder.setProfile(writerNameProfile.getUserProfile());
        }

        return builder
                .setResponseId(response.getResponseId())
                .setResponseWriterId(response.getResponseWriterId())
                .setResponseWriterName(writerNameProfile.getUserName())
                .setResponseContent(response.getResponseContent())
                .setResponseAdopt(response.getResponseAdopt())
                .setLikeCount(likeCount)
                .setCreatedAt(toGrpcTimestamp(response.getCreatedAt()))
                .setUpdatedAt(toGrpcTimestamp(response.getUpdatedAt()))
                .setAuthority(responseAuthority)
                .setIsAi(Objects.equals(response.getResponseWriterId(), 1L))
                .setCommentNum(commentAndAdditionalQuestionNum.commentNum())
                .setAdditionalQuestionNum(commentAndAdditionalQuestionNum.additionalQuestionNum())
                .setFollowUpRoomId(followUpRoomId != null ? followUpRoomId : 0L)
                .build();
    }
}
