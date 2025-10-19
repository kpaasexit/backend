package com.exit.gateway.controller.search;

import com.exit.common.response.SuccessResponse;
import com.exit.common.response.success.SearchSuccessCode;
import com.exit.gateway.controller.search.dto.response.IntegratedSearchResponseDto;
import com.exit.gateway.controller.search.dto.response.RecommendedSearchTermsDto;
import com.exit.gateway.service.search.SearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
@Slf4j
public class SearchController {
    private final SearchService searchService;

    @GetMapping
    public SuccessResponse<IntegratedSearchResponseDto> integratedSearch(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return SuccessResponse.of(SearchSuccessCode.SEARCH_SUCCESS, searchService.integratedSearch(keyword, page, size));
    }

    @GetMapping("/recommend")
    public SuccessResponse<RecommendedSearchTermsDto> integratedSearch() {
        List<String> terms = List.of("감자", "고구마", "강아지", "고양이");
        return SuccessResponse.of(SearchSuccessCode.SEARCH_SUCCESS, new RecommendedSearchTermsDto(terms));
    }
}