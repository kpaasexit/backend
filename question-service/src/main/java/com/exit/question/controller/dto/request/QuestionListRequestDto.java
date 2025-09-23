package com.exit.question.controller.dto.request;

import com.exit.common.grpc.QuestionListRequest;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class QuestionListRequestDto {
    private List<Long> categoryIds;
    private Integer page;
    private Integer size;
    private String keyword;

    public static QuestionListRequestDto from(QuestionListRequest request) {
        QuestionListRequestDto dto = new QuestionListRequestDto();
        dto.setCategoryIds(request.getCategoryIdsList());
        dto.setPage(request.getPage());
        dto.setSize(request.getSize());
        dto.setKeyword(request.getKeyword());
        return dto;
    }
}