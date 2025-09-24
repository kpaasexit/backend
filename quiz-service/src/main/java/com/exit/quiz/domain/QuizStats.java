package com.exit.quiz.domain;

import com.exit.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "quiz_stats")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AttributeOverride(name = "createdAt", column = @Column(name = "quiz_stat_created_at"))
@AttributeOverride(name = "updatedAt", column = @Column(name = "quiz_stat_updated_at"))
public class QuizStats extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "quiz_stat_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "quiz_id")
    private Quiz quiz;

    @Column(name = "correct_answer_number")
    private Integer correctAnswerNumber;

    @Column(name = "total_number")
    private Integer totalNumber;

    public double getCorrectRate() {
        if (totalNumber == null || totalNumber == 0) {
            return 0.0;
        }
        return (double) correctAnswerNumber / totalNumber * 100;
    }

    public void incrementTotal() {
        this.totalNumber += 1;
    }

    public void incrementCorrect() {
        this.correctAnswerNumber += 1;
    }

    public QuizStats(Quiz quiz, Integer correctAnswerNumber, Integer totalNumber) {
        this.quiz = quiz;
        this.correctAnswerNumber = correctAnswerNumber;
        this.totalNumber = totalNumber;
    }
}