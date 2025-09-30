package com.exit.gateway.controller.question.dto.request.question;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuestionCreateRequestDto {

    @NotBlank(message = "질문 제목은 필수입니다.")
    private String questionTitle;

    @NotBlank(message = "질문 내용은 필수입니다.")
    private String questionContent;

    @NotNull(message = "질문 카테고리는 필수입니다.")
    private Long questionCategory;

    private Boolean questionUrgency = false;

    @NotBlank(message = "답변 타입은 필수입니다.")
    private String questionAnswerType;

    @NotBlank(message = "공개 타입은 필수입니다.")
    private String questionDisclosureType;

    @NotNull(message = "작성자 ID는 필수입니다.")
    private Long questionWriterId;

    private Boolean questionIsAnonymous = false;

    private List<MultipartFile> images;
}