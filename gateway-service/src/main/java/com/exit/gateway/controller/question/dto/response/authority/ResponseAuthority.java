package com.exit.gateway.controller.question.dto.response.authority;

import com.exit.common.grpc.Authority;
import lombok.Builder;

@Builder
public record ResponseAuthority(
        Boolean canAdopt,
        Boolean canModify,
        Boolean canDelete,
        Boolean canWrite
) {
    public static ResponseAuthority from(Authority authority) {
        return ResponseAuthority.builder()
                .canAdopt(authority.getCanAdopt())
                .canModify(authority.getCanModify())
                .canDelete(authority.getCanDelete())
                .canWrite(authority.getCanWrite())
                .build();
    }
}
