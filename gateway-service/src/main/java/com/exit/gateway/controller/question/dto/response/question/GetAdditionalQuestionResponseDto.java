package com.exit.gateway.controller.question.dto.response.question;

import com.exit.common.grpc.GetAdditionalQuestionResponse;
import com.exit.gateway.controller.question.dto.response.authority.AdditionalQuestionAuthority;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GetAdditionalQuestionResponseDto {
    private Long followUpRoomId;
    private List<MessageItemDto> messageList;
    private AdditionalQuestionAuthority authority;

    public static GetAdditionalQuestionResponseDto from(GetAdditionalQuestionResponse response) {
        return new GetAdditionalQuestionResponseDto(
                response.getFollowUpRoomId(),
                response.getMessageList().stream()
                        .map(MessageItemDto::from)
                        .toList(),
                AdditionalQuestionAuthority.from(response.getAuthority())
        );
    }
}