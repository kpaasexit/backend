package com.exit.quiz.domain;

import com.exit.common.domain.BaseEntity;
import com.exit.common.grpc.ReportQuizRequest;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "quiz_reports")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AttributeOverride(name = "createdAt", column = @Column(name = "quiz_report_created_at"))
@AttributeOverride(name = "updatedAt", column = @Column(name = "quiz_report_updated_at"))
public class QuizReport extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "quiz_report_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id", insertable = false, updatable = false)
    private Quiz quiz;

    @Column(name = "quiz_report_reporter_id")
    private Long reporterId;

    @Column(name = "quiz_report_title", length = 255)
    private String title;

    @Column(name = "quiz_report_content", columnDefinition = "TEXT")
    private String content;

    @Builder
    public QuizReport(Quiz quiz, Long reporterId, String title, String content) {
        this.quiz = quiz;
        this.reporterId = reporterId;
        this.title = title;
        this.content = content;
    }

    public static QuizReport createQuizReport(Quiz quiz, ReportQuizRequest request) {
        return QuizReport.builder()
                .quiz(quiz)
                .reporterId(request.getUserId())
                .title(request.getQuizReportTitle())
                .content(request.getQuizReportContent())
                .build();
    }
}