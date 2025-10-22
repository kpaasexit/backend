package com.exit.gateway.controller.question.dto.response.question;

import com.exit.common.grpc.AnswerCreateResponse;
import com.exit.gateway.controller.question.dto.ImageObjectDto;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.util.List;
import lombok.Builder;

import java.time.LocalDateTime;

import static com.exit.common.util.time.TimeStampUtil.timestampToLocalDateTime;

@Builder
public record AnswerCreateResponseDto(
        Long responseId,
        String responseContent,
        Long questionId,
        Long responseWriterId,
        List<ImageObjectDto> images,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime createdAt
) {
    public static AnswerCreateResponseDto from(AnswerCreateResponse answerCreateResponse) {
        AnswerCreateResponseDtoBuilder builder = AnswerCreateResponseDto.builder();

        if (!answerCreateResponse.getImageList().isEmpty()) {
            List<ImageObjectDto> images = answerCreateResponse.getImageList().stream().map(
                    imageObject -> {
                        return ImageObjectDto.builder()
                                .imageId(imageObject.getImageId())
                                .imageUrl(imageObject.getImageUrl())
                                .build();
                    }
            ).toList();
            builder.images(images);
        }
        return builder
                .responseId(answerCreateResponse.getResponseId())
                .responseContent(answerCreateResponse.getResponseContent())
                .questionId(answerCreateResponse.getQuestionId())
                .responseWriterId(answerCreateResponse.getResponseWriterId())
                .createdAt(timestampToLocalDateTime(answerCreateResponse.getCreatedAt()))
                .build();
    }
}