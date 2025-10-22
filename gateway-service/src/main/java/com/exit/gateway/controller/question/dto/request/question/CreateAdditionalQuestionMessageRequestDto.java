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
public class CreateAdditionalQuestionMessageRequestDto {

    @NotNull(message = "질문 ID는 필수입니다.")
    private Long questionId;

    @NotNull(message = "응답 ID는 필수입니다.")
    private Long responseId;

    @NotBlank(message = "메시지 내용은 필수입니다.")
    private String content;

    private List<MultipartFile> images = new ArrayList<>();
}