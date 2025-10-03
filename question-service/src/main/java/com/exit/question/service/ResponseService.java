package com.exit.question.service;

import com.exit.common.exception.grpc.GrpcException;
import com.exit.common.grpc.*;
import com.exit.common.util.file.FileUploadUtil;
import com.exit.question.domain.question.Question;
import com.exit.question.domain.question.repository.FollowUpRoomRepository;
import com.exit.question.domain.question.repository.QuestionRepository;
import com.exit.question.domain.response.Response;
import com.exit.question.domain.response.ResponseImage;
import com.exit.question.domain.response.ResponseLike;
import com.exit.question.domain.response.ResponseReport;
import com.exit.question.domain.response.repository.ResponseImageRepository;
import com.exit.question.domain.response.repository.ResponseLikeRepository;
import com.exit.question.domain.response.repository.ResponseReportRepository;
import com.exit.question.domain.response.repository.ResponseRepository;
import com.exit.question.exception.GrpcQuestionErrorCode;
import com.exit.question.exception.GrpcResponseErrorCode;
import com.exit.question.service.client.NotificationGrpcClient;
import com.exit.question.service.client.UserGrpcClient;
import com.exit.question.service.util.NotificationGrpcMapper;
import com.exit.question.service.util.ResponseGrpcMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ResponseService {
    private static final String RESPONSE_FOLDER = "response";

    private final QuestionRepository questionRepository;
    private final ResponseRepository responseRepository;
    private final ResponseImageRepository responseImageRepository;
    private final ResponseLikeRepository responseLikeRepository;
    private final ResponseReportRepository responseReportRepository;
    private final FollowUpRoomRepository followUpRoomRepository;
    private final FileUploadUtil fileUploadUtil;
    private final UserGrpcClient userGrpcClient;
    private final NotificationGrpcClient notificationGrpcClient;
    private final ResponseGrpcMapper responseGrpcMapper;
    private final NotificationGrpcMapper notificationGrpcMapper;


    public AnswerCreateResponse answerCreate(AnswerCreateRequest request) {
        Response newResponse = Response.createResponse(request);
        Response savedResponse = responseRepository.save(newResponse);

        List<String> imageUrls = processAnswerImages(request, savedResponse);
        Question question = questionRepository.findById(savedResponse.getQuestionId())
                .orElseThrow(() -> new GrpcException(GrpcQuestionErrorCode.NULL_QUESTION));

        String subBody = truncateContent(savedResponse.getResponseContent());
        SendNotificationRequest sendNotificationRequest = notificationGrpcMapper.getSendNotificationRequest(
                subBody, "NEW_ANSWER_ON_QUESTION", question);
        notificationGrpcClient.sendNotification(sendNotificationRequest);

        return responseGrpcMapper.getAnswerCreateResponse(savedResponse, imageUrls);
    }

    public AnswerRecommendResponse toggleAnswerLike(AnswerRecommendRequest request) {
        Optional<ResponseLike> existingLike =
                responseLikeRepository.findByResponseIdAndUserId(request.getResponseId(), request.getUserId());

        boolean isLiked = handleLikeToggle(existingLike, request);
        int likeCount = responseLikeRepository.countByResponseId(request.getResponseId());

        return responseGrpcMapper.getAnswerRecommendResponse(request.getResponseId(), likeCount, isLiked);
    }

    public UpdateResponseResponse updateResponse(UpdateResponseRequest request) {
        Response response = responseRepository.findById(request.getResponseId())
                .orElseThrow(() -> new GrpcException(GrpcQuestionErrorCode.NULL_RESPONSE));

        if (Boolean.TRUE.equals(response.getResponseAdopt()))
            throw new GrpcException(GrpcResponseErrorCode.ALREADY_RESPONSE_ADOPTED);

        response.updateContent(request.getContent());
        responseRepository.save(response);

        return UpdateResponseResponse.newBuilder()
                .setResponseId(response.getResponseId())
                .setContent(response.getResponseContent())
                .build();
    }

    public AnswerAdoptResponse answerAdopt(AnswerAdoptRequest request) {
        Response adoptedResponse = adoptResponse(request.getResponseId());
        markQuestionAsAdopted(adoptedResponse);
        tryToSendAdoptionNotification(adoptedResponse);

        return responseGrpcMapper.getAnswerAdoptResponse(adoptedResponse);
    }

    public void deleteResponse(DeleteResponseRequest request) {
        Response response = responseRepository.findById(request.getResponseId())
                .orElseThrow(() -> new GrpcException(GrpcQuestionErrorCode.NULL_RESPONSE));

        if (Boolean.TRUE.equals(response.getResponseAdopt()))
            throw new GrpcException(GrpcResponseErrorCode.ALREADY_RESPONSE_ADOPTED);

        // FollowUpRoom이 있다면 먼저 삭제
        followUpRoomRepository.findByResponse(response)
                .ifPresent(followUpRoomRepository::delete);

        responseRepository.delete(response);
    }

    public AnswerReportResponse answerReport(AnswerReportRequest request) {
        Response response = responseRepository.findById(request.getResponseId())
                .orElseThrow(() -> new GrpcException(GrpcQuestionErrorCode.NULL_RESPONSE));
        ResponseReport responseReport = ResponseReport.from(request);

        ResponseReport savedResponseReport = responseReportRepository.save(responseReport);
        userGrpcClient.increaseReportCount(response.getResponseWriterId());
        return responseGrpcMapper.getAnswerReportResponse(savedResponseReport);
    }

    private Response adoptResponse(Long responseId) {
        Response response = responseRepository.findById(responseId)
                .orElseThrow(() -> new GrpcException(GrpcQuestionErrorCode.NULL_RESPONSE));

        validateQuestionNotAlreadyAdopted(response.getQuestionId());
        response.updateResponseAdopt();
        return responseRepository.save(response);
    }

    private void validateQuestionNotAlreadyAdopted(Long questionId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new GrpcException(GrpcQuestionErrorCode.NULL_QUESTION));

        if (Boolean.TRUE.equals(question.getQuestionAnswerAdopt())) {
            throw new GrpcException(GrpcQuestionErrorCode.EXIST_ADOPTED_RESPONSE);
        }
    }

    private void tryToSendAdoptionNotification(Response response) {
        try {
            Question question = questionRepository.findById(response.getQuestionId())
                    .orElseThrow(() -> new GrpcException(GrpcQuestionErrorCode.NULL_QUESTION));

            String body = truncateContent(response.getResponseContent());
            SendNotificationRequest notificationRequest =
                    notificationGrpcMapper.getSendNotificationRequest(body, "ANSWER_ADOPTED", question);
            notificationGrpcClient.sendNotification(notificationRequest);
        } catch (Exception e) {
            log.error("Failed to send adoption notification for response {}: {}",
                    response.getResponseId(), e.getMessage(), e);
        }
    }

    private void markQuestionAsAdopted(Response response) {
        Question question = questionRepository.findById(response.getQuestionId())
                .orElseThrow(() -> new GrpcException(GrpcQuestionErrorCode.NULL_QUESTION));

        question.updateAnswerAdopt();
        questionRepository.save(question);
    }

    private String truncateContent(String content) {
        String subBody;
        if (content.length() <= 100) {
            subBody = content.substring(0, content.length() - 1);
        } else {
            subBody = content.substring(0, 100);
        }
        return subBody;
    }

    private boolean handleLikeToggle(Optional<ResponseLike> existingLike, AnswerRecommendRequest request) {
        if (existingLike.isPresent()) {
            responseLikeRepository.delete(existingLike.get());
            return false; // 좋아요 취소됨
        }

        ResponseLike newLike = ResponseLike.from(request);
        responseLikeRepository.save(newLike);
        return true; // 새로운 좋아요
    }

    private List<String> processAnswerImages(AnswerCreateRequest answerCreateRequest, Response savedResponse) {
        List<String> imageUrls = null;
        if (!answerCreateRequest.getImagesList().isEmpty()) {
            imageUrls = fileUploadUtil.uploadImages(answerCreateRequest.getImagesList(), RESPONSE_FOLDER);
            for (String imageUrl : imageUrls) {
                ResponseImage responseImage = ResponseImage.builder()
                        .responseId(savedResponse.getResponseId())
                        .responseImageUrl(imageUrl)
                        .build();
                responseImageRepository.save(responseImage);
            }
        }
        return imageUrls;
    }
}
