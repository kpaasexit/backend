package com.exit.question.domain.question;

import com.exit.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "question_images")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AttributeOverride(name = "createdAt", column = @Column(name = "question_image_created_at"))
@AttributeOverride(name = "updatedAt", column = @Column(name = "question_image_updated_at"))
public class QuestionImage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "question_image_id")
    private Long questionImageId;

    @Column(name = "question_id", nullable = false)
    private Long questionId;

    @Column(name = "question_iamge_url", length = 512)
    private String questionImageUrl;

    @Builder
    public QuestionImage(Long questionId, String questionImageUrl) {
        this.questionId = questionId;
        this.questionImageUrl = questionImageUrl;
    }
}