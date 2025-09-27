package com.exit.gateway.controller.question.dto.response.question;

import com.exit.common.grpc.CreateAdditionalQuestionMessageResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateAdditionalQuestionMessageResponseDto {
    private Long followUpRoomId;
    private MessageItemDto message;

    public static CreateAdditionalQuestionMessageResponseDto from(CreateAdditionalQuestionMessageResponse response) {
        return new CreateAdditionalQuestionMessageResponseDto(
                response.getFollowUpRoomId(),
                MessageItemDto.from(response.getMessage())
        );
    }
}