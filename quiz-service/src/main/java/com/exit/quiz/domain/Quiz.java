package com.exit.quiz.domain;

import com.exit.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "quizs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AttributeOverride(name = "createdAt", column = @Column(name = "quiz_created_at"))
@AttributeOverride(name = "updatedAt", column = @Column(name = "quiz_updated_at"))
public class Quiz extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "quiz_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_category_id", insertable = false, updatable = false)
    private QuizCategory quizCategory;

    @Column(name = "quiz_title", length = 100)
    private String title;

    @Column(name = "quiz_content", columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "quiz_type")
    private QuizType type;

    @Column(name = "quiz_correct_answer", length = 1)
    private String correctAnswer;

    @Column(name = "quiz_additional_information", columnDefinition = "TEXT")
    private String additionalInformation;
}
