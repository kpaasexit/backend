package com.exit.question.domain.response;

import com.exit.common.domain.BaseEntity;
import com.exit.common.grpc.AnswerReportRequest;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "response_reports")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AttributeOverride(name = "createdAt", column = @Column(name = "response_report_created_at"))
@AttributeOverride(name = "updatedAt", column = @Column(name = "response_report_updated_at"))
public class ResponseReport extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "response_report_id")
    private Long responseReportId;

    @Column(name = "response_id", nullable = false)
    private Long responseId;

    @Column(name = "response_report_reason")
    private Integer responseReportReason;

    @Column(name = "response_report_content", columnDefinition = "TEXT")
    private String responseReportContent;

    @Column(name = "response_report_writer_id")
    private Long responseReportWriterId;

    @Builder
    public ResponseReport(Long responseId, Integer responseReportReason, String responseReportContent, Long responseReportWriterId) {
        this.responseId = responseId;
        this.responseReportReason = responseReportReason;
        this.responseReportContent = responseReportContent;
        this.responseReportWriterId = responseReportWriterId;
    }

    public static ResponseReport from(AnswerReportRequest request) {
        return ResponseReport.builder()
                .responseId(request.getResponseId())
                .responseReportWriterId(request.getResponseReportWriterId())
                .responseReportReason(request.getResponseReportReason())
                .responseReportContent(request.getResponseReportContent())
                .build();
    }
}