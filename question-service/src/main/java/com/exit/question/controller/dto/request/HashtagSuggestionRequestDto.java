package com.exit.question.controller.dto.request;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public record HashtagSuggestionRequest(
        String title,
        String content,
        List<MultipartFile> images
) {
}