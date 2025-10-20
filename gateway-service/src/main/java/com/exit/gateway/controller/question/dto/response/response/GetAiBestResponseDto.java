package com.exit.gateway.controller.question.dto.response.response;

import com.exit.common.grpc.AiBestResponse;
import com.exit.common.grpc.GetAiBestResponseResponse;
import java.util.List;
import lombok.Builder;

@Builder
public record GetAiBestResponseDto(
        List<GetAiBestResponseItem> aiBestResponseItemList
) {
    public static GetAiBestResponseDto from(GetAiBestResponseResponse response) {
        List<GetAiBestResponseItem> itemList = response.getAiBestResponseList().stream()
                .map(GetAiBestResponseItem::from)
                .toList();

        return GetAiBestResponseDto.builder()
                .aiBestResponseItemList(itemList)
                .build();
    }

    @Builder
    public record GetAiBestResponseItem(
            Long questionId,
            Long responseId,
            String title,
            String content
    ) {
        public static GetAiBestResponseItem from(AiBestResponse response) {
            return GetAiBestResponseItem.builder()
                    .questionId(response.getQuestionId())
                    .responseId(response.getResponseId())
                    .title(response.getTitle())
                    .content(response.getContent())
                    .build();
        }
    }
}
