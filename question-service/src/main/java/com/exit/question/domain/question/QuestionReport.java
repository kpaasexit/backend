package com.exit.question.domain.question;

import com.exit.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
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
}