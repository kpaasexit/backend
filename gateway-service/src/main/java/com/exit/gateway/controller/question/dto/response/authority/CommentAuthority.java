package com.exit.gateway.controller.question.dto.response.authority;

import com.exit.common.grpc.Authority;
import lombok.Builder;

@Builder
public record CommentAuthority(
        Boolean canModify,
        Boolean canDelete
) {
    public static CommentAuthority from(Authority authority) {
        return CommentAuthority.builder()
                .canModify(authority.getCanModify())
                .canDelete(authority.getCanDelete())
                .build();
    }
}
