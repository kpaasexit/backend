package com.exit.question.domain.question;

import com.exit.common.domain.BaseEntity;
import com.exit.common.grpc.QuestionReportRequest;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "question_reports")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AttributeOverride(name = "createdAt", column = @Column(name = "question_report_created_at"))
@AttributeOverride(name = "updatedAt", column = @Column(name = "question_report_updated_at"))
public class QuestionReport extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "question_report_id")
    private Long questionReportId;

    @Column(name = "question_id", nullable = false)
    private Long questionId;

    @Column(name = "question_report_title", length = 100)
    private String questionReportTitle;

    @Column(name = "question_report_content", columnDefinition = "TEXT")
    private String questionReportContent;

    @Column(name = "question_report_writer_id")
    private Long questionReportWriterId;

    @Builder
    public QuestionReport(Long questionId, String questionReportTitle, String questionReportContent, Long questionReportWriterId) {
        this.questionId = questionId;
        this.questionReportTitle = questionReportTitle;
        this.questionReportContent = questionReportContent;
        this.questionReportWriterId = questionReportWriterId;
    }

    public static QuestionReport from(QuestionReportRequest request) {
        return QuestionReport.builder()
                .questionId(request.getQuestionId())
                .questionReportTitle(request.getQuestionReportTitle())
                .questionReportContent(request.getQuestionReportContent())
                .questionReportWriterId(request.getQuestionReportWriterId())
                .build();
    }
}