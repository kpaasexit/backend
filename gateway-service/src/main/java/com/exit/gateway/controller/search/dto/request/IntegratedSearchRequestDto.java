package com.exit.gateway.controller.search.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record IntegratedSearchRequestDto(
        @NotBlank(message = "검색 키워드는 필수입니다.")
        String keyword,

        @Pattern(regexp = "all|question|magazine", message = "검색 타입은 all, question, magazine 중 하나여야 합니다.")
        String type // all, question, magazine
) {
    public IntegratedSearchRequestDto {
        if (type == null || type.isBlank()) {
            type = "all";
        }
    }
}
