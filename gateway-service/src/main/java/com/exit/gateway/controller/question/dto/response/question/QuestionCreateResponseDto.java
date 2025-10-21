package com.exit.gateway.controller.question.dto.response.question;

import com.exit.common.grpc.QuestionCreateResponse;
import com.exit.gateway.controller.question.dto.ImageObjectDto;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

import static com.exit.common.util.time.TimeStampUtil.timestampToLocalDateTime;

@Builder
public record QuestionCreateResponseDto(
        Long questionId,
        String questionTitle,
        String questionContent,
        Long questionCategory,
        Boolean questionUrgency,
        String questionAnswerType,
        String questionDisclosureType,
        Long questionWriterId,
        String questionWriterName,
        String questionWriterProfile,
        List<ImageObjectDto> images,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime createdAt
) {
    public static QuestionCreateResponseDto from(QuestionCreateResponse questionCreateResponse) {
        QuestionCreateResponseDtoBuilder builder = QuestionCreateResponseDto.builder();
        if (!questionCreateResponse.getImagesList().isEmpty()) {
            List<ImageObjectDto> images = questionCreateResponse.getImagesList().stream().map(
                    imageObject -> {
                        return ImageObjectDto.builder()
                                .imageId(imageObject.getImageId())
                                .imageUrl(imageObject.getImageUrl())
                                .build();
                    }
            ).toList();
            builder.images(images);
        }

        if(!questionCreateResponse.getQuestionWriterProfile().isEmpty()) {
            builder.questionWriterProfile(questionCreateResponse.getQuestionWriterProfile());
        }

        return builder
                .questionId(questionCreateResponse.getQuestionId())
                .questionTitle(questionCreateResponse.getQuestionTitle())
                .questionContent(questionCreateResponse.getQuestionContent())
                .questionCategory(questionCreateResponse.getQuestionCategory())
                .questionUrgency(questionCreateResponse.getQuestionUrgency())
                .questionAnswerType(questionCreateResponse.getQuestionAnswerType())
                .questionDisclosureType(questionCreateResponse.getQuestionDisclosureType())
                .questionWriterId(questionCreateResponse.getQuestionWriterId())
                .questionWriterName(questionCreateResponse.getQuestionWriterName())
                .createdAt(timestampToLocalDateTime(questionCreateResponse.getCreatedAt()))
                .build();
    }
}
