package com.exit.question.domain.question;

import com.exit.common.domain.BaseEntity;
import com.exit.common.grpc.QuestionCreateRequest;
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_category_id", nullable = false)
    private QuestionCategory questionCategory;

    @Column(name = "question_writer_id")
    private Long questionWriterId;

    @Column(name = "question_title", length = 100)
    private String questionTitle;

    @Column(name = "question_content", columnDefinition = "TEXT")
    private String questionContent;

    @Column(name = "question_urgency")
    private Boolean questionUrgency;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_answer_type")
    private QuestionAnswerType questionAnswerType;

    @Column(name = "questIon_answer_adopt")
    private Boolean questionAnswerAdopt;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_disclosure")
    private QuestionDisclosureType questionDisclosure;

    @Column(name = "question_is_anonymous")
    private Boolean questionIsAnonymous;

    @Builder
    public Question(Long questionWriterId, String questionTitle, String questionContent, QuestionCategory questionCategory, Boolean questionUrgency, QuestionAnswerType questionAnswerType, Boolean questionAnswerAdopt, QuestionDisclosureType questionDisclosure, Boolean questionIsAnonymous) {
        this.questionWriterId = questionWriterId;
        this.questionTitle = questionTitle;
        this.questionContent = questionContent;
        this.questionCategory = questionCategory;
        this.questionUrgency = questionUrgency;
        this.questionAnswerType = questionAnswerType;
        this.questionAnswerAdopt = questionAnswerAdopt;
        this.questionDisclosure = questionDisclosure;
        this.questionIsAnonymous = questionIsAnonymous;
    }

    public static Question createQuestionFromRequest(QuestionCreateRequest request, QuestionCategory questionCategory) {
        return Question.builder()
                .questionWriterId(request.getQuestionWriterId())
                .questionTitle(request.getQuestionTitle())
                .questionContent(request.getQuestionContent())
                .questionCategory(questionCategory)
                .questionUrgency(request.getQuestionUrgency())
                .questionAnswerType(QuestionAnswerType.valueOf(request.getQuestionAnswerType()))
                .questionAnswerAdopt(false)
                .questionDisclosure(QuestionDisclosureType.valueOf(request.getQuestionDisclosureType()))
                .questionIsAnonymous(request.getQuestionIsAnonymous())
                .build();
    }

    public void updateAnswerAdopt() {
        questionAnswerAdopt = true;
    }

    public void updateQuestion(String content) {
        this.questionContent = content;
    }
}