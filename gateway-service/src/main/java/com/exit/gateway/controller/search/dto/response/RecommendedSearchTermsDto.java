package com.exit.gateway.controller.search.dto.response;

import java.util.List;

public record RecommendedSearchTermsDto(
        List<String> terms
) {
}
