package com.exit.quiz.domain;

import com.exit.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "quiz_attempts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AttributeOverride(name = "createdAt", column = @Column(name = "quiz_category_created_at"))
@AttributeOverride(name = "updatedAt", column = @Column(name = "quiz_category_updated_at"))
public class QuizAttempts extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "quiz_attempt_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    @JoinColumn(name = "quiz_id")
    private Quiz quiz;

    @Column(name = "user_id")
    private Long userId;

    @Builder
    public QuizAttempts(Quiz quiz, Long userId) {
        this.quiz = quiz;
        this.userId = userId;
    }
}