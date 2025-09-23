package com.exit.gateway.controller.dto.request.question;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuestionReportRequestDto {

    @NotBlank(message = "신고 제목은 필수입니다.")
    private String questionReportTitle;

    @NotBlank(message = "신고 내용은 필수입니다.")
    private String questionReportContent;
}