package com.exit.question.service;

import com.exit.common.exception.grpc.GrpcException;
import com.exit.common.grpc.DeleteResponseRequest;
import com.exit.common.grpc.UpdateResponseRequest;
import com.exit.common.grpc.UpdateResponseResponse;
import com.exit.common.util.file.FileUploadUtil;
import com.exit.question.controller.dto.request.AnswerCreateRequestDto;
import com.exit.question.controller.dto.request.AnswerRecommendRequestDto;
import com.exit.question.controller.dto.request.AnswerReportRequestDto;
import com.exit.question.controller.dto.response.AnswerCreateResponseDto;
import com.exit.question.controller.dto.response.AnswerRecommendResponseDto;
import com.exit.question.controller.dto.response.AnswerReportResponseDto;
import com.exit.question.domain.question.Question;
import com.exit.question.domain.question.repository.*;
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

    public AnswerCreateResponseDto answerCreate(AnswerCreateRequestDto answerCreateRequestDto) {
        Response newResponse = Response.createResponse(answerCreateRequestDto);
        Response savedResponse = responseRepository.save(newResponse);

        List<String> imageUrls = processAnswerImages(answerCreateRequestDto, savedResponse);
        Question question = questionRepository.findById(savedResponse.getQuestionId())
                .orElseThrow(() -> new GrpcException(GrpcQuestionErrorCode.NULL_QUESTION));

        String subBody = truncateContent(savedResponse.getResponseContent());
        SendNotificationRequestDto requestDto = SendNotificationRequestDto.builder()
                .body(subBody)
                .type("NEW_ANSWER_ON_QUESTION")
                .targetId(question.getQuestionId())
                .receiverId(question.getQuestionWriterId())
                .deviceId(answerCreateRequestDto.deviceId())
                .build();
        notificationGrpcClient.sendNotification(requestDto);

        return AnswerCreateResponseDto.from(savedResponse, imageUrls);
    }

    public AnswerRecommendResponseDto toggleAnswerLike(AnswerRecommendRequestDto req) {
        Optional<ResponseLike> existingLike =
                responseLikeRepository.findByResponseIdAndUserId(req.responseId(), req.userId());

        boolean isLiked = handleLikeToggle(existingLike, req);
        int likeCount = responseLikeRepository.countByResponseId(req.responseId());

        return new AnswerRecommendResponseDto(likeCount, isLiked);
    }

    public UpdateResponseResponse updateResponse(UpdateResponseRequest request) {
        Response response = responseRepository.findById(request.getResponseId())
                .orElseThrow(() -> new GrpcException(GrpcQuestionErrorCode.NULL_RESPONSE));

        if(Boolean.TRUE.equals(response.getResponseAdopt()))
            throw new GrpcException(GrpcResponseErrorCode.ALREADY_RESPONSE_ADOPTED);

        response.updateContent(request.getContent());
        responseRepository.save(response);

        return UpdateResponseResponse.newBuilder()
                .setResponseId(response.getResponseId())
                .setContent(response.getResponseContent())
                .build();
    }

    public void deleteResponse(DeleteResponseRequest request) {
        Response response = responseRepository.findById(request.getResponseId())
                .orElseThrow(() -> new GrpcException(GrpcQuestionErrorCode.NULL_RESPONSE));

        if(Boolean.TRUE.equals(response.getResponseAdopt()))
            throw new GrpcException(GrpcResponseErrorCode.ALREADY_RESPONSE_ADOPTED);

        // FollowUpRoom이 있다면 먼저 삭제
        followUpRoomRepository.findByResponse(response)
                .ifPresent(followUpRoomRepository::delete);

        responseRepository.delete(response);
    }

    public AnswerReportResponseDto answerReport(AnswerReportRequestDto request) {
        Response response = responseRepository.findById(request.responseId())
                .orElseThrow(() -> new GrpcException(GrpcQuestionErrorCode.NULL_RESPONSE));
        ResponseReport responseReport = ResponseReport.from(request);

        ResponseReport savedResponseReport = responseReportRepository.save(responseReport);
        userGrpcClient.increaseReportCount(response.getResponseWriterId());

        return AnswerReportResponseDto.from(savedResponseReport);
    }

    private boolean handleLikeToggle(Optional<ResponseLike> existingLike, AnswerRecommendRequestDto req) {
        if (existingLike.isPresent()) {
            responseLikeRepository.delete(existingLike.get());
            return false; // 좋아요 취소됨
        }

        ResponseLike newLike = createNewLike(req);
        responseLikeRepository.save(newLike);
        return true; // 새로운 좋아요
    }

    private ResponseLike createNewLike(AnswerRecommendRequestDto req) {
        return ResponseLike.builder()
                .responseId(req.responseId())
                .userId(req.userId())
                .build();
    }

    private List<String> processAnswerImages(AnswerCreateRequestDto answerCreateRequestDto, Response savedResponse) {
        List<String> imageUrls = null;
        if (answerCreateRequestDto.images() != null) {
            imageUrls = fileUploadUtil.uploadImages(answerCreateRequestDto.images(), RESPONSE_FOLDER);
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

    private String truncateContent(String content) {
        String subBody;
        if(content.length() <= 100) {
            subBody = content.substring(0, content.length()-1);
        } else {
            subBody = content.substring(0, 100);
        }
        return subBody;
    }
}
