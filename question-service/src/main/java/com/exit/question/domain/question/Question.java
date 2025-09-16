package com.exit.question.domain.question;

import com.exit.common.domain.BaseEntity;
import com.exit.question.controller.dto.request.QuestionCreateRequest;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "questions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AttributeOverride(name = "createdAt", column = @Column(name = "question_created_at"))
@AttributeOverride(name = "updatedAt", column = @Column(name = "question_updated_at"))
public class Question extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "question_id")
    private Long questionId;

    @Column(name = "question_category_id", nullable = false)
    private Long questionCategoryId;

    @Column(name = "question_writer_id")
    private Long questionWriterId;

    @Column(name = "question_title", length = 100)
    private String questionTitle;

    @Column(name = "question_content", columnDefinition = "TEXT")
    private String questionContent;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_category")
    private QuestionCategoryType questionCategory;

    @Column(name = "question_urgency")
    private Boolean questionUrgency;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_answer_type")
    private QuestionAnswerType questionAnswerType;

    @Column(name = "questIon_answer_adopt")
    private Boolean questionAnswerAdopt;

    @Builder
    public Question(Long questionCategoryId, Long questionWriterId, String questionTitle, String questionContent, QuestionCategoryType questionCategory, Boolean questionUrgency, QuestionAnswerType questionAnswerType, Boolean questionAnswerAdopt) {
        this.questionCategoryId = questionCategoryId;
        this.questionWriterId = questionWriterId;
        this.questionTitle = questionTitle;
        this.questionContent = questionContent;
        this.questionCategory = questionCategory;
        this.questionUrgency = questionUrgency;
        this.questionAnswerType = questionAnswerType;
        this.questionAnswerAdopt = questionAnswerAdopt;
    }

    public static Question createQuestionFromRequest(QuestionCreateRequest questionCreateRequest) {
        return Question.builder()
                .questionCategoryId(questionCreateRequest.questionCategoryId())
                .questionTitle(questionCreateRequest.questionTitle())
                .questionContent(questionCreateRequest.questionContent())
                .questionCategory(questionCreateRequest.questionCategory())
                .questionUrgency(questionCreateRequest.questionUrgency())
                .questionAnswerType(questionCreateRequest.questionAnswerType())
                .questionAnswerAdopt(false)
                .build();
    }
}