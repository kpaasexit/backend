package com.exit.gateway.service.question;

import com.exit.common.exception.rest.RestApiException;
import com.exit.common.grpc.*;
import com.exit.common.response.error.rest.question.QuestionErrorCode;
import com.exit.gateway.controller.question.dto.request.question.*;
import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class QuestionRequestMapper {

    public QuestionCreateRequest toGrpcQuestionCreateRequest(QuestionCreateRequestDto dto, Long userId) {
        QuestionCreateRequest.Builder builder = QuestionCreateRequest.newBuilder()
                .setQuestionTitle(dto.getQuestionTitle())
                .setQuestionContent(dto.getQuestionContent())
                .setQuestionCategory(dto.getQuestionCategory())
                .setQuestionUrgency(dto.getQuestionUrgency() != null ? dto.getQuestionUrgency() : false)
                .setQuestionAnswerType(dto.getQuestionAnswerType())
                .setQuestionDisclosureType(dto.getQuestionDisclosureType())
                .setQuestionWriterId(userId)
                .setQuestionIsAnonymous(dto.getQuestionIsAnonymous() != null ? dto.getQuestionIsAnonymous() : false);

        if (dto.getImages() != null && !dto.getImages().isEmpty()) {
            List<UploadBytesRequest> imageRequests = convertMultipartFilesToUploadRequests(dto.getImages());
            builder.addAllImages(imageRequests);
        }

        return builder.build();
    }

    public AnswerCreateRequest toGrpcAnswerCreateRequest(AnswerCreateRequestDto dto, Long userId) {
        AnswerCreateRequest.Builder builder = AnswerCreateRequest.newBuilder()
                .setQuestionId(dto.getQuestionId())
                .setResponseContent(dto.getResponseContent())
                .setResponseWriterId(userId)
                .setResponseIsAnonymous(dto.getResponseIsAnonymous() != null ? dto.getResponseIsAnonymous() : false);

        if (dto.getImages() != null && !dto.getImages().isEmpty()) {
            List<UploadBytesRequest> imageRequests = convertMultipartFilesToUploadRequests(dto.getImages());
            builder.addAllImages(imageRequests);
        }

        return builder.build();
    }

    public QuestionReportRequest toGrpcQuestionReportRequest(Long questionId, Long userId, QuestionReportRequestDto dto) {
        return QuestionReportRequest.newBuilder()
                .setQuestionId(questionId)
                .setQuestionReportWriterId(userId)
                .setQuestionReportTitle(dto.getQuestionReportTitle())
                .setQuestionReportContent(dto.getQuestionReportContent())
                .build();
    }

    public AnswerReportRequest toGrpcAnswerReportRequest(Long responseId, AnswerReportRequestDto dto) {
        return AnswerReportRequest.newBuilder()
                .setResponseId(responseId)
                .setResponseReportTitle(dto.getResponseReportTitle())
                .setResponseReportContent(dto.getResponseReportContent())
                .build();
    }

    public CreateAdditionalQuestionMessageRequest toGrpcCreateAdditionalQuestionMessageRequest(Long userId, CreateAdditionalQuestionMessageRequestDto dto) {
        CreateAdditionalQuestionMessageRequest.Builder builder = CreateAdditionalQuestionMessageRequest.newBuilder()
                .setUserId(userId)
                .setQuestionId(dto.getQuestionId())
                .setResponseId(dto.getResponseId())
                .setContent(dto.getContent());

        if (dto.getImages() != null && !dto.getImages().isEmpty()) {
            List<UploadBytesRequest> imageRequests = convertMultipartFilesToUploadRequests(dto.getImages());
            builder.addAllImages(imageRequests);
        }

        return builder.build();
    }

    private List<UploadBytesRequest> convertMultipartFilesToUploadRequests(List<MultipartFile> files) {
        List<UploadBytesRequest> uploadRequests = new ArrayList<>();

        for (MultipartFile file : files) {
            if (file != null && !file.isEmpty()) {
                try {
                    ImageMetadata metadata = ImageMetadata.newBuilder()
                            .setFilename(file.getOriginalFilename() != null ? file.getOriginalFilename() : "unknown")
                            .setContentType(file.getContentType() != null ? file.getContentType() : "application/octet-stream")
                            .build();

                    UploadBytesRequest uploadRequest = UploadBytesRequest.newBuilder()
                            .setMeta(metadata)
                            .setData(ByteString.copyFrom(file.getBytes()))
                            .build();

                    uploadRequests.add(uploadRequest);

                } catch (IOException e) {
                    log.error("Failed to convert MultipartFile to UploadBytesRequest: {}", file.getOriginalFilename(), e);
                    throw new RestApiException(QuestionErrorCode.FILE_CONVERSION_FAIL, "파일 변환 중 오류가 발생했습니다: " + file.getOriginalFilename());
                }
            }
        }

        return uploadRequests;
    }
}