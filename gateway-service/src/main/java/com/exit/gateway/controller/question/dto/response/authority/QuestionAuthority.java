package com.exit.gateway.controller.question.dto.response.authority;

import com.exit.common.grpc.Authority;
import lombok.Builder;

@Builder
public record QuestionAuthority(
        Boolean canModify,
        Boolean canDelete
) {
    public static QuestionAuthority from(Authority authority) {
        return QuestionAuthority.builder()
                .canModify(authority.getCanModify())
                .canDelete(authority.getCanDelete())
                .build();
    }
}
