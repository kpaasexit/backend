package com.exit.gateway.controller.question.dto.request.question;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnswerCreateRequestDto {

    @NotNull(message = "질문 ID는 필수입니다.")
    private Long questionId;

    @NotBlank(message = "답변 내용은 필수입니다.")
    private String responseContent;

    private Boolean responseIsAnonymous = false;

    private List<MultipartFile> images = new ArrayList<>();
}