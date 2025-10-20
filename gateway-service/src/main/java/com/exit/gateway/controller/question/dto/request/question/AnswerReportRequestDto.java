package com.exit.gateway.controller.question.dto.request.question;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnswerReportRequestDto {

    @NotNull(message = "신고 사유는 필수입니다.")
    private Integer responseReportReason;

    @NotBlank(message = "신고 내용은 필수입니다.")
    private String responseReportContent;
}