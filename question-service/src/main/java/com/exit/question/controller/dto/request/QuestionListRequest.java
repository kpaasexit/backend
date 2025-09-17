package com.exit.question.controller.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class QuestionListRequest {
    private List<Short> categoryIds;
    private Integer page;
    private Integer size;
    private String keyword;
}