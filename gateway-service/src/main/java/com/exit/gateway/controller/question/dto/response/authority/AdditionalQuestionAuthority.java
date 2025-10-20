package com.exit.gateway.controller.question.dto.response.authority;

import com.exit.common.grpc.Authority;
import lombok.Builder;

@Builder
public record AdditionalQuestionAuthority(
        Boolean isThirdParty,
        Boolean canWrite
) {
    public static AdditionalQuestionAuthority from(Authority authority) {
        return AdditionalQuestionAuthority.builder()
                .isThirdParty(authority.getIsThirdParty())
                .canWrite(authority.getCanWrite())
                .build();
    }
}
