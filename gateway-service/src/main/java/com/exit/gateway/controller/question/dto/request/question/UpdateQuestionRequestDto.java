package com.exit.gateway.controller.question.dto.request.question;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateQuestionRequestDto {
    private String content;
    private List<Long> deleteIds = new ArrayList<>();
}
